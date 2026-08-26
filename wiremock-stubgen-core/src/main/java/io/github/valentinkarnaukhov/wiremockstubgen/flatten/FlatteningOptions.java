package io.github.valentinkarnaukhov.wiremockstubgen.flatten;

import io.github.valentinkarnaukhov.wiremockstubgen.naming.Identifiers;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * What the flattener is allowed to do.
 *
 * @param maxDepth     how many property hops a single scope may flatten through
 * @param accessorName what to call the accessor reached by a route through the schema.
 *                     Spelling a name is the target language's business — where Java
 *                     camel-joins and steps around the methods its scopes inherit,
 *                     another language will want something else — so the target supplies
 *                     this rather than the flattener choosing.
 */
public record FlatteningOptions(int maxDepth, Function<List<String>, String> accessorName) {

    /**
     * Deep enough that real specifications do not reach it, shallow enough to stop a
     * cycle. Real schemas are wide and shallow, so the accessor count stops growing well
     * before this.
     */
    public static final int DEFAULT_MAX_DEPTH = 5;

    public FlatteningOptions {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be at least 1, got " + maxDepth);
        }
        Objects.requireNonNull(accessorName, "accessorName");
    }

    /** Camel-joined names, which is what a target has to say otherwise. */
    public static FlatteningOptions defaults() {
        return new FlatteningOptions(DEFAULT_MAX_DEPTH, Identifiers::camelJoin);
    }

    public FlatteningOptions withAccessorName(Function<List<String>, String> naming) {
        return new FlatteningOptions(maxDepth, naming);
    }

    public FlatteningOptions withMaxDepth(int depth) {
        return new FlatteningOptions(depth, accessorName);
    }
}
