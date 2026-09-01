package io.github.valentinkarnaukhov.wiremockstubgen.spec;

/**
 * How a collection-valued parameter reaches the wire.
 *
 * <p>Mirrors openapi-generator 7.24.0's {@code ApiClient.CollectionFormat}: a query
 * parameter without {@code explode: false} is <em>repeated</em>, not comma-joined, which
 * is the case most easily got wrong.
 */
public enum CollectionFormat {

    /** The parameter carries a single value, so nothing is joined. */
    NONE,

    /** Joined by a comma: the form used everywhere outside the query string. */
    CSV(","),

    /** Joined by a space. */
    SSV(" "),

    /** Joined by a vertical bar. */
    PIPES("|"),

    /** Not joined at all — the parameter is written once per element. */
    MULTI;

    private final String separator;

    CollectionFormat() {
        this(null);
    }

    CollectionFormat(String separator) {
        this.separator = separator;
    }

    /** Whether the parameter carries several values at all. */
    public boolean isCollection() {
        return this != NONE;
    }

    /**
     * The text between two elements.
     *
     * @throws IllegalStateException for {@link #NONE} and {@link #MULTI}, neither of which
     *                               joins anything — a repeated parameter has no separator
     */
    public String separator() {
        if (separator == null) {
            throw new IllegalStateException(this + " does not join its elements");
        }
        return separator;
    }
}
