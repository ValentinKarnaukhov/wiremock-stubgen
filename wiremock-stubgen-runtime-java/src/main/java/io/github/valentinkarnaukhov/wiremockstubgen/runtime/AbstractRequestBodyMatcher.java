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
 * <p>Nothing is constructed here: a matcher only adds a JSONPath expression, so one class
 * per schema serves every position that schema occupies, however deep. There is no
 * "whole node equals" method — a value pattern on a path ending in {@code [*]} applies to
 * the whole array, not to one element, so matching a body as a whole stays on the stub.
 *
 * <p>A path ending in {@code [*]} means this instance is some element of an array. Every
 * condition asked of it folds into one combined filter instead of becoming independent,
 * since independent conditions on an array can each be satisfied by a different element
 * (see {@link #condition(String)}).
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
     * Requires the value at a path below this one to equal the given value. Chained
     * calls on an element-position matcher combine into one filter, so
     * {@code compositeList().innerField("A").deepFieldDeepestField("B")} requires both on
     * the same element rather than either on any element.
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
     * {@code value in @.path}: a {@code [?(...)]} filter nested inside this class's own
     * filter does not reliably match, while {@code in} does.
     */
    protected final void matchContains(String relativePath, Object value) {
        if (isElementScope()) {
            condition(literal(value) + " in @" + relativePath);
        } else {
            sink.accept(new Object(), matchingJsonPath(path + relativePath + "[?(@ == " + literal(value) + ")]"));
        }
    }

    /**
     * Adds one clause to this instance's combined filter, replacing whatever was
     * registered under this instance last time with the complete combination so far.
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
     * Renders a value as a JSONPath literal. Text is quoted with a quote/backslash
     * escaped, so a value like {@code O'Reilly} doesn't end the expression early, and
     * rendered through {@link ParameterValues#format} rather than {@code toString()} for
     * the same {@code OffsetDateTime} reason a parameter is. Numbers and booleans are
     * written bare, matching the filter's own type rules.
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
