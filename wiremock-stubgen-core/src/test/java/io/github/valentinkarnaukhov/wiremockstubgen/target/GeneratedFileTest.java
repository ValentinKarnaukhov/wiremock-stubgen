package io.github.valentinkarnaukhov.wiremockstubgen.target;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneratedFileTest {

    @Test
    void keepsThePathAndContentItWasGivenVerbatim() {
        GeneratedFile file = new GeneratedFile("com/example/stub/UserApiStub.java", "class UserApiStub {}");

        assertThat(file.relativePath()).isEqualTo("com/example/stub/UserApiStub.java");
        assertThat(file.content()).isEqualTo("class UserApiStub {}");
    }

    @Test
    void rejectsABlankPath() {
        assertThatThrownBy(() -> new GeneratedFile("", "content"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeneratedFile("   ", "content"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANullPathOrContent() {
        assertThatThrownBy(() -> new GeneratedFile(null, "content"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new GeneratedFile("path", null))
                .isInstanceOf(NullPointerException.class);
    }
}
