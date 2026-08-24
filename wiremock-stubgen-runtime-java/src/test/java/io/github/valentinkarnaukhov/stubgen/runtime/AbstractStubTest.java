package io.github.valentinkarnaukhov.stubgen.runtime;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractStubTest {

    /** Minimal stand-in for a generated subclass. */
    private static final class TestStub extends AbstractStub<TestStub> {
        private TestStub(StubTarget target) {
            super(target);
        }

        @Override
        protected MappingBuilder toMappingBuilder() {
            return WireMock.get(WireMock.urlEqualTo("/things"))
                    .willReturn(WireMock.aResponse().withStatus(200));
        }
    }

    @Test
    void mockRegistersOnTheGivenTarget() {
        List<StubMapping> registered = new ArrayList<>();

        StubMapping returned = new TestStub(registered::add).mock();

        assertThat(registered).containsExactly(returned);
        assertThat(returned.getRequest().getUrl()).isEqualTo("/things");
    }

    @Test
    void buildStubDoesNotRegister() {
        List<StubMapping> registered = new ArrayList<>();

        new TestStub(registered::add).buildStub();

        assertThat(registered).isEmpty();
    }

    @Test
    void customizeReachesTheRawMappingBuilder() {
        StubMapping mapping = new TestStub(m -> {
        })
                .customize(builder -> builder.atPriority(7))
                .buildStub();

        assertThat(mapping.getPriority()).isEqualTo(7);
    }

    @Test
    void customizersApplyInOrder() {
        StubMapping mapping = new TestStub(m -> {
        })
                .customize(builder -> builder.atPriority(1))
                .customize(builder -> builder.atPriority(2))
                .buildStub();

        assertThat(mapping.getPriority()).isEqualTo(2);
    }
}
