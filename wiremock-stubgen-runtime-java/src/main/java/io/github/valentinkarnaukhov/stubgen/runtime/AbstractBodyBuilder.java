package io.github.valentinkarnaukhov.stubgen.runtime;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.Objects;

/**
 * Base class for the generated builders that describe a response body.
 *
 * <p>A body builder writes into one model instance — the one its parent handed it — and
 * exposes one method per leaf of that schema. Nesting the builders instead of flattening
 * every path into the stub buys three things at once:
 *
 * <ul>
 *   <li>the status code leaves the accessor names, so two codes declaring the same schema
 *       share one builder instead of forcing two identical method sets;</li>
 *   <li>a builder is reached only through the method that created the body, so there is
 *       nothing to defer and no body of the wrong type to guard against;</li>
 *   <li>the caller never names an intermediate model type.</li>
 * </ul>
 *
 * <p>Generated builders are inner classes of the stub they belong to, so one operation
 * remains one file. That duplicates a schema's builder into every stub that mentions it
 * — measured at 1.7x more classes over 62 real specifications — and buys locality in
 * return: nothing has to be looked up in a shared package to read a stub.
 *
 * <p>The price is that configuration becomes ordered: once inside a body builder the
 * stub's own methods are out of scope until {@link #exit()}. That order is enforced by
 * the compiler rather than discovered at runtime, which is the trade this generator
 * exists to make.
 *
 * <p>{@link #mock()} and {@link #buildStub()} are repeated here so the common case — go
 * down into the body and finish — does not have to climb back out first.
 *
 * @param <P> the builder or stub to return to
 */
public abstract class AbstractBodyBuilder<P> {

    private final P parent;
    private final AbstractStub<?> root;

    protected AbstractBodyBuilder(P parent, AbstractStub<?> root) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.root = Objects.requireNonNull(root, "root");
    }

    /**
     * The stub this builder ultimately belongs to. Generated code passes it down when it
     * creates a nested builder, so every level can finish the chain.
     */
    protected final AbstractStub<?> root() {
        return root;
    }

    /**
     * Returns to the enclosing builder, or to the stub at the top level.
     */
    public final P exit() {
        return parent;
    }

    /**
     * Builds the mapping described by the owning stub, without registering it.
     */
    public final StubMapping buildStub() {
        return root.buildStub();
    }

    /**
     * Builds and registers the mapping described by the owning stub.
     */
    public final StubMapping mock() {
        return root.mock();
    }
}
