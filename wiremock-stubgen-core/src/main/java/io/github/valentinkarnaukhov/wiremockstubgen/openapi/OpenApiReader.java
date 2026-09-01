package io.github.valentinkarnaukhov.wiremockstubgen.openapi;

import io.github.valentinkarnaukhov.wiremockstubgen.naming.Identifiers;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.CollectionFormat;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.HttpMethod;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.Operation;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.Parameter;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.ParameterLocation;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.Property;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.Response;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.StubApi;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.TypeRef;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter.StyleEnum;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Reads an OpenAPI document into the language-neutral description under {@code spec}.
 *
 * <p>The only place that knows about swagger-parser: everything downstream sees records
 * with no third-party types in them. Reading is deliberately lossy — a specification says
 * far more than a stub builder can express.
 */
public final class OpenApiReader {

    private static final String JSON = "application/json";

    private final Consumer<String> warnings;

    private final Composition composition;

    /**
     * @param warnings where to report everything the reader had to decide for itself — a
     *                 missing operationId, a missing tag, a media type it ignored
     */
    public OpenApiReader(Consumer<String> warnings) {
        this(warnings, Composition.MERGE);
    }

    /**
     * @param composition what to make of {@code oneOf} and {@code anyOf}. Defaults to
     *                    {@link Composition#MERGE} elsewhere because that is what
     *                    openapi-generator does unless told otherwise.
     */
    public OpenApiReader(Consumer<String> warnings, Composition composition) {
        this.warnings = Objects.requireNonNull(warnings, "warnings");
        this.composition = Objects.requireNonNull(composition, "composition");
    }

    public OpenApiReader() {
        this(warning -> {
        });
    }

    public StubApi read(Path specification) {
        return read(Objects.requireNonNull(specification, "specification").toString());
    }

    /**
     * @param location a file path or a URL, as swagger-parser understands it
     */
    public StubApi read(String location) {
        Objects.requireNonNull(location, "location");
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIParser().readLocation(location, null, options);
        OpenAPI document = result.getOpenAPI();
        if (document == null) {
            throw new IllegalArgumentException(
                    "Cannot read the specification at " + location + ": " + result.getMessages());
        }
        result.getMessages().forEach(warnings);

        Components components = document.getComponents() == null ? new Components() : document.getComponents();
        Map<String, Schema> declared = components.getSchemas() == null
                ? Map.of() : components.getSchemas();

        Schemas schemas = new Schemas(declared, warnings, composition);

        // Bodies written out in place are read first, and into the same Schemas, because
        // that is the order openapi-generator gives names in: where the same shape is
        // written out twice, the class is named after whichever the generator reached
        // first, and it reaches the paths before the components.
        List<Operation> operations = operations(document, components, schemas);
        schemas.readDeclared();

        if (!declared.isEmpty() && schemas.resolved().isEmpty()) {
            warnings.accept("None of the " + declared.size() + " schemas this specification"
                    + " declares could be described; every stub will take its bodies whole");
        }
        return new StubApi(title(document), operations, schemas.resolved());
    }

    private String title(OpenAPI document) {
        if (document.getInfo() != null && document.getInfo().getTitle() != null) {
            return document.getInfo().getTitle();
        }
        warnings.accept("The specification declares no info.title; falling back to \"API\"");
        return "API";
    }

    // ── OPERATIONS ────────────────────────────────────────────────────────────

    private List<Operation> operations(OpenAPI document, Components components, Schemas schemas) {
        List<Operation> operations = new ArrayList<>();
        if (document.getPaths() == null) {
            return operations;
        }
        document.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) ->
                        operations.add(operation(path, method, operation, pathItem, components, schemas))));
        return operations;
    }

    private Operation operation(String path, PathItem.HttpMethod method,
                                io.swagger.v3.oas.models.Operation operation,
                                PathItem pathItem, Components components, Schemas schemas) {
        HttpMethod httpMethod = HttpMethod.valueOf(method.name());
        String operationId = operationId(operation, httpMethod, path);
        return new Operation(
                operationId,
                tag(operation, path),
                path,
                httpMethod,
                parameters(operation, pathItem, components, schemas),
                requestBody(operation, operationId, components, schemas),
                responses(operation, operationId, components, schemas));
    }

    /**
     * The specification's operationId when it has one.
     *
     * <p>The fallback is a name, not an identity: two operations differing only in a
     * parameter would collide. openapi-generator resolves it the same way, so a user
     * meeting the collision meets it once.
     */
    private String operationId(io.swagger.v3.oas.models.Operation operation,
                               HttpMethod method, String path) {
        if (operation.getOperationId() != null && !operation.getOperationId().isBlank()) {
            return operation.getOperationId();
        }
        String synthesised = method.name().toLowerCase(Locale.ROOT) + camelise(pathWords(path));
        warnings.accept(method + " " + path + " declares no operationId; using " + synthesised);
        return synthesised;
    }

    /**
     * The first declared tag.
     *
     * <p>openapi-generator emits an operation into every tag it declares; for a stub
     * builder that would mean the same class twice, so the first tag wins and the rest are
     * reported. Without any tag the first path segment stands in.
     */
    private String tag(io.swagger.v3.oas.models.Operation operation, String path) {
        List<String> tags = operation.getTags();
        if (tags == null || tags.isEmpty()) {
            String fallback = pathWords(path).isEmpty() ? "default" : pathWords(path).get(0);
            warnings.accept(path + " declares no tag; grouping under " + fallback);
            return fallback;
        }
        if (tags.size() > 1) {
            warnings.accept(path + " declares several tags " + tags + "; using " + tags.get(0));
        }
        return tags.get(0);
    }

    /**
     * Parameters declared on the operation, plus those declared once on the path item.
     * An operation may restate a shared parameter, and then its own declaration wins:
     * concatenating the two lists would emit the parameter twice.
     *
     * <p>A query parameter written as an object is expanded into its properties. See
     * {@link Schemas#queryProperties}.
     */
    private List<Parameter> parameters(io.swagger.v3.oas.models.Operation operation,
                                       PathItem pathItem, Components components, Schemas schemas) {
        Map<String, Parameter> byIdentity = new LinkedHashMap<>();
        List<io.swagger.v3.oas.models.parameters.Parameter> declared = new ArrayList<>();
        if (pathItem.getParameters() != null) {
            declared.addAll(pathItem.getParameters());
        }
        if (operation.getParameters() != null) {
            declared.addAll(operation.getParameters());
        }
        for (io.swagger.v3.oas.models.parameters.Parameter reference : declared) {
            io.swagger.v3.oas.models.parameters.Parameter parameter =
                    dereference(reference, components.getParameters(), "#/components/parameters/",
                            io.swagger.v3.oas.models.parameters.Parameter::get$ref);
            if (parameter == null) {
                continue;
            }
            ParameterLocation location = location(parameter.getIn());
            if (location == null) {
                warnings.accept("Ignoring parameter " + parameter.getName()
                        + " declared in an unsupported location " + parameter.getIn());
                continue;
            }
            List<Property> grouped = location == ParameterLocation.QUERY
                    ? schemas.queryProperties(parameter.getSchema())
                    : List.of();
            if (!grouped.isEmpty()) {
                for (Property property : grouped) {
                    byIdentity.put(location + " " + property.name(),
                            new Parameter(property.name(), location, property.type(),
                                    // A property of an expanded object is handed to the client
                                    // without a collection format of its own, and the client
                                    // falls back to a comma.
                                    joinedBy(property.type(), CollectionFormat.CSV)));
                }
                continue;
            }
            TypeRef type = schemas.typeOf(parameter.getSchema(), null);
            byIdentity.put(location + " " + parameter.getName(), new Parameter(
                    parameter.getName(),
                    location,
                    type,
                    joinedBy(type, location == ParameterLocation.QUERY
                            ? queryCollectionFormat(parameter)
                            : CollectionFormat.CSV)));
        }
        return List.copyOf(byIdentity.values());
    }

    /** The format a collection would use, or {@link CollectionFormat#NONE} if it is not one. */
    private static CollectionFormat joinedBy(TypeRef type, CollectionFormat format) {
        return type.kind() == TypeRef.Kind.ARRAY ? format : CollectionFormat.NONE;
    }

    /**
     * How a list in the query string is written, mirroring openapi-generator 7.24.0 — the
     * client the stub has to agree with. Measured there: {@code spaceDelimited} and
     * {@code pipeDelimited} pick their separator whatever {@code explode} says,
     * {@code deepObject} falls back to a comma, and everything else repeats the parameter
     * unless {@code explode} is explicitly false.
     *
     * <p>This is the one place {@code explode} is read. Elsewhere it is deliberately
     * ignored, but here it decides the text on the wire and cannot be guessed around.
     */
    private static CollectionFormat queryCollectionFormat(
            io.swagger.v3.oas.models.parameters.Parameter parameter) {
        StyleEnum style = parameter.getStyle();
        if (style == StyleEnum.SPACEDELIMITED) {
            return CollectionFormat.SSV;
        }
        if (style == StyleEnum.PIPEDELIMITED) {
            return CollectionFormat.PIPES;
        }
        if (style == StyleEnum.DEEPOBJECT || Boolean.FALSE.equals(parameter.getExplode())) {
            return CollectionFormat.CSV;
        }
        return CollectionFormat.MULTI;
    }

    private ParameterLocation location(String in) {
        if (in == null) {
            return null;
        }
        return switch (in.toLowerCase(Locale.ROOT)) {
            case "path" -> ParameterLocation.PATH;
            case "query" -> ParameterLocation.QUERY;
            case "header" -> ParameterLocation.HEADER;
            case "cookie" -> ParameterLocation.COOKIE;
            default -> null;
        };
    }

    private TypeRef requestBody(io.swagger.v3.oas.models.Operation operation, String operationId,
                                Components components, Schemas schemas) {
        RequestBody body = dereference(operation.getRequestBody(), components.getRequestBodies(),
                "#/components/requestBodies/", RequestBody::get$ref);
        if (body == null || body.getContent() == null) {
            return null;
        }
        Map.Entry<String, MediaType> media = jsonMediaType(body.getContent(), "the request body of " + operationId);
        return media == null ? null
                : schemas.typeOf(media.getValue().getSchema(), Identifiers.pascalJoin(operationId, "request"));
    }

    private List<Response> responses(io.swagger.v3.oas.models.Operation operation, String operationId,
                                     Components components, Schemas schemas) {
        List<Response> responses = new ArrayList<>();
        if (operation.getResponses() == null) {
            return responses;
        }
        operation.getResponses().forEach((code, reference) -> {
            ApiResponse response = dereference(reference, components.getResponses(),
                    "#/components/responses/", ApiResponse::get$ref);
            if (response == null) {
                return;
            }
            Map.Entry<String, MediaType> media = response.getContent() == null ? null
                    : jsonMediaType(response.getContent(), "response " + code + " of " + operationId);
            responses.add(new Response(
                    statusCode(code, operation),
                    media == null ? TypeRef.unknown()
                            : schemas.typeOf(media.getValue().getSchema(),
                            Identifiers.pascalJoin(operationId, code, "response")),
                    media == null ? null : media.getKey()));
        });
        return responses;
    }

    /**
     * Follows a {@code $ref} that points at a component other than a schema.
     *
     * <p>swagger-parser leaves these alone even under {@code setResolve(true)}: a response
     * written as {@code $ref: '#/components/responses/BadRequest'} arrives with its
     * {@code $ref} still set and its content null. Shared error responses are common, so
     * not following it silently loses their bodies.
     */
    private <T> T dereference(T reference, Map<String, T> components, String prefix,
                              java.util.function.Function<T, String> refOf) {
        if (reference == null) {
            return null;
        }
        String ref = refOf.apply(reference);
        if (ref == null) {
            return reference;
        }
        if (!ref.startsWith(prefix)) {
            warnings.accept("Ignoring the unsupported reference " + ref);
            return null;
        }
        String name = ref.substring(prefix.length());
        T target = components == null ? null : components.get(name);
        if (target == null) {
            warnings.accept("Ignoring the dangling reference " + ref);
            return null;
        }
        return target;
    }

    private Integer statusCode(String code, io.swagger.v3.oas.models.Operation operation) {
        if ("default".equalsIgnoreCase(code)) {
            return null;
        }
        try {
            return Integer.valueOf(code);
        } catch (NumberFormatException e) {
            warnings.accept("Ignoring the unparseable status code \"" + code + "\" on "
                    + operation.getOperationId() + "; treating it as the default response");
            return null;
        }
    }

    /**
     * Picks the JSON media type, or the only one present.
     *
     * <p>Everything downstream assumes JSON — responses are serialised with a JSON mapper
     * and requests matched with JSONPath — so a document declaring XML as well would
     * otherwise produce a stub that quietly speaks the wrong dialect.
     */
    private Map.Entry<String, MediaType> jsonMediaType(Map<String, MediaType> content, String what) {
        if (content.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, MediaType> entry : content.entrySet()) {
            if (entry.getKey().toLowerCase(Locale.ROOT).contains("json")) {
                if (content.size() > 1) {
                    warnings.accept(what + " declares " + content.keySet()
                            + "; using " + entry.getKey());
                }
                return entry;
            }
        }
        Map.Entry<String, MediaType> only = content.entrySet().iterator().next();
        warnings.accept(what + " declares no JSON media type, only " + content.keySet()
                + "; using " + only.getKey() + ", which the generated stub will still treat as JSON");
        return only;
    }

    // ── NAMES ─────────────────────────────────────────────────────────────────

    private List<String> pathWords(String path) {
        return Arrays.stream(path.split("/"))
                .filter(segment -> !segment.isBlank())
                .filter(segment -> !segment.startsWith("{"))
                .toList();
    }

    private String camelise(List<String> words) {
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            for (String part : word.split("[^A-Za-z0-9]+")) {
                if (!part.isEmpty()) {
                    result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
                }
            }
        }
        return result.toString();
    }
}
