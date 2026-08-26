package io.github.valentinkarnaukhov.wiremockstubgen.flatten;

import java.util.List;
import java.util.Objects;

/**
 * An object that flattening walked through rather than stopped at.
 *
 * <p>{@code composite.deepField.deepestField} becomes one accessor, but reaching it means
 * two objects have to exist first. Those objects are not accessors — nothing in the
 * generated API names them — yet a builder still has to create them. They are kept on the
 * scope, not on each accessor, because one {@code composite} is shared by every accessor
 * that goes through it.
 *
 * @param name       the identifier a target may give the object, joined and escaped the
 *                   same way an accessor name is
 * @param path       the wire property names leading to it, from the scope's own schema
 * @param schemaName the schema of the object itself
 * @param readOnly   whether the property holding this object is itself read-only. Such an
 *                   object still has to be created and attached before anything inside it
 *                   can be reached.
 */
public record Intermediate(String name, List<String> path, String schemaName, boolean readOnly) {

    public Intermediate {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(schemaName, "schemaName");
        path = List.copyOf(Objects.requireNonNull(path, "path"));
        if (path.isEmpty()) {
            throw new IllegalArgumentException("an intermediate must stand on a path");
        }
    }
}
