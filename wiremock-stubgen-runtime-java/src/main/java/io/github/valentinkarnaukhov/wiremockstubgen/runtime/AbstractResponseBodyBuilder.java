package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

/**
 * Base class for the generated builders that describe a response body.
 *
 * <p>Writes into the one model instance its parent handed it, exposing a method per leaf
 * of that schema. It adds nothing to {@link AbstractBodyScope} today and exists so the two
 * directions are named rather than one borrowing the other's base class.
 *
 * @param <P> the level or stub {@link #exit()} returns to
 */
public abstract class AbstractResponseBodyBuilder<P> extends AbstractBodyScope<P> {

    protected AbstractResponseBodyBuilder(P parent, AbstractStub<?> root) {
        super(parent, root);
    }
}
