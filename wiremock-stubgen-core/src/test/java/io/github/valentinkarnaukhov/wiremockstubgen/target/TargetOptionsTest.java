package io.github.valentinkarnaukhov.wiremockstubgen.target;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A builder for readable call sites, a record underneath for the immutability and
 * equality that come free with it — this checks both halves: what the builder decides on
 * the caller's behalf, and what the record itself refuses to hold.
 */
class TargetOptionsTest {

    @Test
    void defaultsToTagGroupingExplodedBodiesAndNoModelPackage() {
        TargetOptions options = TargetOptions.builder("com.example.stubs").build();

        assertThat(options.stubPackage()).isEqualTo("com.example.stubs");
        assertThat(options.grouping()).isEqualTo(Grouping.TAG);
        assertThat(options.explode()).isTrue();
        assertThat(options.modelPackageIfPresent()).isEmpty();
    }

    @Test
    void everyBuilderMethodReachesTheBuiltRecord() {
        TargetOptions options = TargetOptions.builder("com.example.stubs")
                .modelPackage("com.example.model")
                .grouping(Grouping.NONE)
                .explode(false)
                .maxDepth(3)
                .options(Map.of("key", "value"))
                .build();

        assertThat(options.modelPackageIfPresent()).contains("com.example.model");
        assertThat(options.grouping()).isEqualTo(Grouping.NONE);
        assertThat(options.explode()).isFalse();
        assertThat(options.maxDepth()).isEqualTo(3);
        assertThat(options.option("key")).contains("value");
        assertThat(options.option("missing")).isEmpty();
    }

    @Test
    void treatsABlankModelPackageAsNonePresentRatherThanAnEmptyString() {
        TargetOptions options = TargetOptions.builder("com.example.stubs")
                .modelPackage("   ")
                .build();

        assertThat(options.modelPackageIfPresent()).isEmpty();
    }

    @Test
    void rejectsAMaxDepthBelowOne() {
        assertThatThrownBy(() -> TargetOptions.builder("com.example.stubs").maxDepth(0).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDepth");
    }

    @Test
    void rejectsANullStubPackageOrGrouping() {
        assertThatThrownBy(() -> new TargetOptions(null, null, Grouping.TAG, true, 1, Map.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TargetOptions("com.example.stubs", null, null, true, 1, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void copiesTheOptionsMapRatherThanKeepingTheCallersReference() {
        Map<String, String> mutable = new java.util.HashMap<>(Map.of("key", "value"));
        TargetOptions options = TargetOptions.builder("com.example.stubs").options(mutable).build();

        mutable.put("key", "changed");

        assertThat(options.option("key")).contains("value");
    }
}
