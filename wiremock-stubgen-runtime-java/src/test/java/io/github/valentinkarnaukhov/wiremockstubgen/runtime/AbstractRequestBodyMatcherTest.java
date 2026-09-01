package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code literal} is exercised end-to-end in the example module against a live WireMock
 * server, which proves the escaped text actually parses; this checks the text itself,
 * one case at a time, which a single example value cannot.
 */
class AbstractRequestBodyMatcherTest {

    @Test
    void quotesAPlainStringAndLeavesItOtherwiseAlone() {
        assertThat(AbstractRequestBodyMatcher.literal("hello")).isEqualTo("'hello'");
    }

    @Test
    void escapesAQuoteSoItDoesNotEndTheExpressionEarly() {
        // Unescaped this would read as [?(@ == 'O') ...Reilly'], a syntax error rather
        // than a mismatch.
        assertThat(AbstractRequestBodyMatcher.literal("O'Reilly")).isEqualTo("'O\\'Reilly'");
    }

    @Test
    void escapesABackslashSoItDoesNotEscapeTheClosingQuote() {
        // Unescaped, a trailing backslash would consume the closing quote and leave the
        // expression unterminated: 'a\' == a followed by an escaped quote, not a value.
        assertThat(AbstractRequestBodyMatcher.literal("a\\b")).isEqualTo("'a\\\\b'");
    }

    @Test
    void escapesABackslashBeforeAQuoteWithoutDoubleEscapingIt() {
        // The backslash must be escaped first: escaping the quote before the backslash
        // would turn one backslash into two and change which character precedes the quote.
        assertThat(AbstractRequestBodyMatcher.literal("a\\'b")).isEqualTo("'a\\\\\\'b'");
    }

    @Test
    void writesNumbersAndBooleansBareRatherThanQuoted() {
        assertThat(AbstractRequestBodyMatcher.literal(3)).isEqualTo("3");
        assertThat(AbstractRequestBodyMatcher.literal(3.5)).isEqualTo("3.5");
        assertThat(AbstractRequestBodyMatcher.literal(true)).isEqualTo("true");
    }

    @Test
    void writesNullAsTheJsonPathLiteralRatherThanTheStringNull() {
        assertThat(AbstractRequestBodyMatcher.literal(null)).isEqualTo("null");
    }
}
