package io.github.valentinkarnaukhov.wiremockstubgen.naming;

import java.util.List;

/**
 * Turns names out of a specification into names a programming language will accept.
 *
 * <p>Wire names are not identifiers: JSON permits {@code x-dashed}, {@code some.field}
 * and {@code 3d}. Repairing that once, here, keeps the result the same across language
 * targets. What comes out is ASCII letters and digits only; a target may re-case it but
 * will not have to repair it.
 */
public final class Identifiers {

    private Identifiers() {
    }

    /** Camel-joins wire names, lower-case first: {@code [x-dashed, id]} to {@code xDashedId}. */
    public static String camelJoin(List<String> segments) {
        String joined = pascalJoin(segments);
        if (joined.equals("_") || joined.startsWith("_")) {
            return joined;
        }
        return Character.toLowerCase(joined.charAt(0)) + joined.substring(1);
    }

    /** Camel-joins wire names, upper-case first: {@code [get, by-id]} to {@code GetById}. */
    public static String pascalJoin(List<String> segments) {
        StringBuilder result = new StringBuilder();
        for (String segment : segments) {
            for (String word : segment.split("[^A-Za-z0-9]+")) {
                if (!word.isEmpty()) {
                    result.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
                }
            }
        }
        if (result.isEmpty()) {
            return "_";
        }
        return Character.isDigit(result.charAt(0)) ? "_" + result : result.toString();
    }

    public static String camelJoin(String... segments) {
        return camelJoin(List.of(segments));
    }

    public static String pascalJoin(String... segments) {
        return pascalJoin(List.of(segments));
    }

    /**
     * Strips a name down to letters and digits and lower-cases it, for places that take
     * no separators at all — a Java package segment, where {@code get-response-errors}
     * has to become {@code getresponseerrors}.
     */
    public static String flatLowerCase(String name) {
        StringBuilder result = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                result.append(Character.toLowerCase(c));
            }
        }
        if (result.isEmpty()) {
            return "_";
        }
        return Character.isDigit(result.charAt(0)) ? "_" + result : result.toString();
    }
}
