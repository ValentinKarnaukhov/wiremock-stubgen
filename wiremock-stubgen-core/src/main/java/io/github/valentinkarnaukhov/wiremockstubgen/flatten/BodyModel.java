package io.github.valentinkarnaukhov.wiremockstubgen.flatten;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Everything an emitter needs to write out one body of one operation.
 *
 * <p>A model exists only when the body is, or is a list of, an object schema the
 * specification describes. A bare string body or an empty response produces none, and the
 * emitter should fall back to the typed whole-body form.
 *
 * @param side       which end of the operation this body belongs to
 * @param rootSchema the schema at the root of the body
 * @param rootIsList whether the body itself is an array
 * @param scopes     every scope reachable from the root, the root first
 */
public record BodyModel(BodySide side, String rootSchema, boolean rootIsList, List<BodyScope> scopes) {

    public BodyModel {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(rootSchema, "rootSchema");
        scopes = List.copyOf(Objects.requireNonNull(scopes, "scopes"));
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException("a body model must have at least the root scope");
        }
    }

    public BodyScope root() {
        return scopes.get(0);
    }

    /** The scope a nested-list accessor hands out. */
    public Optional<BodyScope> target(Accessor accessor) {
        return accessor.targetSchemaIfPresent()
                .flatMap(schema -> scope(schema, side.distinguishesPosition()));
    }

    public Optional<BodyScope> scope(String schemaName, boolean listPosition) {
        return scopes.stream()
                .filter(s -> s.schemaName().equals(schemaName) && s.listPosition() == listPosition)
                .findFirst();
    }
}
