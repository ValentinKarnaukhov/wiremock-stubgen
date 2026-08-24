package io.github.valentinkarnaukhov.stubgen.runtime;

/**
 * Base class for the generated builders that describe a response body.
 *
 * <p>A response body builder writes into one model instance — the one its parent handed
 * it — and exposes one method per leaf of that schema. It adds nothing to
 * {@link AbstractBodyScope} today and exists so that the two directions are named rather
 * than one of them borrowing the other's base class. That also gives response-only
 * behaviour somewhere to go without being visible to matchers.
 *
 * @param <P> the level or stub {@link #exit()} returns to
 */
public abstract class AbstractResponseBodyBuilder<P> extends AbstractBodyScope<P> {

    protected AbstractResponseBodyBuilder(P parent, AbstractStub<?> root) {
        super(parent, root);
    }
}
