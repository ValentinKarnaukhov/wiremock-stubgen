package io.github.valentinkarnaukhov.stubgen.maven;

import io.github.valentinkarnaukhov.stubgen.target.GeneratedFile;
import io.github.valentinkarnaukhov.stubgen.target.LanguageTarget;
import io.github.valentinkarnaukhov.stubgen.target.LanguageTargets;
import io.github.valentinkarnaukhov.stubgen.target.TargetOptions;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Generates WireMock stub builders from an OpenAPI specification.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    /** OpenAPI specification to read. */
    @Parameter(property = "stubgen.inputSpec", required = true)
    private String inputSpec;

    /** Target language, resolved through ServiceLoader. */
    @Parameter(property = "stubgen.language", defaultValue = "java")
    private String language;

    /** Package (or namespace) for the generated stubs. */
    @Parameter(property = "stubgen.packageName", defaultValue = "io.github.valentinkarnaukhov.stubgen.generated")
    private String packageName;

    /** Where generated sources are written. */
    @Parameter(property = "stubgen.outputDirectory",
            defaultValue = "${project.build.directory}/generated-test-sources/stubgen")
    private Path outputDirectory;

    /** Generate flattened accessors for nested model fields. */
    @Parameter(property = "stubgen.explode", defaultValue = "false")
    private boolean explode;

    /** How deep to descend when exploding. Ignored unless {@code explode} is set. */
    @Parameter(property = "stubgen.maxDepth", defaultValue = "3")
    private int maxDepth;

    /** Target-specific settings, passed through untouched. */
    @Parameter
    private Map<String, String> options = Map.of();

    /** Register the output directory as a test source root. */
    @Parameter(property = "stubgen.addTestSourceRoot", defaultValue = "true")
    private boolean addTestSourceRoot;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        LanguageTarget target;
        try {
            target = LanguageTargets.require(language, getClass().getClassLoader());
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        TargetOptions targetOptions = TargetOptions.builder(packageName)
                .explode(explode)
                .maxDepth(maxDepth)
                .options(options)
                .build();

        getLog().info("Generating %s stubs from %s".formatted(target.displayName(), inputSpec));

        // Reading the specification arrives with the parser stage; until then the
        // pipeline is wired but produces nothing.
        List<GeneratedFile> files = List.of();

        write(files);

        if (addTestSourceRoot) {
            project.addTestCompileSourceRoot(outputDirectory.toString());
        }

        getLog().info("Generated %d file(s) into %s".formatted(files.size(), outputDirectory));
    }

    private void write(List<GeneratedFile> files) throws MojoExecutionException {
        try {
            for (GeneratedFile file : files) {
                Path destination = outputDirectory.resolve(file.relativePath());
                Files.createDirectories(destination.getParent());
                Files.writeString(destination, file.content(), StandardCharsets.UTF_8);
            }
        } catch (IOException | UncheckedIOException e) {
            throw new MojoExecutionException("Failed to write generated sources to " + outputDirectory, e);
        }
    }
}
