package io.github.valentinkarnaukhov.stubgen.it;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.valentinkarnaukhov.stubgen.runtime.AbstractStub;
import io.github.valentinkarnaukhov.stubgen.runtime.StubTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the runtime actually drives a real WireMock, rather than only compiling
 * against it. Stands in for generated code until the emitter exists.
 */
class RuntimeAgainstWireMockIT {

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

    /** Hand-written stand-in for what the generator will emit. */
    private static final class GreetingStub extends AbstractStub<GreetingStub> {
        private String name = "world";

        private GreetingStub(StubTarget target) {
            super(target);
        }

        GreetingStub name(String name) {
            this.name = name;
            return self();
        }

        @Override
        protected MappingBuilder toMappingBuilder() {
            return WireMock.get(WireMock.urlPathEqualTo("/greeting"))
                    .withQueryParam("name", WireMock.equalTo(name))
                    .willReturn(WireMock.aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/plain")
                            .withBody("hello " + name));
        }
    }

    @Test
    void registeredStubServesTraffic() throws IOException, InterruptedException {
        new GreetingStub(StubTarget.of(server)).name("valentin").mock();

        HttpResponse<String> response = get("/greeting?name=valentin");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("hello valentin");
    }

    @Test
    void customizeAffectsTheServedStub() throws IOException, InterruptedException {
        new GreetingStub(StubTarget.of(server))
                .name("valentin")
                .customize(builder -> builder.willReturn(WireMock.aResponse().withStatus(503)))
                .mock();

        assertThat(get("/greeting?name=valentin").statusCode()).isEqualTo(503);
    }

    @Test
    void nonMatchingRequestIsNotServed() throws IOException, InterruptedException {
        new GreetingStub(StubTarget.of(server)).name("valentin").mock();

        assertThat(get("/greeting?name=someone-else").statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(server.baseUrl() + path))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
