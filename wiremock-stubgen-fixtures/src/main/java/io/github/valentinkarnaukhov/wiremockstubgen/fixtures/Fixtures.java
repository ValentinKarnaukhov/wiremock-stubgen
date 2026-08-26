package io.github.valentinkarnaukhov.wiremockstubgen.fixtures;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Hands the sample specification to a test as a file on disk.
 *
 * <p>The specification is a classpath resource, because that is the only way it can travel
 * to another module, but swagger-parser resolves {@code $ref} against a base location and
 * so wants a path. Doing the write-out here means one temporary file per JVM rather than
 * one per test class. Nothing here is writable: callers share the file.
 */
public final class Fixtures {

    /**
     * The specification every layer of the project is measured against, chosen to be
     * awkward rather than realistic: reserved words as parameter and property names, a
     * dashed property, an operation answering with several error codes, three shapes of
     * recursion and a chain deep enough to run past the depth limit.
     */
    public static Path sampleApi() {
        return SampleApi.PATH;
    }

    /**
     * The specification for the rules a reader has to follow when a document is written
     * the way large real ones are: compositions, schemas shared by reference, bodies
     * written out in place, names given to things that are not classes.
     *
     * <p>Separate from {@link #sampleApi()} because that one is the source of the golden
     * stubs and should not change every time a new rule needs covering.
     */
    public static Path compositionApi() {
        return CompositionApi.PATH;
    }

    private Fixtures() {
    }

    private static final class SampleApi {
        private static final Path PATH = materialise("/specs/sample-api.yaml", "sample-api", ".yaml");
    }

    private static final class CompositionApi {
        private static final Path PATH = materialise("/specs/composition-api.yaml", "composition-api", ".yaml");
    }

    private static Path materialise(String resource, String prefix, String suffix) {
        try (InputStream in = Fixtures.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(resource + " is not on the classpath;"
                        + " wiremock-stubgen-fixtures is probably missing from the test dependencies");
            }
            Path file = Files.createTempFile(prefix, suffix);
            file.toFile().deleteOnExit();
            Files.write(file, in.readAllBytes());
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("could not write " + resource + " out for a test to read", e);
        }
    }
}
