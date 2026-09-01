package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.Objects;

/**
 * Where a generated stub sends its mappings.
 *
 * <p>WireMock is not always in-process: it is frequently a shared deployment that tests
 * talk to over the admin API. Generated code registers through this interface and does not
 * care which of the two it is.
 */
@FunctionalInterface
public interface StubTarget {

    /** Registers a single mapping. */
    void register(StubMapping mapping);

    /**
     * How a stub renders a body to JSON, for matching a request or building a response.
     * Defaults to WireMock's own mapper — see {@link BodySerializer} for what that does
     * and does not see, and when a consumer should supply their own instead.
     */
    default BodySerializer serializer() {
        return BodySerializer.wireMockDefault();
    }

    /** Targets a WireMock server running inside the current JVM. */
    static StubTarget of(WireMockServer server) {
        Objects.requireNonNull(server, "server");
        return server::addStubMapping;
    }

    /** As {@link #of(WireMockServer)}, but rendering bodies through {@code serializer}. */
    static StubTarget of(WireMockServer server, BodySerializer serializer) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(serializer, "serializer");
        return new StubTarget() {
            @Override
            public void register(StubMapping mapping) {
                server.addStubMapping(mapping);
            }

            @Override
            public BodySerializer serializer() {
                return serializer;
            }
        };
    }

    /** Targets a remote WireMock through an admin API client. */
    static StubTarget of(WireMock wireMock) {
        Objects.requireNonNull(wireMock, "wireMock");
        return wireMock::register;
    }

    /** As {@link #of(WireMock)}, but rendering bodies through {@code serializer}. */
    static StubTarget of(WireMock wireMock, BodySerializer serializer) {
        Objects.requireNonNull(wireMock, "wireMock");
        Objects.requireNonNull(serializer, "serializer");
        return new StubTarget() {
            @Override
            public void register(StubMapping mapping) {
                wireMock.register(mapping);
            }

            @Override
            public BodySerializer serializer() {
                return serializer;
            }
        };
    }
}
