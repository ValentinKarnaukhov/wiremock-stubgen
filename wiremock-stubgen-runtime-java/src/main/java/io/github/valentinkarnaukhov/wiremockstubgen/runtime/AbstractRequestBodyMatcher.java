package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.matching.StringValuePattern;

import java.util.Objects;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;

/**
 * Base class for the generated builders that constrain a request body.
 *
 * <p>The mirror image of {@link AbstractResponseBodyBuilder}, and deliberately shaped the
 * same way so that the same schema reads the same on both sides of a stub. The machinery
 * underneath is not the same at all: a response builder writes into a model instance,
 * whereas a matcher only ever adds a JSONPath expression. Nothing is constructed, so there
 * is no lazy parent creation here — a matcher for a field five levels down is just a
 * longer path.
 *
 * <p>Each instance carries the path it is rooted at, which is what lets one class per
 * schema serve every position that schema occupies. Matchers accumulate: WireMock ANDs
 * repeated request-body patterns together.
 *
 * <p>There is deliberately no "this whole node equals" method here. Verified against
 * WireMock: a value pattern on a path ending in {@code [*]} is applied to the entire
 * selection rendered as an array, not to each element, so such a method would read as
 * "some element equals this" and mean "the whole list equals this". Matching a body as a
 * whole therefore stays on the stub, which is the only object that unambiguously denotes
 * the whole document.
 *
 * @param <P> the level or stub exit() returns to
 */
public abstract class AbstractRequestBodyMatcher<P> extends AbstractBodyScope<P> {

    private final String path;
    private final Consumer<StringValuePattern> sink;

    protected AbstractRequestBodyMatcher(P parent, AbstractStub<?> root, String path,
                                  Consumer<StringValuePattern> sink) {
        super(parent, root);
        this.path = Objects.requireNonNull(path, "path");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * The JSONPath expression this matcher is rooted at — {@code $} at the top of a
     * body, {@code $.compositeList[*]} for an element of a nested list.
     */
    protected final String path() {
        return path;
    }

    /**
     * Requires the value at a path below this one to satisfy a pattern.
     */
    protected final void match(String relativePath, StringValuePattern pattern) {
        sink.accept(matchingJsonPath(path + relativePath, pattern));
    }

    /**
     * Requires a path below this one to select at least one node. Used for the filter
     * form, where the condition is inside the expression rather than beside it.
     */
    protected final void match(String relativePath) {
        sink.accept(matchingJsonPath(path + relativePath));
    }
}
