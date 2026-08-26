package io.github.valentinkarnaukhov.wiremockstubgen.spec;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Which schema refers to which, derived from a catalogue of {@link ObjectSchema}.
 *
 * <p>Needed because swagger-parser does not inline {@code $ref}, not even with
 * {@code setResolve(true)}: a property referring to another schema arrives as a bare
 * reference. It answers one question — given a body schema, which schemas does emitting
 * it drag in — so that schemas nothing responds with are not emitted as noise.
 *
 * <p>There is no cycle detection; the flattener's depth cap terminates a cycle and a
 * merely deep schema by the same mechanism. The visited set below is not for recursion:
 * without it a diamond in the graph is walked twice and a cycle not at all.
 */
public final class SchemaGraph {

    private final Map<String, Set<String>> edges;

    private SchemaGraph(Map<String, Set<String>> edges) {
        this.edges = edges;
    }

    public static SchemaGraph of(Map<String, ObjectSchema> schemas) {
        Objects.requireNonNull(schemas, "schemas");
        Map<String, Set<String>> edges = new LinkedHashMap<>();
        schemas.forEach((name, schema) -> {
            Set<String> targets = new LinkedHashSet<>();
            schema.properties().forEach(property -> collectNames(property.type(), targets));
            edges.put(name, targets);
        });
        return new SchemaGraph(edges);
    }

    /** The schemas reachable from this one, including itself. Terminates on a cycle. */
    public Set<String> reachableFrom(String schemaName) {
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.push(schemaName);
        while (!pending.isEmpty()) {
            String current = pending.pop();
            if (!seen.add(current)) {
                continue;
            }
            references(current).forEach(pending::push);
        }
        return seen;
    }

    /** The schemas this one refers to directly, reaching through arrays and maps. */
    private Set<String> references(String schemaName) {
        return edges.getOrDefault(schemaName, Set.of());
    }

    private static void collectNames(TypeRef type, Set<String> into) {
        switch (type.kind()) {
            case OBJECT -> into.add(type.schemaName());
            case ARRAY, MAP -> {
                if (type.items() != null) {
                    collectNames(type.items(), into);
                }
            }
            default -> {
            }
        }
    }
}
