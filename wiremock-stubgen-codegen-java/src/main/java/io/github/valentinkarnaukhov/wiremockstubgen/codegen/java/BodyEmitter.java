package io.github.valentinkarnaukhov.wiremockstubgen.codegen.java;

import io.github.valentinkarnaukhov.wiremockstubgen.flatten.Accessor;
import io.github.valentinkarnaukhov.wiremockstubgen.flatten.BodyModel;
import io.github.valentinkarnaukhov.wiremockstubgen.flatten.BodyScope;
import io.github.valentinkarnaukhov.wiremockstubgen.flatten.Intermediate;
import io.github.valentinkarnaukhov.wiremockstubgen.naming.Identifiers;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns one flattened scope into the view of one nested class.
 *
 * <p>Both sides read the same schema under the same names, and that is where the symmetry
 * ends: a builder writes into an object, so it must materialise the objects on the way and
 * remember which list element it is on, while a matcher writes a JSONPath expression and
 * needs neither.
 */
final class BodyEmitter {

    private static final String RUNTIME = "io.github.valentinkarnaukhov.wiremockstubgen.runtime.";

    private static final String WIREMOCK = "com.github.tomakehurst.wiremock.";

    private final JavaTypes types;

    private final Imports imports;

    private final String stubClass;

    BodyEmitter(JavaTypes types, Imports imports, String stubClass) {
        this.types = types;
        this.imports = imports;
        this.stubClass = stubClass;
    }

    /**
     * A scope's class name says which position it stands at, because on a response the two
     * are genuinely different classes: one appends to a list, the other writes an object.
     */
    static String builderName(BodyScope scope) {
        return scope.schemaName() + (scope.listPosition() ? "List" : "") + "Builder";
    }

    /** A matcher is one class per schema wherever it stands: the position lives in its path. */
    static String matcherName(String schemaName) {
        return schemaName + "Matcher";
    }

    // ── response builders ─────────────────────────────────────────────────────

    StubView.BuilderClass builder(BodyModel model, BodyScope scope) {
        String name = builderName(scope);
        String itemType = imports.use(types.modelType(scope.schemaName()));
        String listType = scope.listPosition()
                ? imports.use("java.util.List<" + types.modelType(scope.schemaName()) + ">")
                : null;

        List<StubView.BuilderAccessor> accessors = new ArrayList<>();
        for (Accessor accessor : scope.accessors()) {
            accessors.add(builderAccessor(model, scope, name, itemType, accessor));
        }

        List<StubView.BuilderHolder> holders = new ArrayList<>();
        for (Intermediate intermediate : scope.intermediates()) {
            holders.add(holder(scope, itemType, intermediate));
        }

        return new StubView.BuilderClass(
                name,
                stubClass,
                imports.use(RUNTIME + "AbstractResponseBodyBuilder"),
                scope.listPosition(),
                itemType,
                listType,
                scope.listPosition() && !(accessors.isEmpty() && holders.isEmpty()),
                accessors,
                holders);
    }

    private StubView.BuilderAccessor builderAccessor(
            BodyModel model, BodyScope scope, String owner, String itemType, Accessor accessor) {
        String property = property(accessor.path());
        if (accessor.kind() == Accessor.Kind.NESTED_LIST) {
            BodyScope target = model.target(accessor).orElseThrow();
            imports.use("java.util.ArrayList");
            Creation creation = creationOf(scope, itemType, accessor.path());
            return new StubView.BuilderAccessor(
                    owner, accessor.name(), null, true, builderName(target),
                    property, getter(accessor.path()), creation.local(), creation.receiver(),
                    accessor.readOnly(), writer(accessor.readOnly()));
        }
        String type = accessor.kind() == Accessor.Kind.VALUE_LIST
                ? imports.use("java.util.List<" + valueType(accessor) + ">")
                : imports.use(valueType(accessor));
        return new StubView.BuilderAccessor(
                owner, accessor.name(), type, false, null, property, null, null,
                leafReceiver(scope, accessor), accessor.readOnly(), writer(accessor.readOnly()));
    }

    private StubView.BuilderHolder holder(BodyScope scope, String itemType, Intermediate intermediate) {
        Creation creation = creationOf(scope, itemType, intermediate.path());
        return new StubView.BuilderHolder(
                intermediate.name(),
                imports.use(types.modelType(intermediate.schemaName())),
                property(intermediate.path()),
                getter(intermediate.path()),
                creation.local(),
                creation.receiver(),
                intermediate.readOnly(),
                writer(intermediate.readOnly()));
    }

    /**
     * The helper a read-only property is written through, imported only where one exists
     * so that stubs over ordinary schemas keep exactly the imports they had.
     */
    private String writer(boolean readOnly) {
        return readOnly ? imports.use(RUNTIME + "ReadOnlyProperties") : null;
    }

    /**
     * Where a value is written when nothing has to be created first: a property of the
     * scope's own schema goes straight to the body, or to the element the builder is on;
     * anything deeper goes through the holder that owns it.
     */
    private String leafReceiver(BodyScope scope, Accessor accessor) {
        if (accessor.path().size() == 1) {
            return scope.listPosition() ? "current()" : "body";
        }
        return scope.parentOf(accessor).orElseThrow().name() + "()";
    }

    /**
     * Where an object is created, for the methods that read their receiver twice — once to
     * test it for null and once to write to it. That second read is why these take a local
     * and the leaf accessors do not.
     */
    private Creation creationOf(BodyScope scope, String itemType, List<String> path) {
        List<String> prefix = path.subList(0, path.size() - 1);
        if (prefix.isEmpty()) {
            return scope.listPosition()
                    ? new Creation(new StubView.Local(itemType, "item", "current()"), "item")
                    : new Creation(null, "body");
        }
        Intermediate parent = scope.objectAt(prefix)
                .orElseThrow(() -> new IllegalStateException("no object recorded at " + prefix));
        String parentType = imports.use(types.modelType(parent.schemaName()));
        return new Creation(
                new StubView.Local(parentType, parent.name(), parent.name() + "()"), parent.name());
    }

    private record Creation(StubView.Local local, String receiver) {
    }

    // ── request matchers ──────────────────────────────────────────────────────

    StubView.MatcherClass matcher(BodyScope scope) {
        String name = matcherName(scope.schemaName());
        List<StubView.MatcherAccessor> accessors = new ArrayList<>();
        for (Accessor accessor : scope.accessors()) {
            accessors.add(matcherAccessor(name, accessor));
        }
        return new StubView.MatcherClass(
                name, stubClass, imports.use(RUNTIME + "AbstractRequestBodyMatcher"), accessors);
    }

    private StubView.MatcherAccessor matcherAccessor(String owner, Accessor accessor) {
        String jsonPath = jsonPathOf(accessor.path());
        if (accessor.kind() == Accessor.Kind.NESTED_LIST) {
            return new StubView.MatcherAccessor(
                    owner, accessor.name(), null, true, false,
                    matcherName(accessor.targetSchema()), jsonPath, null, null);
        }
        // The element type either way: a list of leaves is asked whether any element
        // equals the value, so the value is one element and not the list.
        String javaType = valueType(accessor);
        return new StubView.MatcherAccessor(
                owner,
                accessor.name(),
                imports.use(javaType),
                false,
                accessor.kind() == Accessor.Kind.VALUE_LIST,
                null,
                jsonPath,
                imports.useStatic(WIREMOCK + "client.WireMock.equalTo"),
                JavaTypes.asQueryValue(javaType, "value"));
    }

    /**
     * A path in bracket notation, one step per wire name.
     *
     * <p>Dot notation would be shorter and is wrong: a wire name is not an identifier, and
     * a dot inside one belongs to the name. Written as {@code .loan.reference} it reads as
     * a field of an object called loan and the match silently never fires. The quotes are
     * inside a Java string literal in the template, hence the doubled escaping.
     */
    private static String jsonPathOf(List<String> path) {
        StringBuilder expression = new StringBuilder();
        for (String step : path) {
            expression.append("['").append(step.replace("\\", "\\\\").replace("'", "\\'"))
                    .append("']");
        }
        return expression.toString();
    }

    // ── names ─────────────────────────────────────────────────────────────────

    private String valueType(Accessor accessor) {
        return types.nameOf(accessor.type()).orElse("java.lang.Object");
    }

    /**
     * The model's own names for a property, as openapi-generator writes them: a fluent
     * setter named after the property and a {@code get}-prefixed reader.
     *
     * <p>The reader follows the JavaBeans rule rather than simply upper-casing, because
     * that is what openapi-generator follows: a name whose second character is already
     * upper case keeps its first as it stands, so {@code xDashedProperty} is read by
     * {@code getxDashedProperty()}.
     */
    private static String property(List<String> path) {
        return Identifiers.camelJoin(last(path));
    }

    private static String getter(List<String> path) {
        String name = property(path);
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
            return "get" + name;
        }
        return "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String last(List<String> path) {
        return path.get(path.size() - 1);
    }
}
