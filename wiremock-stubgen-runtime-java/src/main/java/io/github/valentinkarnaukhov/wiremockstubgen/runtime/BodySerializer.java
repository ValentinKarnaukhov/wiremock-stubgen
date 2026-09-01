package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.common.Json;

import java.util.Objects;

/**
 * Turns a body into the JSON text a stub sends or matches against.
 *
 * <p>Defaults to WireMock's own Jackson instance, which already handles ordinary types
 * like {@code OffsetDateTime} but won't see a consumer's own modules (e.g.
 * {@code jackson-databind-nullable} for {@code JsonNullable}) and is unreachable at all
 * under {@code wiremock-standalone}, where Jackson is relocated. Pass {@link #of} a
 * consumer-supplied mapper when the default doesn't cover what a body needs.
 */
@FunctionalInterface
public interface BodySerializer {

    /** Renders {@code body} as JSON text. */
    String serialize(Object body);

    /** WireMock's own mapper. */
    static BodySerializer wireMockDefault() {
        return Json::write;
    }

    /** Renders through a consumer-supplied mapper instead. */
    static BodySerializer of(com.fasterxml.jackson.databind.ObjectMapper mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return body -> {
            try {
                return mapper.writeValueAsString(body);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IllegalArgumentException("could not serialise " + body, e);
            }
        };
    }
}
