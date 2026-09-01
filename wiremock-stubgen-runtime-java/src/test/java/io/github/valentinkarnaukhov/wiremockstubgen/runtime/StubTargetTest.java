package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the two ends {@link StubTarget#of} builds against a real server rather than a
 * stand-in for one: what a generated stub calls {@code register} on is either a
 * {@link WireMockServer} running in this JVM or a {@link WireMock} admin client talking to
 * one over HTTP, and both are cheap enough to run for real here.
 */
class StubTargetTest {

    private WireMockServer server;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    @Test
    void registersDirectlyWithAnInProcessServer() {
        StubTarget target = StubTarget.of(server);

        target.register(mapping());

        assertThat(server.listAllStubMappings().getMappings()).hasSize(1);
    }

    @Test
    void registersWithAnInProcessServerThroughASuppliedSerializer() {
        BodySerializer marker = body -> "marked";
        StubTarget target = StubTarget.of(server, marker);

        target.register(mapping());

        assertThat(server.listAllStubMappings().getMappings()).hasSize(1);
        assertThat(target.serializer()).isSameAs(marker);
    }

    @Test
    void registersOverTheAdminApiOfARemoteServer() {
        StubTarget target = StubTarget.of(new WireMock(server.port()));

        target.register(mapping());

        assertThat(server.listAllStubMappings().getMappings()).hasSize(1);
    }

    @Test
    void registersOverTheAdminApiThroughASuppliedSerializer() {
        BodySerializer marker = body -> "marked";
        StubTarget target = StubTarget.of(new WireMock(server.port()), marker);

        target.register(mapping());

        assertThat(server.listAllStubMappings().getMappings()).hasSize(1);
        assertThat(target.serializer()).isSameAs(marker);
    }

    @Test
    void defaultsToWireMocksOwnSerializerWhenNoneWasSupplied() {
        StubTarget target = mapping -> {
        };

        // wireMockDefault() is a fresh instance each call, so this checks what it does
        // rather than what it is: both render an ordinary value the same way.
        assertThat(target.serializer().serialize("x")).isEqualTo(BodySerializer.wireMockDefault().serialize("x"));
    }

    @Test
    void rejectsANullServerOrSerializer() {
        assertThatThrownBy(() -> StubTarget.of((WireMockServer) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StubTarget.of(server, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StubTarget.of((WireMockServer) null, body -> "x")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StubTarget.of((WireMock) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StubTarget.of(new WireMock(server.port()), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> StubTarget.of((WireMock) null, body -> "x")).isInstanceOf(NullPointerException.class);
    }

    private static StubMapping mapping() {
        return get(urlEqualTo("/x")).build();
    }
}
