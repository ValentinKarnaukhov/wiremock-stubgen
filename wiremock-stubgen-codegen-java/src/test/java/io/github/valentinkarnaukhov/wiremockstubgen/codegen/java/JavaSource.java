package io.github.valentinkarnaukhov.wiremockstubgen.codegen.java;

import java.util.List;

/**
 * Reduces Java source to the declarations it makes, so two files can be compared for what
 * they say rather than how they are commented.
 *
 * <p>The golden stubs carry hand-written design notes that the generator is not meant to
 * produce, so a verbatim comparison would fail for the one reason that does not matter.
 *
 * <p>Whitespace is <em>not</em> normalised away: indentation is part of what this
 * generator delivers. Blank lines are dropped, because where a stripped comment leaves a
 * gap is not worth asserting.
 */
final class JavaSource {

    private JavaSource() {
    }

    /** Comments removed, blank lines removed, trailing whitespace removed. */
    static List<String> declarations(String source) {
        return stripComments(source).lines()
                .map(String::stripTrailing)
                .filter(line -> !line.isBlank())
                .toList();
    }

    /**
     * Removes comments without being fooled by the contents of string literals. A regular
     * expression is wrong here: generated stubs are full of JSONPath expressions and URL
     * templates, and one containing {@code //} would silently delete the rest of the line.
     */
    private static String stripComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        State state = State.CODE;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            switch (state) {
                case CODE -> {
                    if (c == '/' && next == '/') {
                        state = State.LINE_COMMENT;
                    } else if (c == '/' && next == '*') {
                        state = State.BLOCK_COMMENT;
                        i++;
                    } else {
                        if (c == '"') {
                            state = State.STRING;
                        } else if (c == '\'') {
                            state = State.CHAR;
                        }
                        out.append(c);
                    }
                }
                case STRING, CHAR -> {
                    out.append(c);
                    if (c == '\\') {
                        out.append(next);
                        i++;
                    } else if (c == (state == State.STRING ? '"' : '\'')) {
                        state = State.CODE;
                    }
                }
                case LINE_COMMENT -> {
                    if (c == '\n') {
                        out.append(c);
                        state = State.CODE;
                    }
                }
                case BLOCK_COMMENT -> {
                    if (c == '*' && next == '/') {
                        state = State.CODE;
                        i++;
                    } else if (c == '\n') {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    private enum State {
        CODE, STRING, CHAR, LINE_COMMENT, BLOCK_COMMENT
    }
}
