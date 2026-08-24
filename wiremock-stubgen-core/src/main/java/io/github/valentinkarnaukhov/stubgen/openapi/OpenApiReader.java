package io.github.valentinkarnaukhov.stubgen.openapi;

import io.github.valentinkarnaukhov.stubgen.spec.HttpMethod;
import io.github.valentinkarnaukhov.stubgen.spec.ObjectSchema;
import io.github.valentinkarnaukhov.stubgen.spec.Operation;
import io.github.valentinkarnaukhov.stubgen.spec.Parameter;
import io.github.valentinkarnaukhov.stubgen.spec.ParameterLocation;
import io.github.valentinkarnaukhov.stubgen.spec.Property;
import io.github.valentinkarnaukhov.stubgen.spec.Response;
import io.github.valentinkarnaukhov.stubgen.spec.StubApi;
import io.github.valentinkarnaukhov.stubgen.spec.TypeRef;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
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
 * <p>This is the only place that knows about swagger-parser. Everything downstream sees
 * records with no third-party types in them, which is what makes a second reader — for a
 * different description format — a matter of adding a class rather than of unpicking the
 * generator.
 *
 * <p>Reading is deliberately lossy. A specification says far more than a stub builder can
 * express, and carrying the rest along would only invite the emitter to depend on it.
 */
public final class OpenApiReader {

    private static final String JSON = "application/json";

    private final Consumer<String> warnings;

    /**
     * @param warnings where to report everything the reader had to decide for itself —
     *                 a missing operationId, a missing tag, a media type it ignored.
     *                 Silence would make those decisions invisible, and they are
     *                 exactly the ones a user needs to see to fix their specification.
     */
    public OpenApiReader(Consumer<String> warnings) {
        this.warnings = Objects.requireNonNull(warnings, "warnings");
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

        Map<String, Schema> components = document.getComponents() == null
                || document.getComponents().getSchemas() == null
                ? Map.of()
                : document.getComponents().getSchemas();

        return new StubApi(title(document), operations(document, components), schemas(components));
    }

    private String title(OpenAPI document) {
        if (document.getInfo() != null && document.getInfo().getTitle() != null) {
            return document.getInfo().getTitle();
        }
        warnings.accept("The specification declares no info.title; falling back to \"API\"");
        return "API";
    }

    // ── OPERATIONS ────────────────────────────────────────────────────────────

    private List<Operation> operations(OpenAPI document, Map<String, Schema> components) {
        List<Operation> operations = new ArrayList<>();
        if (document.getPaths() == null) {
            return operations;
        }
        document.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) ->
                        operations.add(operation(path, method, operation, pathItem, components))));
        return operations;
    }

    private Operation operation(String path, PathItem.HttpMethod method,
                                io.swagger.v3.oas.models.Operation operation,
                                PathItem pathItem, Map<String, Schema> components) {
        HttpMethod httpMethod = HttpMethod.valueOf(method.name());
        return new Operation(
                operationId(operation, httpMethod, path),
                tag(operation, path),
                path,
                httpMethod,
                parameters(operation, pathItem, components),
                requestBody(operation, components),
                responses(operation, components));
    }

    /**
     * The specification's operationId when it has one.
     *
     * <p>The fallback is a name, not an identity: two operations differing only in a
     * parameter would collide. openapi-generator has the same problem and resolves it the
     * same way, which at least means a user meeting the collision meets it once.
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
     * <p>openapi-generator emits an operation into every tag it declares. For a stub
     * builder that would mean the same class twice under two names, so the first tag wins
     * and the rest are reported. Without any tag the first path segment stands in — a
     * grouping the user can see in the URL, unlike a package called "default".
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
     * Parameters declared on the operation, plus those declared once on the path item and
     * shared by every operation under it. An operation may restate a shared parameter,
     * and then its own declaration wins — the specification says so, and a reader that
     * simply concatenated the two lists would emit the parameter twice.
     */
    private List<Parameter> parameters(io.swagger.v3.oas.models.Operation operation,
                                       PathItem pathItem, Map<String, Schema> components) {
        Map<String, Parameter> byIdentity = new LinkedHashMap<>();
        List<io.swagger.v3.oas.models.parameters.Parameter> declared = new ArrayList<>();
        if (pathItem.getParameters() != null) {
            declared.addAll(pathItem.getParameters());
        }
        if (operation.getParameters() != null) {
            declared.addAll(operation.getParameters());
        }
        for (io.swagger.v3.oas.models.parameters.Parameter parameter : declared) {
            ParameterLocation location = location(parameter.getIn());
            if (location == null) {
                warnings.accept("Ignoring parameter " + parameter.getName()
                        + " declared in an unsupported location " + parameter.getIn());
                continue;
            }
            byIdentity.put(location + " " + parameter.getName(), new Parameter(
                    parameter.getName(),
                    location,
                    typeOf(parameter.getSchema(), components),
                    Boolean.TRUE.equals(parameter.getRequired())));
        }
        return List.copyOf(byIdentity.values());
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

    private TypeRef requestBody(io.swagger.v3.oas.models.Operation operation,
                                Map<String, Schema> components) {
        if (operation.getRequestBody() == null || operation.getRequestBody().getContent() == null) {
            return null;
        }
        MediaType media = jsonMediaType(operation.getRequestBody().getContent(),
                "the request body of " + operation.getOperationId());
        return media == null ? null : typeOf(media.getSchema(), components);
    }

    private List<Response> responses(io.swagger.v3.oas.models.Operation operation,
                                     Map<String, Schema> components) {
        List<Response> responses = new ArrayList<>();
        if (operation.getResponses() == null) {
            return responses;
        }
        operation.getResponses().forEach((code, response) -> {
            MediaType media = response.getContent() == null ? null
                    : jsonMediaType(response.getContent(),
                    "response " + code + " of " + operation.getOperationId());
            responses.add(new Response(
                    statusCode(code, operation),
                    media == null ? TypeRef.unknown() : typeOf(media.getSchema(), components),
                    response.getDescription()));
        });
        return responses;
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
     * <p>Everything downstream assumes JSON — the response is serialised with a JSON
     * mapper and the request is matched with JSONPath — so a specification declaring XML
     * as well would otherwise produce a stub that quietly speaks the wrong dialect.
     */
    private MediaType jsonMediaType(Map<String, MediaType> content, String what) {
        if (content.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, MediaType> entry : content.entrySet()) {
            if (entry.getKey().toLowerCase(Locale.ROOT).contains("json")) {
                if (content.size() > 1) {
                    warnings.accept(what + " declares " + content.keySet()
                            + "; using " + entry.getKey());
                }
                return entry.getValue();
            }
        }
        Map.Entry<String, MediaType> only = content.entrySet().iterator().next();
        warnings.accept(what + " declares no JSON media type, only " + content.keySet()
                + "; using " + only.getKey() + ", which the generated stub will still treat as JSON");
        return only.getValue();
    }

    // ── SCHEMAS ───────────────────────────────────────────────────────────────

    private Map<String, ObjectSchema> schemas(Map<String, Schema> components) {
        Map<String, ObjectSchema> schemas = new LinkedHashMap<>();
        components.forEach((name, schema) -> {
            if (schema.getProperties() == null) {
                return;
            }
            List<String> required = schema.getRequired() == null ? List.of() : schema.getRequired();
            List<Property> properties = new ArrayList<>();
            schema.getProperties().forEach((propertyName, propertySchema) -> properties.add(
                    new Property(String.valueOf(propertyName),
                            typeOf((Schema<?>) propertySchema, components),
                            required.contains(String.valueOf(propertyName)))));
            schemas.put(name, new ObjectSchema(name, properties));
        });
        return schemas;
    }

    /**
     * Turns a schema into a type reference, resolving {@code $ref} ourselves.
     *
     * <p>swagger-parser does not inline references, not even with
     * {@code setResolve(true)} — verified against the parser. A property pointing at
     * another schema arrives with a null type and a bare {@code $ref} string, so
     * following it is our job, and so is stopping: this method resolves one step and
     * records the name, never descending. Cycles are therefore not this method's problem,
     * which is why the fixture's three kinds of recursion do not hang it.
     */
    private TypeRef typeOf(Schema<?> schema, Map<String, Schema> components) {
        if (schema == null) {
            return TypeRef.unknown();
        }
        if (schema.get$ref() != null) {
            String name = referencedName(schema.get$ref());
            if (name == null) {
                warnings.accept("Ignoring the unsupported reference " + schema.get$ref());
                return TypeRef.unknown();
            }
            Schema<?> referenced = components.get(name);
            return referenced != null && referenced.getEnum() != null
                    ? TypeRef.enumeration(name)
                    : TypeRef.object(name);
        }
        if ("array".equals(schema.getType()) || schema.getItems() != null) {
            return TypeRef.array(typeOf(schema.getItems(), components));
        }
        if (schema.getAdditionalProperties() instanceof Schema<?> values) {
            return TypeRef.map(typeOf(values, components));
        }
        if (schema.getProperties() != null) {
            // An object written inline rather than referenced. openapi-generator invents
            // a name for it; we would have to invent the same one to reference the model
            // it generates, and inventing it separately is how the two drift apart.
            warnings.accept("Ignoring an inline object schema; only referenced schemas are supported");
            return TypeRef.unknown();
        }
        if (schema.getType() == null) {
            return TypeRef.unknown();
        }
        // An enum declared inline in a parameter is deliberately read as its base type.
        // No Java client library generates a type for one, so inventing a type here would
        // make the stub the only place that has it — see the note on enums in the plan.
        return TypeRef.primitive(schema.getType(), schema.getFormat());
    }

    private String referencedName(String ref) {
        int lastSlash = ref.lastIndexOf('/');
        if (!ref.startsWith("#/components/schemas/") && !ref.startsWith("#/definitions/")) {
            return null;
        }
        return ref.substring(lastSlash + 1);
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
