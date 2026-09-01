package io.github.valentinkarnaukhov.wiremockstubgen.target;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TestLanguageTarget} is registered on this module's own test classpath via
 * {@code META-INF/services}, purely so there is something real for
 * {@link java.util.ServiceLoader} to find here — the actual Java target lives in
 * {@code codegen-java} and is exercised through its own tests.
 */
class LanguageTargetsTest {

    @Test
    void findsWhatIsRegisteredOnTheClassLoader() {
        List<LanguageTarget> found = LanguageTargets.available(getClass().getClassLoader());

        assertThat(found).extracting(LanguageTarget::id).contains("test");
    }

    @Test
    void findLooksUpByIdAndIsEmptyWhenNothingMatches() {
        assertThat(LanguageTargets.find("test", getClass().getClassLoader())).isPresent();
        assertThat(LanguageTargets.find("no-such-target", getClass().getClassLoader())).isEmpty();
    }

    @Test
    void requireReturnsWhatFindWouldHaveFound() {
        LanguageTarget target = LanguageTargets.require("test", getClass().getClassLoader());

        assertThat(target.id()).isEqualTo("test");
    }

    @Test
    void requireFailsWithAMessageListingWhatIsActuallyAvailable() {
        assertThatThrownBy(() -> LanguageTargets.require("no-such-target", getClass().getClassLoader()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no-such-target")
                .hasMessageContaining("test");
    }

    @Test
    void displayNameFallsBackToIdWhenNotOverridden() {
        LanguageTarget target = LanguageTargets.require("test", getClass().getClassLoader());

        assertThat(target.displayName()).isEqualTo(target.id());
    }
}
