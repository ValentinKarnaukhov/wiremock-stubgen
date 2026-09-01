package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.common.Json;

import java.util.Objects;

/**
 * Turns a body — a request to match, or a response to send — into the JSON text a stub
 * puts on the wire or compares against.
 *
 * <p>Resolves what was previously an open question, recorded here because the reasoning
 * matters as much as the answer:
 *
 * <p>WireMock's own mapper ({@link #wireMockDefault()}) is a reasonable default — it
 * already bundles {@code jackson-datatype-jsr310}, so a type as ordinary as
 * {@code OffsetDateTime} serialises correctly out of the box — but it is WireMock's own
 * instance, not the consumer's. It does not carry whatever Jackson modules or
 * configuration the consumer's own generated client was built with — a {@code
 * JsonNullable} wrapper needs {@code jackson-databind-nullable} registered to serialise as
 * anything but an opaque object, for one — and under the {@code wiremock-standalone}
 * artifact Jackson is relocated, so it cannot be reached or reconfigured at all. A
 * consumer whose models need either supplies their own mapper here instead of AbstractStub
 * silently reaching for one that cannot see what it needs to.
 *
 * <p>Deciding this once, where {@link StubTarget} is already assembled, was chosen over
 * bundling a specific Jackson configuration (this project would then be guessing at every
 * consumer's setup) and over a per-stub override (every generated stub would need
 * subclassing to reach it, which the design otherwise never asks of a consumer).
 */
@FunctionalInterface
public interface BodySerializer {

    /** Renders {@code body} as JSON text. */
    String serialize(Object body);

    /**
     * WireMock's own mapper, reached the same way its own admin API does. Fine as long as
     * a body's types need nothing the consumer's own client configured that this mapper
     * does not already have.
     */
    static BodySerializer wireMockDefault() {
        return Json::write;
    }

    /** Renders through a consumer-supplied mapper instead — see the class documentation. */
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
