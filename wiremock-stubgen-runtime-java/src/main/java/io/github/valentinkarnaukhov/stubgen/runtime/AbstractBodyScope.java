package io.github.valentinkarnaukhov.stubgen.runtime;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.Objects;

/**
 * One level of a generated chain: it knows what created it and which stub it ultimately
 * belongs to, and that is all.
 *
 * <p>Named after the position rather than the direction because both halves of a stub
 * extend it — {@link AbstractResponseBodyBuilder} writes into a model instance,
 * {@link AbstractRequestBodyMatcher} adds a JSONPath condition, and nothing they share is
 * about producing or matching. An earlier arrangement had the matcher extend the response
 * builder, which made the base class claim a role it did not have.
 *
 * <p>Nesting the levels instead of flattening every path onto the stub buys three things
 * at once:
 *
 * <ul>
 *   <li>the status code leaves the accessor names, so two codes declaring the same schema
 *       share one builder instead of forcing two identical method sets;</li>
 *   <li>a level is reached only through the method that created it, so there is nothing
 *       to defer and no body of the wrong type to guard against;</li>
 *   <li>the caller never names an intermediate model type.</li>
 * </ul>
 *
 * <p>Generated levels are inner classes of the stub they belong to, so one operation
 * remains one file. That duplicates a schema's builder into every stub that mentions it —
 * measured at 1.7x more classes over 62 real specifications — and buys locality in
 * return: nothing has to be looked up in a shared package to read a stub.
 *
 * <p>The price is that configuration becomes ordered: once inside a level the stub's own
 * methods are out of scope until {@link #exit()}. That order is enforced by the compiler
 * rather than discovered at runtime, which is the trade this generator exists to make.
 *
 * <p>{@link #mock()} and {@link #buildStub()} are repeated here so the common case — go
 * down into the body and finish — does not have to climb back out first.
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

    /**
     * Returns to the enclosing level, or to the stub at the top.
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
