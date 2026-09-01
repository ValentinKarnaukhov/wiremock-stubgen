package io.github.valentinkarnaukhov.wiremockstubgen.target;

import io.github.valentinkarnaukhov.wiremockstubgen.spec.StubApi;

import java.util.List;
import java.util.function.Consumer;

/**
 * Registered on the test classpath only, via {@code META-INF/services}, so
 * {@link LanguageTargets} has something real to find without this module depending on
 * {@code codegen-java} just to test its own lookup.
 */
public final class TestLanguageTarget implements LanguageTarget {

    @Override
    public String id() {
        return "test";
    }

    @Override
    public List<GeneratedFile> generate(StubApi api, TargetOptions options, Consumer<String> warnings) {
        return List.of();
    }
}
