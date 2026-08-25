package io.github.valentinkarnaukhov.stubgen.target;

import io.github.valentinkarnaukhov.stubgen.flatten.FlatteningOptions;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Settings handed to a language target.
 *
 * <p>Common settings are explicit fields; anything target-specific goes into
 * {@link #options()} so that adding a new language does not require changing the core.
 *
 * @param packageName  target package or namespace for generated stubs
 * @param modelPackage where the consumer's own model classes live, or {@code null}
 * @param grouping     how stubs are laid out under {@code packageName}
 * @param explode      whether bodies are described field by field as well as whole
 * @param maxDepth     how many property hops a body scope may flatten through
 * @param options      target-specific settings
 */
public record TargetOptions(
        String packageName,
        String modelPackage,
        Grouping grouping,
        boolean explode,
        int maxDepth,
        Map<String, String> options) {

    public TargetOptions {
        Objects.requireNonNull(packageName, "packageName");
        Objects.requireNonNull(grouping, "grouping");
        options = Map.copyOf(Objects.requireNonNull(options, "options"));
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be at least 1, got " + maxDepth);
        }
    }

    public static Builder builder(String packageName) {
        return new Builder(packageName);
    }

    public Optional<String> option(String key) {
        return Optional.ofNullable(options.get(key));
    }

    /**
     * Where the model classes a stub refers to are to be found.
     *
     * <p>This generator does not produce models and does not want to. The consumer
     * already runs openapi-generator against the same specification to get a client, and
     * a second set of classes for the same schemas would be a second thing to keep in
     * step. So stubs import the consumer's, and there is no sensible default for where
     * those live.
     *
     * <p>Empty means a target cannot emit anything that names a body type: no
     * {@code code200(CompositeBody)}, no {@code requestBody(CompositeBody)}. Field
     * accessors do not need it, so this degrades rather than fails.
     */
    public Optional<String> modelPackageIfPresent() {
        return Optional.ofNullable(modelPackage);
    }

    /**
     * Whether to describe bodies field by field, or only whole.
     *
     * <p>Switched off, a stub keeps exactly the forms that take a model the caller
     * already has — {@code code200(CompositeBody)}, {@code requestBody(CompositeBody)} —
     * and no builders, matchers or flattened accessors are generated at all.
     *
     * <p>This costs a target almost nothing, because the same branch is compulsory
     * anyway: a bare string body, an empty response and an array of primitives all make
     * the flattener return nothing, and something still has to be emitted for them. The
     * switch simply chooses that branch on purpose rather than having it forced by the
     * schema.
     *
     * <p>It does interact with {@link #modelPackageIfPresent()}. The whole-body forms are
     * the only ones that name a model type, so a stub generated with explosion off and no
     * model package can describe nothing about its body whatsoever.
     */
    public boolean explode() {
        return explode;
    }

    public static final class Builder {
        private final String packageName;
        private String modelPackage;
        private Grouping grouping = Grouping.TAG;
        private boolean explode = true;
        private int maxDepth = FlatteningOptions.DEFAULT_MAX_DEPTH;
        private Map<String, String> options = Map.of();

        private Builder(String packageName) {
            this.packageName = packageName;
        }

        public Builder explode(boolean explode) {
            this.explode = explode;
            return this;
        }

        public Builder modelPackage(String modelPackage) {
            this.modelPackage = modelPackage == null || modelPackage.isBlank() ? null : modelPackage;
            return this;
        }

        public Builder grouping(Grouping grouping) {
            this.grouping = grouping;
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
            return new TargetOptions(packageName, modelPackage, grouping, explode, maxDepth, options);
        }
    }
}
