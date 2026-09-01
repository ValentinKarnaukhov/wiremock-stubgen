package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.Objects;

/**
 * One level of a generated fluent chain: knows its parent and the stub it belongs to.
 *
 * <p>Shared by {@link AbstractResponseBodyBuilder} and {@link AbstractRequestBodyMatcher}
 * so a schema keeps one nested class regardless of how many response codes or operations
 * use it. {@link #mock()} and {@link #buildStub()} are repeated here so a caller doesn't
 * have to {@link #exit()} first just to finish the chain.
 *
 * @param <P> the level or stub {@link #exit()} returns to
 */
public abstract class AbstractBodyScope<P> {

    private final P parent;
    private final AbstractStub<?> root;

    protected AbstractBodyScope(P parent, AbstractStub<?> root) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.root = Objects.requireNonNull(root, "root");
    }

    /** The stub this level ultimately belongs to. */
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
