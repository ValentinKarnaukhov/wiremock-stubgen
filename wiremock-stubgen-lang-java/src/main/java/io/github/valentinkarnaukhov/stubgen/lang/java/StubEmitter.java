package io.github.valentinkarnaukhov.stubgen.lang.java;

import io.github.valentinkarnaukhov.stubgen.flatten.BodyModel;
import io.github.valentinkarnaukhov.stubgen.flatten.BodyScope;
import io.github.valentinkarnaukhov.stubgen.flatten.BodySide;
import io.github.valentinkarnaukhov.stubgen.flatten.FlatteningOptions;
import io.github.valentinkarnaukhov.stubgen.flatten.Flattener;
import io.github.valentinkarnaukhov.stubgen.naming.Identifiers;
import io.github.valentinkarnaukhov.stubgen.spec.HttpMethod;
import io.github.valentinkarnaukhov.stubgen.spec.Operation;
import io.github.valentinkarnaukhov.stubgen.spec.Parameter;
import io.github.valentinkarnaukhov.stubgen.spec.ParameterLocation;
import io.github.valentinkarnaukhov.stubgen.spec.Response;
import io.github.valentinkarnaukhov.stubgen.spec.StubApi;
import io.github.valentinkarnaukhov.stubgen.spec.TypeRef;
import io.github.valentinkarnaukhov.stubgen.target.GeneratedFile;
import io.github.valentinkarnaukhov.stubgen.target.Grouping;
import io.github.valentinkarnaukhov.stubgen.target.TargetOptions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Builds the view of one stub class and hands it to a template.
 *
 * <p>Everything the generated class does at runtime is inherited. What is generated is
 * the typed surface: a method per parameter, a method per declared status code, a request
 * body method, and the one override that says which request this stub is about. That
 * split is deliberate — anything written into a generated file is a thing a consumer
 * cannot fix without regenerating.
 *
 * <p>This class decides <em>what</em> appears; {@code stub.mustache} decides how it is
 * laid out. Keeping the two apart is what makes the layout replaceable.
 */
final class StubEmitter {

    private static final String RUNTIME = "io.github.valentinkarnaukhov.stubgen.runtime.";

    private static final String WIREMOCK = "com.github.tomakehurst.wiremock.";

    private static final String TEMPLATE = "stub";

    /**
     * Names a generated scope may not use, because the runtime base it extends already
     * declares them.
     *
     * <p>The set comes from the language target and not from the core: these are method
     * names on a Java class, not keywords, and another target's runtime will have its own.
     * It is the union across both sides on purpose — {@code path()} is declared only on a
     * matcher, but escaping it only there would make one schema read differently depending
     * on which end of the operation you met it at, which is the whole thing the two-sided
     * naming rule exists to prevent.
     *
     * <p>Arity is not consulted, for the same reason. {@code exit(String)} is a legal
     * overload of the final {@code exit()} and would compile; a name that changed shape
     * according to whether the property happened to be a list would be worse than the
     * underscore.
     */
    private static final Set<String> RESERVED = Set.of(
            // AbstractBodyScope
            "exit", "mock", "buildStub", "root",
            // AbstractRequestBodyMatcher
            "path", "match",
            // generated into every list scope
            "addNew", "current");

    private final TargetOptions options;

    private final JavaTypes types;

    private final Templates templates;

    private final Flattener flattener;

    StubEmitter(StubApi api, TargetOptions options) {
        this.options = options;
        this.types = new JavaTypes(options);
        this.templates = Templates.from(options);
        this.flattener = new Flattener(api, new FlatteningOptions(options.maxDepth(), RESERVED));
    }

    GeneratedFile emit(Operation operation) {
        String packageName = packageOf(operation);
        String className = Identifiers.pascalJoin(operation.operationId()) + "Stub";
        Imports imports = new Imports(packageName);
        BodyEmitter bodies = new BodyEmitter(types, imports, className);

        // Order matters: every type name is resolved, and so every import registered,
        // before the import list itself is read into the view.
        String abstractStub = imports.use(RUNTIME + "AbstractStub");
        String stubTarget = imports.use(RUNTIME + "StubTarget");
        StubView.ParameterFields parameterFields = parameterFields(operation, imports);
        List<StubView.ParameterMethod> parameterMethods = parameterMethods(operation, imports);
        StubView.BodyMethod requestBody = requestBody(operation, imports);

        Optional<BodyModel> requestModel = takeApart(operation.requestBodyIfPresent().orElse(null), BodySide.REQUEST);
        StubView.MatcherEntry requestMatcher = requestModel
                .map(model -> new StubView.MatcherEntry(
                        BodyEmitter.matcherName(model.rootSchema()), model.rootIsList() ? "$[*]" : "$"))
                .orElse(null);

        List<StubView.ResponseMethod> responses = new ArrayList<>();
        List<BodyModel> responseModels = new ArrayList<>();
        for (Response response : operation.responses()) {
            Optional<BodyModel> model = takeApart(response.body(), BodySide.RESPONSE);
            model.ifPresent(responseModels::add);
            responses.add(responseMethod(response, model, imports));
        }

        List<StubView.MatcherClass> matchers = requestModel
                .map(model -> scopesOf(List.of(model)).stream().map(bodies::matcher).toList())
                .orElse(List.of());
        List<StubView.BuilderClass> builders = new ArrayList<>();
        for (Map.Entry<String, BodyScope> scope : indexed(responseModels).entrySet()) {
            builders.add(bodies.builder(modelOwning(responseModels, scope.getValue()), scope.getValue()));
        }

        StubView.Request request = request(operation, imports);

        StubView view = new StubView(
                packageName,
                imports.render(),
                className,
                abstractStub,
                stubTarget,
                pathConstant(operation),
                operation.path(),
                parameterFields,
                parameterMethods,
                requestBody,
                requestMatcher,
                responses,
                request,
                matchers,
                builders);

        String source = templates.render(TEMPLATE, view).stripTrailing() + "\n";
        String path = packageName.replace('.', '/') + "/" + className + ".java";
        return new GeneratedFile(path, source);
    }

    /**
     * Flattens a body, unless it must not be flattened at all.
     *
     * <p>Two things stop it. {@code explode=false} says so outright. A missing model
     * package is the quieter one: a builder names the schema of every object it creates,
     * and without a model package there is no name to use — so the stub falls back to the
     * whole-body form, which only has to name the body itself and can degrade to Object.
     */
    private Optional<BodyModel> takeApart(TypeRef body, BodySide side) {
        if (!options.explode() || options.modelPackageIfPresent().isEmpty()) {
            return Optional.empty();
        }
        return flattener.flatten(body, side);
    }

    /**
     * One class per scope across the whole operation. Two status codes declaring one
     * schema is the case this exists for: they keep their own methods, and share the
     * builder those methods hand out.
     */
    private static Map<String, BodyScope> indexed(List<BodyModel> models) {
        Map<String, BodyScope> byId = new LinkedHashMap<>();
        for (BodyScope scope : scopesOf(models)) {
            byId.putIfAbsent(scope.id(), scope);
        }
        return byId;
    }

    private static List<BodyScope> scopesOf(List<BodyModel> models) {
        return models.stream().flatMap(model -> model.scopes().stream()).toList();
    }

    private static BodyModel modelOwning(List<BodyModel> models, BodyScope scope) {
        return models.stream()
                .filter(model -> model.scopes().contains(scope))
                .findFirst()
                .orElseThrow();
    }

    // ── the request this stub is about ────────────────────────────────────────

    private static String pathConstant(Operation operation) {
        return isTemplated(operation) ? "PATH_TEMPLATE" : "PATH";
    }

    /**
     * A templated path is kept whole rather than assembled by concatenation, so that an
     * unset parameter degrades to "match any value in this segment" instead of producing
     * a URL that matches nothing.
     */
    private static boolean isTemplated(Operation operation) {
        return operation.path().indexOf('{') >= 0;
    }

    private StubView.Request request(Operation operation, Imports imports) {
        String mappingBuilder = imports.use(WIREMOCK + "client.MappingBuilder");
        String verb = imports.useStatic(WIREMOCK + "client.WireMock." + verbOf(operation.method()));
        String url = isTemplated(operation)
                ? imports.useStatic(WIREMOCK + "client.WireMock.urlPathTemplate")
                : imports.useStatic(WIREMOCK + "client.WireMock.urlPathEqualTo");

        boolean hasPath = has(operation, ParameterLocation.PATH);
        boolean hasQuery = has(operation, ParameterLocation.QUERY);
        boolean hasHeader = has(operation, ParameterLocation.HEADER);
        boolean hasCookie = has(operation, ParameterLocation.COOKIE);

        return new StubView.Request(
                mappingBuilder,
                "%s(%s(%s))".formatted(verb, url, pathConstant(operation)),
                hasPath || hasHeader || hasCookie,
                hasPath,
                hasQuery,
                hasHeader,
                hasCookie);
    }

    private static String verbOf(HttpMethod method) {
        return method.name().toLowerCase(Locale.ROOT);
    }

    // ── parameters ────────────────────────────────────────────────────────────

    private StubView.ParameterFields parameterFields(Operation operation, Imports imports) {
        List<String> names = fieldNames().entrySet().stream()
                .filter(entry -> has(operation, entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        if (names.isEmpty()) {
            return null;
        }
        return new StubView.ParameterFields(
                imports.use("java.util.Map"),
                imports.use("java.lang.String"),
                imports.use(WIREMOCK + "matching.StringValuePattern"),
                imports.use("java.util.LinkedHashMap"),
                names);
    }

    private List<StubView.ParameterMethod> parameterMethods(Operation operation, Imports imports) {
        Map<ParameterLocation, String> fields = fieldNames();
        List<StubView.ParameterMethod> methods = new ArrayList<>();
        for (Parameter parameter : operation.parameters()) {
            String javaType = types.nameOf(parameter.type()).orElse("java.lang.String");
            methods.add(new StubView.ParameterMethod(
                    methodNameOf(parameter),
                    imports.use(javaType),
                    fields.get(parameter.location()),
                    parameter.name(),
                    imports.useStatic(WIREMOCK + "client.WireMock.equalTo"),
                    JavaTypes.asQueryValue(javaType, "value")));
        }
        return methods;
    }

    /**
     * A parameter's location is part of its method name — {@code pathStringParam},
     * {@code queryStringParam}. A specification may declare a path and a query parameter
     * of the same name, which without the prefix is two methods with identical signatures
     * and a class that does not compile. Prefixing only on collision would read better and
     * would make a name depend on the rest of the operation, so an unrelated edit to the
     * specification would churn code the consumer has already written against.
     */
    private static String methodNameOf(Parameter parameter) {
        String prefix = parameter.location().name().toLowerCase(Locale.ROOT);
        return Identifiers.camelJoin(prefix, parameter.name());
    }

    private static Map<ParameterLocation, String> fieldNames() {
        Map<ParameterLocation, String> fields = new LinkedHashMap<>();
        fields.put(ParameterLocation.PATH, "pathParams");
        fields.put(ParameterLocation.QUERY, "queryParams");
        fields.put(ParameterLocation.HEADER, "headerParams");
        fields.put(ParameterLocation.COOKIE, "cookieParams");
        return fields;
    }

    private static boolean has(Operation operation, ParameterLocation location) {
        return !operation.parametersIn(location).isEmpty();
    }

    // ── bodies, whole ─────────────────────────────────────────────────────────

    private StubView.BodyMethod requestBody(Operation operation, Imports imports) {
        return operation.requestBodyIfPresent()
                .filter(body -> body.kind() != TypeRef.Kind.UNKNOWN)
                .map(body -> new StubView.BodyMethod(imports.use(bodyType(body))))
                .orElse(null);
    }

    private StubView.ResponseMethod responseMethod(Response response, Optional<BodyModel> model, Imports imports) {
        String name = response.isDefault() ? "codeDefault" : "code" + response.statusCode();
        String status = response.isDefault() ? "200" : String.valueOf(response.statusCode());
        String type = response.body().kind() == TypeRef.Kind.UNKNOWN
                ? null
                : imports.use(bodyType(response.body()));
        return new StubView.ResponseMethod(name, status, type, model.map(m -> builderEntry(m, imports)).orElse(null));
    }

    /**
     * The no-argument form, which installs a body and hands out a builder over it. The
     * body is created here rather than lazily so that there is no order in which a builder
     * is writing into something that does not exist yet.
     */
    private StubView.BuilderEntry builderEntry(BodyModel model, Imports imports) {
        String itemType = imports.use(types.modelType(model.rootSchema()));
        if (model.rootIsList()) {
            imports.use("java.util.ArrayList");
            return new StubView.BuilderEntry(
                    BodyEmitter.builderName(model.root()),
                    imports.use("java.util.List<" + types.modelType(model.rootSchema()) + ">"),
                    "new ArrayList<>()");
        }
        return new StubView.BuilderEntry(BodyEmitter.builderName(model.root()), itemType, "new " + itemType + "()");
    }

    /**
     * Falls back to Object when the body names a schema and no model package is set. The
     * stub still works — WireMock serialises whatever it is handed — but the compiler
     * stops checking the one call it exists to check, which is why the plugin warns about
     * a missing model package rather than letting it pass unremarked.
     */
    private String bodyType(TypeRef body) {
        return types.nameOf(body).orElse("java.lang.Object");
    }

    // ── layout ────────────────────────────────────────────────────────────────

    private String packageOf(Operation operation) {
        if (options.grouping() == Grouping.NONE) {
            return options.packageName();
        }
        return options.packageName() + "." + Identifiers.flatLowerCase(operation.tag());
    }
}
