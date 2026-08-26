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

    /** Targets a WireMock server running inside the current JVM. */
    static StubTarget of(WireMockServer server) {
        Objects.requireNonNull(server, "server");
        return server::addStubMapping;
    }

    /** Targets a remote WireMock through an admin API client. */
    static StubTarget of(WireMock wireMock) {
        Objects.requireNonNull(wireMock, "wireMock");
        return wireMock::register;
    }
}
