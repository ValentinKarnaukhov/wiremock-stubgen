package io.github.valentinkarnaukhov.stubgen.lang.java;

import io.github.valentinkarnaukhov.stubgen.spi.LanguageTarget;
import io.github.valentinkarnaukhov.stubgen.spi.LanguageTargets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards the extension point: a language target must be usable without anyone
 * referencing its class, otherwise adding a language would mean editing the core.
 */
class JavaLanguageTargetDiscoveryTest {

    private static ClassLoader classLoader() {
        return JavaLanguageTargetDiscoveryTest.class.getClassLoader();
    }

    @Test
    void isDiscoveredViaServiceLoader() {
        assertThat(LanguageTargets.available(classLoader()))
                .extracting(LanguageTarget::id)
                .contains(JavaLanguageTarget.ID);
    }

    @Test
    void isResolvableById() {
        LanguageTarget target = LanguageTargets.require(JavaLanguageTarget.ID, classLoader());

        assertThat(target).isInstanceOf(JavaLanguageTarget.class);
        assertThat(target.displayName()).isEqualTo("Java (WireMock)");
    }

    @Test
    void unknownIdReportsWhatIsAvailable() {
        assertThatThrownBy(() -> LanguageTargets.require("cobol", classLoader()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cobol")
                .hasMessageContaining(JavaLanguageTarget.ID);
    }
}
