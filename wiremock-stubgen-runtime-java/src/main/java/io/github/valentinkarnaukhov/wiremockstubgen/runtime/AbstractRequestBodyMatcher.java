package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.matching.StringValuePattern;

import java.util.Objects;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;

/**
 * Base class for the generated builders that constrain a request body.
 *
 * <p>Shaped like {@link AbstractResponseBodyBuilder} so that the same schema reads the
 * same on both sides of a stub, but nothing is constructed here: a matcher only adds a
 * JSONPath expression, so a field five levels down is just a longer path. Each instance
 * carries the path it is rooted at, which lets one class per schema serve every position
 * that schema occupies. Matchers accumulate: WireMock ANDs repeated request-body patterns.
 *
 * <p>There is deliberately no "this whole node equals" method. In WireMock a value pattern
 * on a path ending in {@code [*]} is applied to the entire selection rendered as an array,
 * not to each element, so such a method would read as "some element equals this" and mean
 * "the whole list equals this". Matching a body as a whole stays on the stub.
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

    /** Requires the value at a path below this one to satisfy a pattern. */
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
