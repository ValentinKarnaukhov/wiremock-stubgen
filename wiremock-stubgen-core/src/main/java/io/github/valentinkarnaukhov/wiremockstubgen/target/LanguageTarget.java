package io.github.valentinkarnaukhov.wiremockstubgen.target;

import io.github.valentinkarnaukhov.wiremockstubgen.spec.StubApi;

import java.util.List;
import java.util.function.Consumer;

/**
 * Turns the language-neutral {@link StubApi} into source files for one target language.
 *
 * <p>Implementations are discovered with {@link java.util.ServiceLoader}, so adding a
 * language means adding a module on the classpath — the core and the build-tool
 * plugins stay untouched.
 */
public interface LanguageTarget {

    /** Stable identifier used to select this target from build configuration. */
    String id();

    /** Human-readable name for logs and error messages. */
    default String displayName() {
        return id();
    }

    /**
     * Produces the source files for the given API. Implementations must not write to
     * disk; persisting the result is the caller's responsibility.
     *
     * @param warnings where to report what a consumer would want to know and can act on —
     *                 a body left partly unreachable, a name that had to be changed.
     *                 Anything worse should throw; anything smaller should stay quiet.
     */
    List<GeneratedFile> generate(StubApi api, TargetOptions options, Consumer<String> warnings);
}
