package io.github.valentinkarnaukhov.wiremockstubgen.flatten;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One generated builder or matcher class: a schema, the position it is entered at, and
 * every accessor it declares.
 *
 * <p>Identity is the pair (schema, position), not the schema alone, and the position only
 * ever matters on a response: a response list scope holds the list and appends to it,
 * while a request scope is the same class either way because the position lives in the
 * JSONPath it carries.
 *
 * <p>The accessor set does <em>not</em> depend on where the scope was reached from. Depth
 * is counted inside a scope and restarts at every nested list, which is what makes one
 * class per schema well defined: a schema met at two different nesting depths still
 * flattens identically.
 *
 * @param schemaName    the component schema this scope describes
 * @param listPosition  whether the scope stands at a list position
 * @param accessors     every method the scope declares, in specification order
 * @param intermediates the objects those accessors reach through, in first-need order
 */
public record BodyScope(
        String schemaName,
        boolean listPosition,
        List<Accessor> accessors,
        List<Intermediate> intermediates) {

    public BodyScope {
        Objects.requireNonNull(schemaName, "schemaName");
        accessors = List.copyOf(Objects.requireNonNull(accessors, "accessors"));
        intermediates = List.copyOf(Objects.requireNonNull(intermediates, "intermediates"));
    }

    /**
     * The object an accessor's path passes through just before its last step, if any.
     *
     * <p>Looked up by path rather than by name: a name may have been escaped away from a
     * reserved word, and matching the escaped form would silently find nothing.
     */
    public Optional<Intermediate> parentOf(Accessor accessor) {
        if (accessor.path().size() < 2) {
            return Optional.empty();
        }
        List<String> prefix = accessor.path().subList(0, accessor.path().size() - 1);
        return intermediates.stream().filter(i -> i.path().equals(prefix)).findFirst();
    }

    /** The object this one stands in, which is the scope's own schema when there is none. */
    public Optional<Intermediate> parentOf(Intermediate intermediate) {
        if (intermediate.path().size() < 2) {
            return Optional.empty();
        }
        List<String> prefix = intermediate.path().subList(0, intermediate.path().size() - 1);
        return intermediates.stream().filter(i -> i.path().equals(prefix)).findFirst();
    }

    /** Stable key for the (schema, position) pair this scope is identified by. */
    public String id() {
        return listPosition ? schemaName + "[]" : schemaName;
    }
}
