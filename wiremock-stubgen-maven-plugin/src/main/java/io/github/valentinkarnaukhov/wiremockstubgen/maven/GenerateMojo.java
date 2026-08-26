package io.github.valentinkarnaukhov.wiremockstubgen.maven;

import io.github.valentinkarnaukhov.wiremockstubgen.openapi.Composition;
import io.github.valentinkarnaukhov.wiremockstubgen.openapi.OpenApiReader;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.StubApi;
import io.github.valentinkarnaukhov.wiremockstubgen.target.GeneratedFile;
import io.github.valentinkarnaukhov.wiremockstubgen.target.Grouping;
import io.github.valentinkarnaukhov.wiremockstubgen.target.LanguageTarget;
import io.github.valentinkarnaukhov.wiremockstubgen.target.LanguageTargets;
import io.github.valentinkarnaukhov.wiremockstubgen.target.TargetOptions;
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

/** Generates WireMock stub builders from an OpenAPI specification. */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    /** OpenAPI specification to read. */
    @Parameter(property = "wiremock-stubgen.inputSpec", required = true)
    private String inputSpec;

    /** Target language, resolved through ServiceLoader. */
    @Parameter(property = "wiremock-stubgen.language", defaultValue = "java")
    private String language;

    /**
     * Package (or namespace) for the generated stubs. Named the way openapi-generator
     * names its own destinations, next to which this one is configured.
     */
    @Parameter(property = "wiremock-stubgen.stubPackage", defaultValue = "io.github.valentinkarnaukhov.wiremockstubgen.generated")
    private String stubPackage;

    /** Where generated sources are written. */
    @Parameter(property = "wiremock-stubgen.outputDirectory",
            defaultValue = "${project.build.directory}/generated-test-sources/wiremock-stubgen")
    private Path outputDirectory;

    /** Where the consumer's own model classes live. Body types are referenced, never generated. */
    @Parameter(property = "wiremock-stubgen.modelPackage")
    private String modelPackage;

    /** How generated stubs are laid out under {@code stubPackage}: TAG or NONE. */
    @Parameter(property = "wiremock-stubgen.grouping", defaultValue = "TAG")
    private Grouping grouping;

    /**
     * Describe bodies field by field as well as whole. Switched off, a stub only offers
     * the forms that take a model the caller already has, and no builders or matchers
     * are generated.
     */
    @Parameter(property = "wiremock-stubgen.explode", defaultValue = "true")
    private boolean explode;

    /**
     * How many property hops a body scope flattens through. Ignored unless
     * {@code explode} is set.
     */
    @Parameter(property = "wiremock-stubgen.maxDepth", defaultValue = "5")
    private int maxDepth;

    /**
     * What to make of a schema written with {@code oneOf} or {@code anyOf}. Set this to
     * {@code OPAQUE} if openapi-generator is being run with
     * {@code useOneOfInterfaces=true}, under which such a schema becomes an empty
     * interface with no setters to call.
     */
    @Parameter(property = "wiremock-stubgen.composition", defaultValue = "MERGE")
    private Composition composition;

    /** Target-specific settings, passed through untouched. */
    @Parameter
    private Map<String, String> options = Map.of();

    /**
     * Compile the stubs for the test classpath only, so they do not reach the artifact.
     * This is the default because a stub is test scaffolding and drags WireMock in with it.
     */
    @Parameter(property = "wiremock-stubgen.addTestCompileSourceRoot", defaultValue = "true")
    private boolean addTestCompileSourceRoot;

    /**
     * Compile the stubs into the artifact instead, for a client library that publishes
     * stubs for its consumers to test against. Cannot be combined with
     * {@code addTestCompileSourceRoot}.
     */
    @Parameter(property = "wiremock-stubgen.addCompileSourceRoot", defaultValue = "false")
    private boolean addCompileSourceRoot;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        if (addCompileSourceRoot && addTestCompileSourceRoot) {
            throw new MojoExecutionException("addCompileSourceRoot and addTestCompileSourceRoot"
                    + " both ask for the same directory and only one can have it."
                    + " Turn addTestCompileSourceRoot off to publish the stubs in the artifact.");
        }

        LanguageTarget target;
        try {
            target = LanguageTargets.require(language, getClass().getClassLoader());
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        TargetOptions targetOptions = TargetOptions.builder(stubPackage)
                .modelPackage(modelPackage)
                .grouping(grouping)
                .explode(explode)
                .maxDepth(maxDepth)
                .options(options)
                .build();

        getLog().info("Generating %s stubs from %s".formatted(target.displayName(), inputSpec));

        StubApi api;
        try {
            api = new OpenApiReader(getLog()::warn, composition).read(inputSpec);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        getLog().info("Read %d operation(s) and %d reachable schema(s) from '%s'"
                .formatted(api.operations().size(), api.reachableSchemas().size(), api.title()));

        if (targetOptions.modelPackageIfPresent().isEmpty()) {
            getLog().warn("modelPackage is not set, so there are no model classes to name:"
                    + " stubs will take each body whole, as java.lang.Object, and offer no"
                    + " accessors into it whatever explode says."
                    + " Point modelPackage at the package openapi-generator writes your"
                    + " client models into.");
        }

        List<GeneratedFile> files = target.generate(api, targetOptions, getLog()::warn);

        write(files);

        if (addCompileSourceRoot) {
            project.addCompileSourceRoot(outputDirectory.toString());
        } else if (addTestCompileSourceRoot) {
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
