package io.github.valentinkarnaukhov.wiremockstubgen.flatten;

import java.util.Objects;
import java.util.Set;

/**
 * What the flattener is allowed to do.
 *
 * @param maxDepth      how many property hops a single scope may flatten through
 * @param reservedNames accessor names the target language cannot accept on a body scope
 */
public record FlatteningOptions(int maxDepth, Set<String> reservedNames) {

    /**
     * Deep enough that no real specification was seen to reach it, shallow enough to stop
     * a cycle.
     *
     * <p>Measured over 44 specifications and 673 schemas, the number of accessors a
     * builder gets stops changing at depth 3: the totals run 1036, 1856, 1973, 2005 and
     * then stay at 2005 through depths 8 and 10, with the largest single builder at 141
     * methods. The combinatorial explosion an earlier draft of the plan feared does not
     * happen, because real schemas are wide and shallow rather than deep. Five leaves
     * headroom over the observed ceiling without inviting one.
     */
    public static final int DEFAULT_MAX_DEPTH = 5;

    public FlatteningOptions {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be at least 1, got " + maxDepth);
        }
        reservedNames = Set.copyOf(Objects.requireNonNull(reservedNames, "reservedNames"));
    }

    public static FlatteningOptions defaults() {
        return new FlatteningOptions(DEFAULT_MAX_DEPTH, Set.of());
    }

    public FlatteningOptions withReservedNames(Set<String> names) {
        return new FlatteningOptions(maxDepth, names);
    }

    public FlatteningOptions withMaxDepth(int depth) {
        return new FlatteningOptions(depth, reservedNames);
    }
}
