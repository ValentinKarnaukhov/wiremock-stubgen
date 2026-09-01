package io.github.valentinkarnaukhov.wiremockstubgen.flatten;

import io.github.valentinkarnaukhov.wiremockstubgen.naming.Identifiers;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlatteningOptionsTest {

    @Test
    void defaultsCamelJoinNamesAtTheDocumentedDepth() {
        FlatteningOptions options = FlatteningOptions.defaults();

        assertThat(options.maxDepth()).isEqualTo(FlatteningOptions.DEFAULT_MAX_DEPTH);
        assertThat(options.accessorName().apply(List.of("composite", "inner-field"))).isEqualTo("compositeInnerField");
    }

    @Test
    void withMaxDepthReplacesOnlyTheDepth() {
        FlatteningOptions options = FlatteningOptions.defaults().withMaxDepth(2);

        assertThat(options.maxDepth()).isEqualTo(2);
        assertThat(options.accessorName()).isEqualTo(FlatteningOptions.defaults().accessorName());
    }

    @Test
    void withAccessorNameReplacesOnlyTheNaming() {
        FlatteningOptions options = FlatteningOptions.defaults().withAccessorName(segments -> "fixed");

        assertThat(options.accessorName().apply(List.of("anything"))).isEqualTo("fixed");
        assertThat(options.maxDepth()).isEqualTo(FlatteningOptions.DEFAULT_MAX_DEPTH);
    }

    @Test
    void rejectsAMaxDepthBelowOne() {
        assertThatThrownBy(() -> new FlatteningOptions(0, Identifiers::camelJoin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDepth");
    }

    @Test
    void rejectsANullAccessorName() {
        assertThatThrownBy(() -> new FlatteningOptions(1, null))
                .isInstanceOf(NullPointerException.class);
    }
}
