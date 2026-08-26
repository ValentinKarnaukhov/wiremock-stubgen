package io.github.valentinkarnaukhov.wiremockstubgen.spec;

import java.util.Objects;

/**
 * One documented response of an operation.
 *
 * @param statusCode  HTTP status code, or {@code null} for the {@code default} response
 * @param body        the response body type; {@link TypeRef.Kind#UNKNOWN} when there is no body
 */
public record Response(
        Integer statusCode,
        TypeRef body) {

    public Response {
        Objects.requireNonNull(body, "body");
    }

    /** {@code true} when this represents the OpenAPI {@code default} response. */
    public boolean isDefault() {
        return statusCode == null;
    }
}
