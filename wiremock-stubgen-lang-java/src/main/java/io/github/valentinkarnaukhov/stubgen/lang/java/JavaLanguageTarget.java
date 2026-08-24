package io.github.valentinkarnaukhov.stubgen.lang.java;

import io.github.valentinkarnaukhov.stubgen.ir.StubApi;
import io.github.valentinkarnaukhov.stubgen.spi.GeneratedFile;
import io.github.valentinkarnaukhov.stubgen.spi.LanguageTarget;
import io.github.valentinkarnaukhov.stubgen.spi.TargetOptions;

import java.util.List;
import java.util.Objects;

/**
 * Emits Java WireMock stub builders.
 *
 * <p>At this stage the target exists to pin down the module boundary and prove the
 * ServiceLoader wiring; source emission is added together with the IR reader and the
 * explode resolver.
 */
public final class JavaLanguageTarget implements LanguageTarget {

    public static final String ID = "java";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Java (WireMock)";
    }

    @Override
    public List<GeneratedFile> generate(StubApi api, TargetOptions options) {
        Objects.requireNonNull(api, "api");
        Objects.requireNonNull(options, "options");
        return List.of();
    }
}
