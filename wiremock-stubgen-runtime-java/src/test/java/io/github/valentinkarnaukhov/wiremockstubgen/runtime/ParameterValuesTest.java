package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks these against what openapi-generator's own {@code ApiClient} does, not against
 * what looks reasonable — a stub that disagrees with the client never matches anything.
 */
class ParameterValuesTest {

    @Test
    void formatsAnOffsetDateTimeTheWayApiClientDoes() {
        // Measured on 7.24.0: ApiClient.formatOffsetDateTime always writes seconds, unlike
        // OffsetDateTime.toString(), which drops them when they are zero.
        OffsetDateTime noSeconds = OffsetDateTime.of(2024, 1, 2, 10, 15, 0, 0, ZoneOffset.ofHours(1));
        assertThat(noSeconds.toString())
                .describedAs("the gap this method exists to close")
                .isNotEqualTo("2024-01-02T10:15:00+01:00");
        assertThat(ParameterValues.format(noSeconds)).isEqualTo("2024-01-02T10:15:00+01:00");

        OffsetDateTime withFraction = OffsetDateTime.of(2024, 1, 2, 10, 15, 30, 123_000_000, ZoneOffset.UTC);
        assertThat(ParameterValues.format(withFraction)).isEqualTo("2024-01-02T10:15:30.123Z");
    }

    @Test
    void leavesEverythingElseToStringValueOf() {
        assertThat(ParameterValues.format("plain")).isEqualTo("plain");
        assertThat(ParameterValues.format(42L)).isEqualTo("42");
        assertThat(ParameterValues.format(null)).isEqualTo("null");
    }

    @Test
    void joinsDatesTheSameWayItFormatsOneOnItsOwn() {
        OffsetDateTime noSeconds = OffsetDateTime.of(2024, 1, 2, 10, 15, 0, 0, ZoneOffset.ofHours(1));
        OffsetDateTime other = OffsetDateTime.of(2024, 1, 3, 9, 0, 0, 0, ZoneOffset.ofHours(1));

        assertThat(ParameterValues.join(",", noSeconds, other))
                .isEqualTo("2024-01-02T10:15:00+01:00,2024-01-03T09:00:00+01:00");
    }

    @Test
    void matchesEachDateAgainstItsOwnFormattedElement() {
        OffsetDateTime noSeconds = OffsetDateTime.of(2024, 1, 2, 10, 15, 0, 0, ZoneOffset.ofHours(1));

        StringValuePattern[] patterns = ParameterValues.eachEqualTo(noSeconds);

        assertThat(patterns).hasSize(1);
        assertThat(patterns[0].getExpected()).isEqualTo("2024-01-02T10:15:00+01:00");
    }
}
