package io.github.valentinkarnaukhov.wiremockstubgen.codegen.java;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nothing stops a specification from calling a schema {@code List}, and the emitter mixes
 * those model classes with {@code java.util} and WireMock's own. Two imports of the same
 * simple name do not compile, so these say which name is written short and which in full.
 */
class ImportsTest {

    private final Imports imports = new Imports("demo.stubs");

    @Test
    void shortensAndImportsATypeFromElsewhere() {
        assertThat(imports.use("demo.model.Body")).isEqualTo("Body");
        assertThat(imports.render()).containsExactly("import demo.model.Body;");
    }

    @Test
    void writesTheSecondClaimantOfASimpleNameInFull() {
        assertThat(imports.use("java.util.List")).isEqualTo("List");
        assertThat(imports.use("demo.model.List")).isEqualTo("demo.model.List");
        assertThat(imports.render()).containsExactly("import java.util.List;");
    }

    @Test
    void keepsShorteningATypeThatAlreadyOwnsItsName() {
        assertThat(imports.use("demo.model.Body")).isEqualTo("Body");
        assertThat(imports.use("demo.model.Body")).isEqualTo("Body");
    }

    @Test
    void yieldsToAModelClassWhenTheModelAskedFirst() {
        // Whoever asks first keeps the short name. Both spellings compile, so the rule only
        // has to be consistent, not clever.
        assertThat(imports.use("demo.model.List")).isEqualTo("List");
        assertThat(imports.use("java.util.List<demo.model.Body>"))
                .isEqualTo("java.util.List<Body>");
    }

    @Test
    void doesNotLetAnImportShadowTheClassTheFileDeclares() {
        imports.reserve("GetListStub");
        assertThat(imports.use("demo.model.GetListStub")).isEqualTo("demo.model.GetListStub");
        assertThat(imports.render()).isEmpty();
    }

    @Test
    void treatsAnUnimportedNeighbourAsAClaimToo() {
        // Nothing in the file's own package is imported, but the simple name is still taken.
        assertThat(imports.use("demo.stubs.Body")).isEqualTo("Body");
        assertThat(imports.use("demo.model.Body")).isEqualTo("demo.model.Body");
        assertThat(imports.render()).isEmpty();
    }

    @Test
    void countsANestedTypeAsAClaimOnItsOuterClassOnly() {
        assertThat(imports.use("demo.model.Holder$MethodEnum")).isEqualTo("Holder.MethodEnum");
        assertThat(imports.use("demo.other.Holder")).isEqualTo("demo.other.Holder");
        assertThat(imports.render()).containsExactly("import demo.model.Holder;");
    }
}
