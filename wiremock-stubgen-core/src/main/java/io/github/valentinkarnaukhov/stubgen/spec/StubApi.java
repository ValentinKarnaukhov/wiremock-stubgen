package io.github.valentinkarnaukhov.stubgen.spec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A whole API as understood by the generator: a flat list of operations, the schemas
 * those operations refer to, and the spec-level title used to derive default naming.
 *
 * <p>This type — and everything else under {@code spec} — is deliberately free of any
 * target-language concepts. It describes what the specification says, not what any
 * particular language should emit.
 *
 * <p>The schema catalogue is new. While a stub's body was a single typed argument the
 * name of a schema was all the emitter needed; now that a stub declares a builder per
 * schema with a method per leaf, it has to know what is inside one.
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
     * Looks up the object schema a type refers to, if it refers to one at all.
     *
     * <p>Reaches through arrays and maps, so an operation answering with
     * {@code array<CompositeBody>} resolves to CompositeBody. That is what a caller
     * almost always wants: the container is not something a builder is generated for.
     */
    public Optional<ObjectSchema> schemaOf(TypeRef type) {
        return Optional.ofNullable(type)
                .map(StubApi::namedSchema)
                .map(schemas::get);
    }

    /**
     * The schemas actually reachable from the operations, in the order they are first
     * met.
     *
     * <p>A specification routinely declares schemas nothing responds with — shared
     * components, request-only envelopes, leftovers from an earlier version. Emitting a
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
