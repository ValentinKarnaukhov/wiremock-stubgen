package io.github.valentinkarnaukhov.stubgen.target;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Settings handed to a language target.
 *
 * <p>Common settings are explicit fields; anything target-specific goes into
 * {@link #options()} so that adding a new language does not require changing the core.
 *
 * @param packageName target package or namespace for generated stubs
 * @param explode     whether to generate flattened accessors for nested model fields
 * @param maxDepth    how deep to descend when exploding; ignored when {@code explode} is false
 * @param options     target-specific settings
 */
public record TargetOptions(
        String packageName,
        boolean explode,
        int maxDepth,
        Map<String, String> options) {

    public TargetOptions {
        Objects.requireNonNull(packageName, "packageName");
        options = Map.copyOf(Objects.requireNonNull(options, "options"));
        if (maxDepth < 0) {
            throw new IllegalArgumentException("maxDepth must not be negative, got " + maxDepth);
        }
    }

    public static Builder builder(String packageName) {
        return new Builder(packageName);
    }

    public Optional<String> option(String key) {
        return Optional.ofNullable(options.get(key));
    }

    /** Effective explosion depth: zero whenever explosion is switched off. */
    public int effectiveMaxDepth() {
        return explode ? maxDepth : 0;
    }

    public static final class Builder {
        private final String packageName;
        private boolean explode = false;
        private int maxDepth = 3;
        private Map<String, String> options = Map.of();

        private Builder(String packageName) {
            this.packageName = packageName;
        }

        public Builder explode(boolean explode) {
            this.explode = explode;
            return this;
        }

        public Builder maxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder options(Map<String, String> options) {
            this.options = options;
            return this;
        }

        public TargetOptions build() {
            return new TargetOptions(packageName, explode, maxDepth, options);
        }
    }
}
