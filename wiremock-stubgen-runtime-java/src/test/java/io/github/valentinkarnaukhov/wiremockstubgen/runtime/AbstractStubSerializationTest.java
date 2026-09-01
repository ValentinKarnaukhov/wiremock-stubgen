package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A stub renders every body through {@link StubTarget#serializer()} rather than a mapper
 * fixed here, so a consumer whose models need a Jackson module WireMock's own instance
 * does not have can supply one, once, where the target is assembled.
 */
class AbstractStubSerializationTest {

    @Test
    void rendersAResponseBodyThroughWireMocksOwnMapperByDefault() {
        TestStub stub = new TestStub(mapping -> {
        });

        StubMapping mapping = stub.respondWith(Map.of("a", 1)).buildStub();

        assertThat(mapping.getResponse().getTextBody()).isEqualTo(BodySerializer.wireMockDefault().serialize(Map.of("a", 1)));
    }

    @Test
    void rendersThroughTheTargetsOwnSerializerWhenOneWasSupplied() {
        // A distinguishable stand-in for "the consumer's own mapper": upper-cases the
        // text WireMock's own mapper would have produced, so the difference is visible
        // without needing a real Jackson module to make the point.
        BodySerializer shouting = body -> BodySerializer.wireMockDefault().serialize(body).toUpperCase(java.util.Locale.ROOT);
        TestStub stub = new TestStub(new StubTarget() {
            @Override
            public void register(StubMapping mapping) {
            }

            @Override
            public BodySerializer serializer() {
                return shouting;
            }
        });

        StubMapping mapping = stub.respondWith(Map.of("a", "b")).buildStub();

        assertThat(mapping.getResponse().getTextBody()).isEqualTo(shouting.serialize(Map.of("a", "b")));
        assertThat(mapping.getResponse().getTextBody()).isNotEqualTo(BodySerializer.wireMockDefault().serialize(Map.of("a", "b")));
    }

    @Test
    void wireMockDefaultAlreadyHandlesWhatItBundlesAJacksonModuleFor() {
        // Measured before deciding this needed no bundled fallback of our own: WireMock's
        // own mapper already registers jackson-datatype-jsr310, so an OffsetDateTime
        // serialises correctly without any consumer-supplied mapper at all.
        java.time.OffsetDateTime when = java.time.OffsetDateTime.parse("2024-01-02T10:15:00+01:00");
        assertThat(BodySerializer.wireMockDefault().serialize(Map.of("when", when)))
                .contains("2024-01-02T10:15:00+01:00");
    }

    @Test
    void ofWrapsAConsumerSuppliedObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        BodySerializer serializer = BodySerializer.of(mapper);

        assertThat(serializer.serialize(Map.of("a", 1))).isEqualTo("{\"a\":1}");
    }

    @Test
    void ofWrapsAJacksonFailureRatherThanLettingTheCheckedExceptionEscape() {
        // A cyclic structure is something Jackson's default configuration refuses to
        // serialise -- infinite recursion, detected rather than run into -- which is
        // enough on its own to reach the catch block without needing a broken mapper.
        Cyclic cyclic = new Cyclic();
        cyclic.self = cyclic;
        BodySerializer serializer = BodySerializer.of(new ObjectMapper());

        assertThatThrownBy(() -> serializer.serialize(cyclic))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("could not serialise")
                .cause().isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    }

    private static final class Cyclic {
        public Cyclic self;
    }

    private static final class TestStub extends AbstractStub<TestStub> {

        TestStub(StubTarget target) {
            super(target);
        }

        TestStub respondWith(Object body) {
            return response(200, body);
        }

        @Override
        protected MappingBuilder toRequest() {
            return get(anyUrl());
        }
    }
}
