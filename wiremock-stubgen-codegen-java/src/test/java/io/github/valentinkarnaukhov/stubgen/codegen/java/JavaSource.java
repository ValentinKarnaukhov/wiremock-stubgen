package io.github.valentinkarnaukhov.stubgen.codegen.java;

import java.util.List;

/**
 * Reduces Java source to the declarations it makes, so two files can be compared for what
 * they say rather than how they are annotated.
 *
 * <p>The golden stubs are hand-written and carry long design notes — why the type
 * parameter survives, what is still open about matching. Those are notes to us, not
 * output the generator should produce, so a verbatim comparison would fail for the one
 * reason that does not matter. Stripping comments from both sides leaves exactly the part
 * a golden is meant to pin down.
 *
 * <p>Whitespace is <em>not</em> normalised away. Indentation and blank lines are part of
 * what this generator delivers: the output is read by people, and a stub nobody wants to
 * open is not much better than no stub. Blank lines go, because where a stripped comment
 * leaves a gap is not something worth asserting.
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
     * Removes comments without being fooled by the contents of string literals.
     *
     * <p>A regular expression is the obvious shortcut and is wrong here: generated stubs
     * are full of JSONPath expressions and URL templates, and one of them containing
     * {@code //} would silently delete the rest of the line.
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
