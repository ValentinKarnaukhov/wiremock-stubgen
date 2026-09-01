package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

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
            joined.append(value);
        }
        return joined.toString();
    }

    /** One {@code equalTo} per element, for a parameter that is written once per value. */
    public static StringValuePattern[] eachEqualTo(Object... values) {
        Objects.requireNonNull(values, "values");
        StringValuePattern[] patterns = new StringValuePattern[values.length];
        for (int index = 0; index < values.length; index++) {
            patterns[index] = WireMock.equalTo(String.valueOf(values[index]));
        }
        return patterns;
    }
}
