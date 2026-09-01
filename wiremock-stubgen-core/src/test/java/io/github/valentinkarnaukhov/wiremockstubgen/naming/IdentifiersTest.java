package io.github.valentinkarnaukhov.wiremockstubgen.naming;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wire names arrive with dashes, dots and leading digits that no identifier accepts;
 * these check every one of {@link Identifiers}' repairs on its own, since a codegen test
 * only ever sees them mixed in with everything else a stub does.
 */
class IdentifiersTest {

    @Test
    void pascalJoinsSegmentsSplittingOnAnythingThatIsNotALetterOrDigit() {
        assertThat(Identifiers.pascalJoin("get", "by-id")).isEqualTo("GetById");
        assertThat(Identifiers.pascalJoin(List.of("x-dashed", "id"))).isEqualTo("XDashedId");
        assertThat(Identifiers.pascalJoin("some.field")).isEqualTo("SomeField");
    }

    @Test
    void pascalJoinPrefixesAnUnderscoreWhenTheResultWouldStartWithADigit() {
        assertThat(Identifiers.pascalJoin("3d")).isEqualTo("_3d");
    }

    @Test
    void pascalJoinIsAnUnderscoreWhenNothingButSeparatorsWasGiven() {
        assertThat(Identifiers.pascalJoin("---")).isEqualTo("_");
        assertThat(Identifiers.pascalJoin()).isEqualTo("_");
    }

    @Test
    void camelJoinsTheSameWayButLowerCasesTheFirstLetter() {
        assertThat(Identifiers.camelJoin("x-dashed", "id")).isEqualTo("xDashedId");
        assertThat(Identifiers.camelJoin(List.of("get", "by-id"))).isEqualTo("getById");
    }

    @Test
    void camelJoinLeavesAnUnderscorePrefixAlone() {
        // "_3d" lower-cased at index 0 would still be "_3d" -- an underscore has no case
        // -- but the special-cased "starts with _" path exists for the all-separators
        // case too, where lower-casing the same underscore alone must not be attempted
        // twice or done differently.
        assertThat(Identifiers.camelJoin("3d")).isEqualTo("_3d");
        assertThat(Identifiers.camelJoin("---")).isEqualTo("_");
    }

    @Test
    void flatLowerCaseStripsSeparatorsAndLowerCasesWhatIsLeft() {
        assertThat(Identifiers.flatLowerCase("get-response-errors")).isEqualTo("getresponseerrors");
        assertThat(Identifiers.flatLowerCase("Some.Field")).isEqualTo("somefield");
    }

    @Test
    void flatLowerCasePrefixesAnUnderscoreWhenTheResultWouldStartWithADigit() {
        assertThat(Identifiers.flatLowerCase("3d-model")).isEqualTo("_3dmodel");
    }

    @Test
    void flatLowerCaseIsAnUnderscoreWhenNothingButSeparatorsWasGiven() {
        assertThat(Identifiers.flatLowerCase("---")).isEqualTo("_");
        assertThat(Identifiers.flatLowerCase("")).isEqualTo("_");
    }
}
