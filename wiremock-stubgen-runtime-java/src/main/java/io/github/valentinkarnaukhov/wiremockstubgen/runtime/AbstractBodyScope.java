package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.Objects;

/**
 * One level of a generated chain: it knows what created it and which stub it ultimately
 * belongs to, and that is all.
 *
 * <p>Named after the position rather than the direction because both halves of a stub
 * extend it — {@link AbstractResponseBodyBuilder} writes into a model instance,
 * {@link AbstractRequestBodyMatcher} adds a JSONPath condition — and nothing they share is
 * about producing or matching.
 *
 * <p>Nesting the levels rather than flattening every path onto the stub keeps the status
 * code out of accessor names, so two codes declaring one schema share a builder; a level
 * is reached only through the method that created it; and the caller never names an
 * intermediate model type. Generated levels are inner classes of their stub, so one
 * operation stays one file, at the cost of duplicating a schema's builder into every stub
 * that mentions it.
 *
 * <p>The price is that configuration becomes ordered: once inside a level the stub's own
 * methods are out of scope until {@link #exit()}. {@link #mock()} and {@link #buildStub()}
 * are repeated here so the common case does not have to climb back out first.
 *
 * @param <P> the level or stub to return to
 */
public abstract class AbstractBodyScope<P> {

    private final P parent;
    private final AbstractStub<?> root;

    protected AbstractBodyScope(P parent, AbstractStub<?> root) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.root = Objects.requireNonNull(root, "root");
    }

    /**
     * The stub this level ultimately belongs to. Generated code passes it down when it
     * creates a nested level, so every one of them can finish the chain.
     */
    protected final AbstractStub<?> root() {
        return root;
    }

    /** Returns to the enclosing level, or to the stub at the top. */
    public final P exit() {
        return parent;
    }

    /** Builds the mapping described by the owning stub, without registering it. */
    public final StubMapping buildStub() {
        return root.buildStub();
    }

    /** Builds and registers the mapping described by the owning stub. */
    public final StubMapping mock() {
        return root.mock();
    }
}
