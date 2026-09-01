package io.github.valentinkarnaukhov.wiremockstubgen.spec;

import java.util.Objects;

/**
 * One documented response of an operation.
 *
 * @param statusCode  HTTP status code, or {@code null} for the {@code default} response
 * @param body        the response body type; {@link TypeRef.Kind#UNKNOWN} when there is no body
 * @param mediaType   the media type declared for {@code body}, exactly as the specification
 *                    wrote it; {@code null} when there is no body. A stub still matches and
 *                    builds every body as JSON regardless of this value — see
 *                    {@code OpenApiReader.jsonMediaType} — but the response's
 *                    {@code Content-Type} header should say what the specification actually
 *                    declared, not silently default to {@code application/json} when it
 *                    declared something else.
 */
public record Response(
        Integer statusCode,
        TypeRef body,
        String mediaType) {

    public Response {
        Objects.requireNonNull(body, "body");
        if (mediaType == null && body.kind() != TypeRef.Kind.UNKNOWN) {
            throw new IllegalArgumentException("a response with a body must carry the media type it was declared with");
        }
    }

    /** {@code true} when this represents the OpenAPI {@code default} response. */
    public boolean isDefault() {
        return statusCode == null;
    }
}
