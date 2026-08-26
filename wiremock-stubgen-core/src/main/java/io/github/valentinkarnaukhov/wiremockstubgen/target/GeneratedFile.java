package io.github.valentinkarnaukhov.wiremockstubgen.target;

import java.util.Objects;

/**
 * One file produced by a language target.
 *
 * @param relativePath path relative to the configured output directory,
 *                     using {@code /} as separator, for example
 *                     {@code com/example/stub/UserApiStub.java}
 * @param content      full file content
 */
public record GeneratedFile(String relativePath, String content) {

    public GeneratedFile {
        Objects.requireNonNull(relativePath, "relativePath");
        Objects.requireNonNull(content, "content");
        if (relativePath.isBlank()) {
            throw new IllegalArgumentException("relativePath must not be blank");
        }
    }
}
