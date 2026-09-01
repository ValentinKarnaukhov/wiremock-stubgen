package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Turns the values handed to a stub's parameter method into the text a request carries.
 * Generated code calls these instead of inlining the same loops in every stub.
 */
public final class ParameterValues {

    private ParameterValues() {
    }

    /**
     * The elements written one after another with {@code separator} between them.
     * {@code String.valueOf} on the list itself would give Java's {@code [a, b]}, which
     * no client sends.
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
     * Renders a value the way openapi-generator's client does. Only {@code OffsetDateTime}
     * needs special-casing: its {@code toString()} drops seconds when they're zero
     * (10:15 vs 10:15:00), unlike the client's {@code ISO_OFFSET_DATE_TIME} formatter.
     */
    public static String format(Object value) {
        if (value instanceof OffsetDateTime offsetDateTime) {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime);
        }
        return String.valueOf(value);
    }
}
