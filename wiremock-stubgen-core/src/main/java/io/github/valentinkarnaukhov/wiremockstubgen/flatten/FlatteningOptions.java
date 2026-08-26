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
     * Deep enough that real specifications do not reach it, shallow enough to stop a
     * cycle. Real schemas are wide and shallow, so the accessor count stops growing well
     * before this.
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
