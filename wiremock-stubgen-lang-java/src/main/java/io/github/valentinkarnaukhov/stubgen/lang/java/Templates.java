package io.github.valentinkarnaukhov.stubgen.lang.java;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and renders the Mustache templates the Java target emits from.
 *
 * <p>Templates are Mustache for one reason above all others: the people who will use this
 * plugin already override openapi-generator's Mustache templates for the client the stubs
 * are typed against. Reusing that syntax means a generated file is something they can take
 * over rather than something they have to live with — which is most of why a generator is
 * tolerated at all.
 *
 * <p>A template is looked up in the directory named by the {@code templateDirectory}
 * option first and on the classpath second, so a consumer may replace one template without
 * having to supply the rest.
 */
final class Templates {

    private static final String CLASSPATH_ROOT = "/templates/java/";

    private static final String SUFFIX = ".mustache";

    private final Optional<Path> overrides;

    private final Mustache.Compiler compiler;

    private final Map<String, Template> compiled = new ConcurrentHashMap<>();

    Templates(Optional<Path> overrides) {
        this.overrides = overrides;
        // Escaping is off because the output is Java, not markup: a body type such as
        // List<Item> must survive rendering as it was written.
        this.compiler = Mustache.compiler()
                .escapeHTML(false)
                .withLoader(this::open);
    }

    static Templates from(io.github.valentinkarnaukhov.stubgen.target.TargetOptions options) {
        return new Templates(options.option("templateDirectory").map(Path::of));
    }

    String render(String name, Object view) {
        return compiled.computeIfAbsent(name, this::compile).execute(view);
    }

    private Template compile(String name) {
        try (Reader reader = open(name)) {
            return compiler.compile(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read template " + name, e);
        }
    }

    private Reader open(String name) {
        Optional<Path> override = overrides.map(dir -> dir.resolve(name + SUFFIX)).filter(Files::isRegularFile);
        if (override.isPresent()) {
            try {
                return Files.newBufferedReader(override.get(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read template " + override.get(), e);
            }
        }
        InputStream stream = Templates.class.getResourceAsStream(CLASSPATH_ROOT + name + SUFFIX);
        if (stream == null) {
            throw new IllegalStateException("No template named " + name + " on the classpath");
        }
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }
}
