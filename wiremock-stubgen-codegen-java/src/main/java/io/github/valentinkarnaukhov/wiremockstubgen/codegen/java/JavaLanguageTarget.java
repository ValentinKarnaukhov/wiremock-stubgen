package io.github.valentinkarnaukhov.wiremockstubgen.codegen.java;

import io.github.valentinkarnaukhov.wiremockstubgen.spec.StubApi;
import io.github.valentinkarnaukhov.wiremockstubgen.target.GeneratedFile;
import io.github.valentinkarnaukhov.wiremockstubgen.target.LanguageTarget;
import io.github.valentinkarnaukhov.wiremockstubgen.target.TargetOptions;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Emits Java WireMock stub builders.
 *
 * <p>Only the typed surface is emitted — a method per parameter, per status code and per
 * body. Everything those methods do at runtime is inherited from the runtime library,
 * because anything written into a generated file is a thing a consumer cannot fix without
 * regenerating.
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
    public List<GeneratedFile> generate(StubApi api, TargetOptions options, Consumer<String> warnings) {
        Objects.requireNonNull(api, "api");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(warnings, "warnings");
        StubEmitter emitter = new StubEmitter(api, options, warnings);
        return api.operations().stream().map(emitter::emit).toList();
    }
}
