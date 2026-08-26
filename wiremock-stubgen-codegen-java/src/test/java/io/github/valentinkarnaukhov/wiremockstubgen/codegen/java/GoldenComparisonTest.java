package io.github.valentinkarnaukhov.wiremockstubgen.codegen.java;

import io.github.valentinkarnaukhov.wiremockstubgen.fixtures.Fixtures;
import io.github.valentinkarnaukhov.wiremockstubgen.openapi.OpenApiReader;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.StubApi;
import io.github.valentinkarnaukhov.wiremockstubgen.target.GeneratedFile;
import io.github.valentinkarnaukhov.wiremockstubgen.target.Grouping;
import io.github.valentinkarnaukhov.wiremockstubgen.target.TargetOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Holds the generator to the stubs that were written by hand before it existed.
 *
 * <p>What this checks is not that the generator works but that the description it works
 * from is complete: every time a golden says something the model cannot express, that
 * shows up here as a missing line. All five goldens are compared in full; three of them
 * pin down the builders and matchers.
 */
class GoldenComparisonTest {

    private static StubApi api;

    @BeforeAll
    static void readFixture() {
        api = new OpenApiReader(warning -> {
        }).read(Fixtures.sampleApi());
    }

    @Test
    void writesTheQueryParameterStubExactlyAsItWasWrittenByHand() {
        assertThat(generated("getbyparametersinquery/GetByInQueryParametersStub.java"))
                .isEqualTo(golden("getbyparametersinquery/GetByInQueryParametersStub.java"));
    }

    @Test
    void writesThePathParameterStubExactlyAsItWasWrittenByHand() {
        assertThat(generated("getbyparametersinpath/GetByInPathParametersStub.java"))
                .isEqualTo(golden("getbyparametersinpath/GetByInPathParametersStub.java"));
    }

    @Test
    void writesTheResponseBuilderStubExactlyAsItWasWrittenByHand() {
        assertThat(generated("getresponsecompositelist/GetResponseCompositeListStub.java"))
                .isEqualTo(golden("getresponsecompositelist/GetResponseCompositeListStub.java"));
    }

    @Test
    void writesTheMultipleStatusCodeStubExactlyAsItWasWrittenByHand() {
        assertThat(generated("getresponseerrors/GetResponseErrorsStub.java"))
                .isEqualTo(golden("getresponseerrors/GetResponseErrorsStub.java"));
    }

    @Test
    void writesTheRequestMatcherStubExactlyAsItWasWrittenByHand() {
        assertThat(generated("postbyrequestbodycomposite/PostByRequestBodyCompositeStub.java"))
                .isEqualTo(golden("postbyrequestbodycomposite/PostByRequestBodyCompositeStub.java"));
    }

    @Test
    void putsEveryOperationInAFileNamedAfterItsTag() {
        List<GeneratedFile> files = generate();

        assertThat(files).hasSize(api.operations().size());
        assertThat(files).extracting(GeneratedFile::relativePath)
                .contains("com/example/stubs/getresponseerrors/GetResponseErrorsStub.java")
                .contains("com/example/stubs/multipleoperations/GetMultipleOperationsFirstStub.java")
                .contains("com/example/stubs/multipleoperations/GetMultipleOperationsSecondStub.java");
    }

    @Test
    void putsEverythingInOnePackageWhenGroupingIsOff() {
        List<GeneratedFile> files = new JavaLanguageTarget()
                .generate(api, options().grouping(Grouping.NONE).build());

        assertThat(files).extracting(GeneratedFile::relativePath)
                .allSatisfy(path -> assertThat(path).startsWith("com/example/stubs/"))
                .contains("com/example/stubs/GetResponseErrorsStub.java");
    }

    @Test
    void offersOnlyTheWholeBodyFormWhenExplodeIsOff() {
        List<GeneratedFile> files = new JavaLanguageTarget()
                .generate(api, options().explode(false).build());

        String source = contentOf(files, "com/example/stubs/getresponseerrors/GetResponseErrorsStub.java");
        assertThat(source).contains("public GetResponseErrorsStub code200(CompositeBody body)");
        assertThat(source)
                .describedAs("no builders, and so no no-argument form to hand one out")
                .doesNotContain("AbstractResponseBodyBuilder")
                .doesNotContain("code200()");
    }

    @Test
    void fallsBackToObjectWhenNoModelPackageIsConfigured() {
        List<GeneratedFile> files = new JavaLanguageTarget()
                .generate(api, TargetOptions.builder("com.example.stubs").build());

        String source = contentOf(files, "com/example/stubs/getresponseerrors/GetResponseErrorsStub.java");
        assertThat(source)
                .describedAs("the stub still works; the compiler just stops checking the call")
                .contains("public GetResponseErrorsStub code200(Object body)");
        assertThat(source)
                .describedAs("a builder has to name the schema of every object it creates, and cannot")
                .doesNotContain("AbstractResponseBodyBuilder");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static List<String> generated(String relativePath) {
        return JavaSource.declarations(contentOf(generate(), "com/example/stubs/" + relativePath));
    }

    private static List<String> golden(String relativePath) {
        return JavaSource.declarations(read(Path.of("src/test/resources/golden", relativePath)));
    }

    private static List<GeneratedFile> generate() {
        return new JavaLanguageTarget().generate(api, options().build());
    }

    private static TargetOptions.Builder options() {
        return TargetOptions.builder("com.example.stubs").modelPackage("com.example.model");
    }

    private static String contentOf(List<GeneratedFile> files, String relativePath) {
        return files.stream()
                .filter(file -> file.relativePath().equals(relativePath))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no generated file at " + relativePath
                        + ", generated: " + files.stream().map(GeneratedFile::relativePath).toList()))
                .content();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
