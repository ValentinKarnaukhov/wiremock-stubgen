package io.github.valentinkarnaukhov.wiremockstubgen.spec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CollectionFormatTest {

    @Test
    void csvSsvAndPipesJoinWithTheirOwnSeparator() {
        assertThat(CollectionFormat.CSV.separator()).isEqualTo(",");
        assertThat(CollectionFormat.SSV.separator()).isEqualTo(" ");
        assertThat(CollectionFormat.PIPES.separator()).isEqualTo("|");
    }

    @Test
    void noneAndMultiHaveNoSeparatorBecauseNeitherJoinsAnything() {
        assertThatThrownBy(CollectionFormat.NONE::separator)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NONE");
        assertThatThrownBy(CollectionFormat.MULTI::separator)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MULTI");
    }

    @Test
    void onlyNoneCarriesASingleValue() {
        assertThat(CollectionFormat.NONE.isCollection()).isFalse();
        assertThat(CollectionFormat.CSV.isCollection()).isTrue();
        assertThat(CollectionFormat.SSV.isCollection()).isTrue();
        assertThat(CollectionFormat.PIPES.isCollection()).isTrue();
        assertThat(CollectionFormat.MULTI.isCollection()).isTrue();
    }
}
