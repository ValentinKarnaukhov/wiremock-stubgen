package io.github.valentinkarnaukhov.stubgen.flatten;

import java.util.List;
import java.util.Objects;

/**
 * An object that flattening walked through rather than stopped at.
 *
 * <p>{@code composite.deepField.deepestField} becomes one accessor, but reaching it means
 * two objects have to exist first. Those objects are not accessors — nothing in the
 * generated API names them — and they are still the thing a builder has to create. This
 * records them, in the order they were first needed.
 *
 * <p>They are kept on the scope rather than on each accessor because that is what they
 * are: one {@code composite} shared by every accessor that goes through it, not a fact
 * repeated per path. The alternative — a parallel list of schema names beside
 * {@link Accessor#path()} — says the same thing while inviting the two to disagree.
 *
 * @param name       the identifier a target may give the object, joined and escaped the
 *                   same way an accessor name is
 * @param path       the wire property names leading to it, from the scope's own schema
 * @param schemaName the schema of the object itself
 * @param readOnly   whether the property holding this object is itself read-only. An
 *                   object nobody can hand to its owner still has to be created and
 *                   attached before anything inside it can be reached.
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
