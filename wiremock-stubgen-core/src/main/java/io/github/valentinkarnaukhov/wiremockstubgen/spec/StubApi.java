package io.github.valentinkarnaukhov.wiremockstubgen.spec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A whole API as understood by the generator: a flat list of operations, the schemas
 * those operations refer to, and the spec-level title used to derive default naming.
 * Everything under {@code spec} is free of target-language concepts.
 *
 * @param title      the specification title
 * @param operations every operation the specification declares
 * @param schemas    named object schemas, keyed by name, in specification order
 */
public record StubApi(String title, List<Operation> operations, Map<String, ObjectSchema> schemas) {

    public StubApi {
        Objects.requireNonNull(title, "title");
        operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
        schemas = Map.copyOf(Objects.requireNonNull(schemas, "schemas"));
    }

    /**
     * Looks up the object schema a type refers to, reaching through arrays and maps: an
     * operation answering with {@code array<CompositeBody>} resolves to CompositeBody.
     */
    public Optional<ObjectSchema> schemaOf(TypeRef type) {
        return Optional.ofNullable(type)
                .map(StubApi::namedSchema)
                .map(schemas::get);
    }

    /**
     * The schemas actually reachable from the operations, in the order they are first met.
     * A specification routinely declares schemas nothing responds with, and emitting a
     * builder for those would be noise.
     */
    public Map<String, ObjectSchema> reachableSchemas() {
        SchemaGraph graph = SchemaGraph.of(schemas);
        Map<String, ObjectSchema> reachable = new LinkedHashMap<>();
        for (Operation operation : operations) {
            operation.requestBodyIfPresent().ifPresent(body -> collect(body, graph, reachable));
            operation.responses().forEach(response -> collect(response.body(), graph, reachable));
        }
        return reachable;
    }

    private void collect(TypeRef type, SchemaGraph graph, Map<String, ObjectSchema> into) {
        String root = namedSchema(type);
        if (root == null) {
            return;
        }
        for (String name : graph.reachableFrom(root)) {
            ObjectSchema schema = schemas.get(name);
            if (schema != null) {
                into.put(name, schema);
            }
        }
    }

    private static String namedSchema(TypeRef type) {
        return switch (type.kind()) {
            case OBJECT -> type.schemaName();
            case ARRAY, MAP -> type.items() == null ? null : namedSchema(type.items());
            default -> null;
        };
    }
}
