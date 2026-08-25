package io.github.valentinkarnaukhov.stubgen.flatten;

import java.util.List;
import java.util.Objects;

/**
 * One generated builder or matcher class: a schema, the position it is entered at, and
 * every accessor it declares.
 *
 * <p>Identity is the pair (schema, position), not the schema alone, and the position only
 * ever matters on a response. A response list scope holds the list and appends to it,
 * which a single scope has no use for; a request scope is the same class either way,
 * because the position lives in the JSONPath it carries. So one schema can yield two
 * response scopes and never two request ones.
 *
 * <p>The accessor set does <em>not</em> depend on where the scope was reached from. Depth
 * is counted inside a scope and restarts at every nested list, which is what makes one
 * class per schema well defined: a schema met at two different nesting depths still
 * flattens identically. Counting depth from the body root instead would make a class's
 * contents depend on its neighbours in the graph, which is the trade this generator has
 * already refused once.
 *
 * @param schemaName   the component schema this scope describes
 * @param listPosition whether the scope stands at a list position
 * @param accessors    every method the scope declares, in specification order
 */
public record BodyScope(String schemaName, boolean listPosition, List<Accessor> accessors) {

    public BodyScope {
        Objects.requireNonNull(schemaName, "schemaName");
        accessors = List.copyOf(Objects.requireNonNull(accessors, "accessors"));
    }

    /** Stable key for the (schema, position) pair this scope is identified by. */
    public String id() {
        return listPosition ? schemaName + "[]" : schemaName;
    }
}
