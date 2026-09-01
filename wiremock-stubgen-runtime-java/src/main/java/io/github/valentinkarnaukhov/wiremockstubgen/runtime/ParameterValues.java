package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Turns the values handed to a stub's parameter method into the text a request carries.
 *
 * <p>Generated code calls these instead of inlining the same loops in every stub. The
 * shapes come from openapi-generator's {@code ApiClient}: a joined list is each element
 * stringified and glued by one separator, and a repeated parameter is one pattern per
 * element, matched as a set rather than a sequence.
 */
public final class ParameterValues {

    private ParameterValues() {
    }

    /**
     * The elements written one after another with {@code separator} between them.
     *
     * <p>{@code String.valueOf} on the list itself would give Java's {@code [a, b]}, which
     * no client ever sends — the brackets and the space are the reason this method exists.
     */
    public static String join(String separator, Object... values) {
        Objects.requireNonNull(separator, "separator");
        Objects.requireNonNull(values, "values");
        StringBuilder joined = new StringBuilder();
        for (Object value : values) {
            if (joined.length() > 0) {
                joined.append(separator);
            }
            joined.append(format(value));
        }
        return joined.toString();
    }

    /** One {@code equalTo} per element, for a parameter that is written once per value. */
    public static StringValuePattern[] eachEqualTo(Object... values) {
        Objects.requireNonNull(values, "values");
        StringValuePattern[] patterns = new StringValuePattern[values.length];
        for (int index = 0; index < values.length; index++) {
            patterns[index] = WireMock.equalTo(format(values[index]));
        }
        return patterns;
    }

    /**
     * Renders a single value the way openapi-generator's own {@code ApiClient} does, so
     * that a stub matches whatever text that client actually puts on the wire.
     *
     * <p>Measured on 7.24.0: {@code parameterToString} special-cases only {@code Date} and
     * {@code OffsetDateTime} before falling back to {@code String.valueOf}. We never emit
     * {@code Date} — {@code LocalDate} and every other mapped type already agree with
     * {@code toString()} — so {@code OffsetDateTime} is the one case worth a formatter:
     * its {@code toString()} omits seconds when they are zero (10:15 vs 10:15:00), while
     * the client always writes them via {@code DateTimeFormatter.ISO_OFFSET_DATE_TIME}.
     */
    public static String format(Object value) {
        if (value instanceof OffsetDateTime offsetDateTime) {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime);
        }
        return String.valueOf(value);
    }
}
