package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.matching.StringValuePattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
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
 * <p>A path ending in {@code [*]} means this instance's own position is some element of
 * an array — reached through a nested-list accessor, or because the whole body is a list.
 * Every condition asked of such an instance is folded into one combined filter instead of
 * becoming an independent condition, because independent conditions on an array can each
 * be satisfied by a different element; see {@link #condition(String)}.
 *
 * @param <P> the level or stub exit() returns to
 */
public abstract class AbstractRequestBodyMatcher<P> extends AbstractBodyScope<P> {

    private static final String ELEMENT_MARKER = "[*]";

    private final String path;
    private final BiConsumer<Object, StringValuePattern> sink;
    private final List<String> clauses = new ArrayList<>();

    protected AbstractRequestBodyMatcher(P parent, AbstractStub<?> root, String path,
                                  BiConsumer<Object, StringValuePattern> sink) {
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
     * Requires the value at a path below this one to equal the given value.
     *
     * <p>Measured against a live WireMock server before writing this: two independent
     * {@code matchingJsonPath} conditions on an array, each an "any element" filter of
     * its own, can each be satisfied by a different element — so
     * {@code compositeList().innerField("A").deepFieldDeepestField("B")} used to accept a
     * request whose first element carried A and whose second carried B. That is not what
     * chaining two conditions on the one matcher instance returned by one
     * {@code compositeList()} call should mean.
     */
    protected final void match(String relativePath, Object value) {
        String equality = "@" + relativePath + " == " + literal(value);
        if (isElementScope()) {
            condition(equality);
        } else {
            sink.accept(new Object(), matchingJsonPath(path + relativePath, equalTo(ParameterValues.format(value))));
        }
    }

    /**
     * Requires a list at a path below this one to contain the given value. Written as
     * {@code value in @.path} rather than a nested {@code [?(...)]} filter: measured
     * against a live WireMock server, a filter predicate nested inside the filter this
     * class builds for {@link #match(String, Object)} does not reliably work — a request
     * lacking the value matched all the same — while {@code in} does.
     */
    protected final void matchContains(String relativePath, Object value) {
        if (isElementScope()) {
            condition(literal(value) + " in @" + relativePath);
        } else {
            sink.accept(new Object(), matchingJsonPath(path + relativePath + "[?(@ == " + literal(value) + ")]"));
        }
    }

    /**
     * Adds one clause to this instance's own combined filter and registers the filter
     * built from every clause added so far under this instance as the owner, replacing
     * whatever was registered the previous time. What is registered is therefore always
     * the complete combination to date, however many more calls follow before the stub is
     * actually built.
     */
    private void condition(String clause) {
        clauses.add(clause);
        String root = path.substring(0, path.length() - ELEMENT_MARKER.length());
        sink.accept(this, matchingJsonPath(root + "[?(" + String.join(" && ", clauses) + ")]"));
    }

    private boolean isElementScope() {
        return path.endsWith(ELEMENT_MARKER);
    }

    /**
     * Renders a value as a JSONPath literal, for the filter form where the value is part
     * of the expression rather than a pattern beside it.
     *
     * <p>Text is quoted, and a quote or backslash inside it escaped: otherwise a value as
     * ordinary as {@code O'Reilly} ends the expression early and the request fails to
     * parse rather than failing to match. That part is load-bearing and pinned by a test.
     * Text is rendered through {@link ParameterValues#format}, not {@code toString()},
     * for the same reason a parameter is: an {@code OffsetDateTime} disagrees with its own
     * {@code toString()} whenever the time has zero seconds.
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
        String text = ParameterValues.format(value).replace("\\", "\\\\").replace("'", "\\'");
        return "'" + text + "'";
    }
}
