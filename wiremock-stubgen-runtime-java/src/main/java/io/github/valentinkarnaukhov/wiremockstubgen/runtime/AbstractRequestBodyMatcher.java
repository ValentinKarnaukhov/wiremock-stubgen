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

    /**
     * Renders a value as a JSONPath literal, for the filter form where the value is part
     * of the expression rather than a pattern beside it.
     *
     * <p>Text is quoted, and a quote or backslash inside it escaped: otherwise a value as
     * ordinary as {@code O'Reilly} ends the expression early and the request fails to
     * parse rather than failing to match. That part is load-bearing and pinned by a test.
     *
     * <p>Numbers and booleans are written bare because that is the literal the filter's
     * own type rules describe. It was measured that the implementation WireMock uses today
     * compares {@code '3'} to {@code 3} as equal, so quoting them would also work; that
     * coercion is a property of one implementation rather than something JSONPath promises.
     */
    protected static String literal(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        String text = value.toString().replace("\\", "\\\\").replace("'", "\\'");
        return "'" + text + "'";
    }
}
