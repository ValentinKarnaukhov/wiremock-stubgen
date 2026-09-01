package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The one place in the generated output the compiler does not check, so what matters here
 * is exactly the three ways it can go: the field exists on the class itself, it exists
 * further up the hierarchy, or it does not exist anywhere and the specification and the
 * model disagree.
 */
class ReadOnlyPropertiesTest {

    @Test
    void writesAPrivateFieldDeclaredOnTheClassItself() {
        Leaf target = new Leaf();

        ReadOnlyProperties.set(target, "own", "value");

        assertThat(target.own).isEqualTo("value");
    }

    @Test
    void writesAFieldDeclaredOnASuperclass() {
        Leaf target = new Leaf();

        ReadOnlyProperties.set(target, "inherited", "value");

        assertThat(target.inherited).isEqualTo("value");
    }

    @Test
    void complainsByNameWhenNoClassInTheHierarchyHasTheField() {
        Leaf target = new Leaf();

        assertThatThrownBy(() -> ReadOnlyProperties.set(target, "missing", "value"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Leaf")
                .hasMessageContaining("missing");
    }

    @Test
    void wrapsAWriteFailureRatherThanLettingTheReflectionExceptionThrough() {
        Leaf target = new Leaf();

        // The field exists, but the value cannot be assigned to it -- an int field given
        // a String -- so writing fails for a different reason than not finding the field,
        // and that failure is wrapped the same way.
        assertThatThrownBy(() -> ReadOnlyProperties.set(target, "count", "not-a-number"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("count")
                .hasMessageContaining("Leaf");
    }

    private static class Root {
        String inherited;
    }

    private static final class Leaf extends Root {
        private String own;
        private int count;
    }
}
