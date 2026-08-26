package io.github.valentinkarnaukhov.stubgen.fixtures;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Hands the sample specification to a test as a file on disk.
 *
 * <p>The specification is a classpath resource, because that is the only way it can travel
 * to another module. swagger-parser resolves {@code $ref} against a base location and so
 * wants a path, not a stream. Somebody therefore has to write the resource out, and before
 * this module existed every consumer did it for itself. Doing it here means one copy of
 * that code and one temporary file per JVM instead of one per test class.
 *
 * <p>Anything a test may write to would have to be copied per call. Nothing here is
 * writable: the file is the fixture, read-only by convention, and callers share it.
 */
public final class Fixtures {

    /**
     * The specification every layer of the project is measured against: seventeen
     * operations chosen to be awkward rather than realistic. Reserved words as parameter
     * and property names, a dashed property, an operation answering with several error
     * codes, three shapes of recursion and a chain deep enough to run past the depth
     * limit. It is a test fixture and reads like one — the example module has a
     * specification meant for humans.
     */
    public static Path sampleApi() {
        return SampleApi.PATH;
    }

    private Fixtures() {
    }

    private static final class SampleApi {
        private static final Path PATH = materialise("/specs/sample-api.yaml", "sample-api", ".yaml");
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
