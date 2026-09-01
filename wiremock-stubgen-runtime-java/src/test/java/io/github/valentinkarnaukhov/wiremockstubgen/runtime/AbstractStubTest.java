package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The lifecycle every generated stub inherits rather than repeats: picking a status and
 * body, matching a request body as a whole, overriding the content type, reaching the raw
 * WireMock API, and finally building or registering the mapping. Generated code exercises
 * all of this constantly; this is where a change to it is caught on its own, without a
 * live server or a generated model in the way.
 */
class AbstractStubTest {

    @Test
    void codeAnswersTheGivenStatusWithNoBody() {
        TestStub stub = new TestStub(mapping -> {
        });

        StubMapping mapping = stub.answerWith(404).buildStub();

        assertThat(mapping.getResponse().getStatus()).isEqualTo(404);
        assertThat(mapping.getResponse().getBody()).isNull();
    }

    @Test
    void matchWholeRequestBodyComparesTheGivenObjectAsJson() {
        TestStub stub = new TestStub(mapping -> {
        });

        StubMapping mapping = stub.matchBody(Map.of("a", 1)).buildStub();

        assertThat(mapping.getRequest().getBodyPatterns()).hasSize(1);
        assertThat(mapping.getRequest().getBodyPatterns().get(0).getExpected())
                .isEqualTo(BodySerializer.wireMockDefault().serialize(Map.of("a", 1)));
    }

    @Test
    void addRequestBodyPatternEndsUpOnTheBuiltMapping() {
        TestStub stub = new TestStub(mapping -> {
        });

        stub.addPattern("first", com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath("$.a"));
        stub.addPattern("second", com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath("$.b"));

        assertThat(stub.buildStub().getRequest().getBodyPatterns()).hasSize(2);
    }

    @Test
    void addingASecondPatternUnderTheSameOwnerReplacesTheFirstRatherThanAddingToIt() {
        TestStub stub = new TestStub(mapping -> {
        });

        stub.addPattern("owner", com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath("$.a"));
        stub.addPattern("owner", com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath("$.b"));

        assertThat(stub.buildStub().getRequest().getBodyPatterns()).hasSize(1);
    }

    @Test
    void contentTypeOverridesTheResponseHeader() {
        TestStub stub = new TestStub(mapping -> {
        });

        StubMapping mapping = stub.respondWith("x").contentType("text/plain").buildStub();

        assertThat(mapping.getResponse().getHeaders().getHeader("Content-Type").firstValue())
                .isEqualTo("text/plain");
    }

    @Test
    void answersApplicationJsonWhenContentTypeWasNeverOverridden() {
        TestStub stub = new TestStub(mapping -> {
        });

        StubMapping mapping = stub.respondWith("x").buildStub();

        assertThat(mapping.getResponse().getHeaders().getHeader("Content-Type").firstValue())
                .isEqualTo("application/json");
    }

    @Test
    void customizeRunsAgainstTheFinishedMappingBuilder() {
        TestStub stub = new TestStub(mapping -> {
        });
        AtomicReference<MappingBuilder> seen = new AtomicReference<>();

        stub.respondWith("x").customize(seen::set).buildStub();

        assertThat(seen.get()).isNotNull();
    }

    @Test
    void customizersAccumulateAndRunInRegistrationOrder() {
        TestStub stub = new TestStub(mapping -> {
        });
        StringBuilder order = new StringBuilder();

        stub.respondWith("x")
                .customize(mappingBuilder -> order.append('a'))
                .customize(mappingBuilder -> order.append('b'))
                .buildStub();

        assertThat(order.toString()).isEqualTo("ab");
    }

    @Test
    void mockBuildsAndRegistersWithTheTarget() {
        AtomicReference<StubMapping> registered = new AtomicReference<>();
        TestStub stub = new TestStub(registered::set);

        StubMapping mapping = stub.respondWith("x").mock();

        assertThat(registered.get()).isSameAs(mapping);
    }

    private static final class TestStub extends AbstractStub<TestStub> {

        TestStub(StubTarget target) {
            super(target);
        }

        TestStub answerWith(int status) {
            return code(status);
        }

        TestStub respondWith(Object body) {
            return response(200, body);
        }

        TestStub matchBody(Object body) {
            return matchWholeRequestBody(body);
        }

        void addPattern(Object owner, com.github.tomakehurst.wiremock.matching.StringValuePattern pattern) {
            addRequestBodyPattern(owner, pattern);
        }

        @Override
        protected MappingBuilder toRequest() {
            return get(anyUrl());
        }
    }
}
