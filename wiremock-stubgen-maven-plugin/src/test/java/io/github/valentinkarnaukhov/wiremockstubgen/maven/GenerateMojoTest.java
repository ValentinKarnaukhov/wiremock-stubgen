package io.github.valentinkarnaukhov.wiremockstubgen.maven;

import io.github.valentinkarnaukhov.wiremockstubgen.fixtures.Fixtures;
import io.github.valentinkarnaukhov.wiremockstubgen.openapi.Composition;
import io.github.valentinkarnaukhov.wiremockstubgen.target.Grouping;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The one entry point every consumer goes through, and the only place the settings they
 * write in a pom turn into anything.
 *
 * <p>The example module proves the happy path end to end, but only that one: it runs on
 * the defaults. What is checked here is that each setting is wired to something, and that
 * a pom which cannot work says so rather than failing later and elsewhere.
 *
 * <p>Fields are set by reflection because that is exactly what Maven does with them. A
 * harness that started a real build would be testing Maven.
 */
class GenerateMojoTest {

    @TempDir
    Path output;

    private GenerateMojo mojo;
    private MavenProject project;
    private List<String> warnings;

    @BeforeEach
    void newMojo() {
        // Also called mid-test to run the mojo a second time with one setting changed.
        // The output directory is shared, so what the first run wrote has to go.
        clearOutput();
        mojo = new GenerateMojo();
        project = new MavenProject();
        warnings = new ArrayList<>();
        mojo.setLog(new SystemStreamLog() {
            @Override
            public void warn(CharSequence content) {
                warnings.add(content.toString());
            }
        });
        set("inputSpec", Fixtures.sampleApi().toString());
        set("language", "java");
        set("stubPackage", "demo.stubs");
        set("modelPackage", "demo.model");
        set("outputDirectory", output);
        set("grouping", Grouping.TAG);
        set("explode", true);
        set("maxDepth", 5);
        set("composition", Composition.MERGE);
        set("options", Map.<String, String>of());
        set("addTestSourceRoot", true);
        set("project", project);
    }

    @Test
    void writesStubsAndOffersThemToTheCompiler() throws Exception {
        mojo.execute();

        assertThat(files()).isNotEmpty();
        assertThat(project.getTestCompileSourceRoots()).contains(output.toString());
    }

    @Test
    void leavesTheSourceRootAloneWhenToldTo() throws Exception {
        set("addTestSourceRoot", false);

        mojo.execute();

        assertThat(files()).isNotEmpty();
        assertThat(project.getTestCompileSourceRoots()).doesNotContain(output.toString());
    }

    @Test
    void putsAStubUnderItsTagOrDirectlyInThePackage() throws Exception {
        mojo.execute();
        assertThat(relative()).isNotEmpty()
                .anySatisfy(path -> assertThat(path).doesNotMatch("demo/stubs/[^/]+\\.java"));

        newMojo();
        set("grouping", Grouping.NONE);
        mojo.execute();
        assertThat(relative()).isNotEmpty()
                .allSatisfy(path -> assertThat(path).matches("demo/stubs/[^/]+\\.java"));
    }

    @Test
    void offersNoBuildersWhenBodiesAreNotToBeExploded() throws Exception {
        mojo.execute();
        assertThat(sources()).anyMatch(source -> source.contains("BodyBuilder"));

        newMojo();
        set("explode", false);
        mojo.execute();
        assertThat(sources()).noneMatch(source -> source.contains("BodyBuilder"));
    }

    @Test
    void passesTheCompositionReadingThrough() throws Exception {
        set("composition", Composition.OPAQUE);
        set("inputSpec", Fixtures.compositionApi().toString());

        mojo.execute();

        assertThat(warnings).anySatisfy(warning -> assertThat(warning).contains("read as opaque"));
    }

    /**
     * The depth limit and its report, which nothing but the mojo puts in front of a
     * consumer.
     */
    @Test
    void saysWhenTheDepthLimitLeftSomethingUnreachable() throws Exception {
        set("maxDepth", 1);

        mojo.execute();

        assertThat(warnings).anySatisfy(warning ->
                assertThat(warning).contains("deeper than maxDepth 1"));
    }

    /**
     * Without a model package there is nothing to name a body, so every stub takes it
     * whole. A legitimate way to use the plugin and a common way to misuse it.
     */
    @Test
    void warnsThatWithoutModelClassesThereIsNothingToDescribe() throws Exception {
        set("modelPackage", null);

        mojo.execute();

        assertThat(warnings).anySatisfy(warning ->
                assertThat(warning).contains("modelPackage is not set"));
    }

    /**
     * A template the consumer supplied is used in place of the one on the classpath. The
     * option carrying it is target-specific and the mojo passes it through untouched,
     * which is the only thing that makes such settings reachable at all.
     */
    @Test
    void rendersFromTheConsumersOwnTemplateWhenThereIsOne(@TempDir Path templates) throws Exception {
        Files.writeString(templates.resolve("stub.mustache"), "// replaced\n", StandardCharsets.UTF_8);
        set("options", Map.of("templateDirectory", templates.toString()));

        mojo.execute();

        assertThat(sources()).isNotEmpty()
                .allSatisfy(source -> assertThat(source).isEqualTo("// replaced\n"));
    }

    @Test
    void refusesALanguageNothingImplements() {
        set("language", "cobol");

        assertThatThrownBy(mojo::execute)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("cobol");
    }

    @Test
    void refusesASpecificationItCannotRead() {
        set("inputSpec", output.resolve("absent.yaml").toString());

        assertThatThrownBy(mojo::execute).isInstanceOf(MojoExecutionException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private List<Path> files() throws IOException {
        try (Stream<Path> walk = Files.walk(output)) {
            return walk.filter(Files::isRegularFile).toList();
        }
    }

    private List<String> relative() throws IOException {
        return files().stream().map(file -> output.relativize(file).toString()).toList();
    }

    private List<String> sources() throws IOException {
        List<String> contents = new ArrayList<>();
        for (Path file : files()) {
            contents.add(Files.readString(file, StandardCharsets.UTF_8));
        }
        return contents;
    }

    private void clearOutput() {
        try (Stream<Path> walk = Files.walk(output)) {
            for (Path path : walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                if (!path.equals(output)) {
                    Files.delete(path);
                }
            }
        } catch (IOException e) {
            throw new AssertionError("could not empty the output directory", e);
        }
    }

    private void set(String name, Object value) {        try {
            Field field = GenerateMojo.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(mojo, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("no such mojo parameter: " + name, e);
        }
    }
}
