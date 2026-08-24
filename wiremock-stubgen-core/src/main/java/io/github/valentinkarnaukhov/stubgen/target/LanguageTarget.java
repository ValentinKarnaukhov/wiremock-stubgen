package io.github.valentinkarnaukhov.stubgen.target;

import io.github.valentinkarnaukhov.stubgen.spec.StubApi;

import java.util.List;

/**
 * Turns the language-neutral {@link StubApi} into source files for one target language.
 *
 * <p>Implementations are discovered with {@link java.util.ServiceLoader}, so adding a
 * language means adding a module on the classpath — the core and the build-tool
 * plugins stay untouched.
 */
public interface LanguageTarget {

    /**
     * Stable identifier used to select this target from build configuration,
     * for example {@code java} or {@code kotlin}.
     */
    String id();

    /**
     * Human-readable name for logs and error messages.
     */
    default String displayName() {
        return id();
    }

    /**
     * Produces the source files for the given API. Implementations must not write to
     * disk; persisting the result is the caller's responsibility.
     */
    List<GeneratedFile> generate(StubApi api, TargetOptions options);
}
