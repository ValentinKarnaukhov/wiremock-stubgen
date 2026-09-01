package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A level's own four methods: which stub it ultimately belongs to, returning to what
 * created it, and the two ways of finishing that exist so the common case does not have
 * to climb back out to the stub first. {@link AbstractResponseBodyBuilder} and
 * {@link AbstractRequestBodyMatcher} both inherit these unchanged, so a builder-shaped
 * double is enough to exercise them without either half of the real thing.
 */
class AbstractBodyScopeTest {

    @Test
    void rootIsTheStubTheLevelWasGivenAtConstruction() {
        TestStub stub = new TestStub(mapping -> {
        });
        TestLevel level = new TestLevel("parent", stub);

        assertThat(level.rootOf()).isSameAs(stub);
    }

    @Test
    void exitReturnsWhateverCreatedThisLevel() {
        TestStub stub = new TestStub(mapping -> {
        });
        TestLevel level = new TestLevel("parent", stub);

        assertThat(level.exit()).isEqualTo("parent");
    }

    @Test
    void buildStubDelegatesToTheOwningStub() {
        TestStub stub = new TestStub(mapping -> {
        });
        TestLevel level = new TestLevel("parent", stub);

        StubMapping mapping = level.buildStub();

        assertThat(mapping.getRequest().getMethod().getName()).isEqualTo("GET");
    }

    @Test
    void mockDelegatesToTheOwningStubAndRegistersWithItsTarget() {
        AtomicReference<StubMapping> registered = new AtomicReference<>();
        TestStub stub = new TestStub(registered::set);
        TestLevel level = new TestLevel("parent", stub);

        StubMapping mapping = level.mock();

        assertThat(registered.get()).isSameAs(mapping);
    }

    private static final class TestLevel extends AbstractResponseBodyBuilder<String> {
        TestLevel(String parent, AbstractStub<?> root) {
            super(parent, root);
        }

        AbstractStub<?> rootOf() {
            return root();
        }
    }

    private static final class TestStub extends AbstractStub<TestStub> {
        TestStub(StubTarget target) {
            super(target);
        }

        @Override
        protected MappingBuilder toRequest() {
            return get(anyUrl());
        }
    }
}
