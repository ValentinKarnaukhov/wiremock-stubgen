package io.github.valentinkarnaukhov.stubgen.runtime;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Base class for generated stub builders.
 *
 * <p>Carries the parts every generated builder needs regardless of operation: where to
 * register, and an escape hatch onto the raw WireMock API. Keeping this hand-written
 * means fixing a bug here does not require regenerating anything downstream.
 *
 * @param <S> the concrete builder type, so fluent methods keep the caller's type
 */
public abstract class AbstractStub<S extends AbstractStub<S>> {

    private final StubTarget target;
    private Consumer<MappingBuilder> customizer = mappingBuilder -> {
    };

    protected AbstractStub(StubTarget target) {
        this.target = Objects.requireNonNull(target, "target");
    }

    @SuppressWarnings("unchecked")
    protected final S self() {
        return (S) this;
    }

    /**
     * Applies arbitrary WireMock configuration that the generated API does not model —
     * delays, priorities, scenarios, fault injection and so on.
     *
     * <p>Deliberate escape hatch: the generated surface covers the common cases, and
     * anything beyond them stays reachable without abandoning the generated builder.
     * Customisers accumulate in registration order.
     */
    public final S customize(Consumer<MappingBuilder> customizer) {
        Objects.requireNonNull(customizer, "customizer");
        this.customizer = this.customizer.andThen(customizer);
        return self();
    }

    /**
     * Builds the mapping described by this builder, without registering it.
     */
    public final StubMapping buildStub() {
        MappingBuilder mappingBuilder = toMappingBuilder();
        customizer.accept(mappingBuilder);
        return mappingBuilder.build();
    }

    /**
     * Builds and registers the mapping.
     */
    public final StubMapping mock() {
        StubMapping mapping = buildStub();
        target.register(mapping);
        return mapping;
    }

    /**
     * Translates the accumulated state into a WireMock mapping. Implemented by
     * generated subclasses.
     */
    protected abstract MappingBuilder toMappingBuilder();
}
