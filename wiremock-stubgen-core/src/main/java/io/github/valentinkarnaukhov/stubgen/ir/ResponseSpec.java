package io.github.valentinkarnaukhov.stubgen.ir;

import java.util.Objects;
import java.util.Optional;

/**
 * One documented response of an operation.
 *
 * @param statusCode  HTTP status code, or {@code null} for the {@code default} response
 * @param body        the response body type; {@link TypeRef.Kind#UNKNOWN} when there is no body
 * @param description free-text description from the specification, may be {@code null}
 */
public record ResponseSpec(
        Integer statusCode,
        TypeRef body,
        String description) {

    public ResponseSpec {
        Objects.requireNonNull(body, "body");
    }

    /** {@code true} when this represents the OpenAPI {@code default} response. */
    public boolean isDefault() {
        return statusCode == null;
    }

    public Optional<Integer> statusCodeIfPresent() {
        return Optional.ofNullable(statusCode);
    }
}
