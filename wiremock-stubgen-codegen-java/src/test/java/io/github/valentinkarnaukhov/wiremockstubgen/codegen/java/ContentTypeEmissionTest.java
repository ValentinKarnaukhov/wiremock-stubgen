package io.github.valentinkarnaukhov.wiremockstubgen.codegen.java;

import io.github.valentinkarnaukhov.wiremockstubgen.fixtures.Fixtures;
import io.github.valentinkarnaukhov.wiremockstubgen.openapi.OpenApiReader;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.StubApi;
import io.github.valentinkarnaukhov.wiremockstubgen.target.GeneratedFile;
import io.github.valentinkarnaukhov.wiremockstubgen.target.TargetOptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AbstractStub} — read as {@code io.github.valentinkarnaukhov.wiremockstubgen.runtime.AbstractStub}
 * in the generated code — always answers {@code application/json} unless told otherwise.
 * A response declared with a different media type needs the generated method itself to
 * say so, or nothing does.
 */
class ContentTypeEmissionTest {

    @Test
    void callsContentTypeOnlyWhenTheDeclaredMediaTypeIsNotAlreadyTheDefault() {
        StubApi api = new OpenApiReader(warning -> {
        }).read(Fixtures.compositionApi());
        List<GeneratedFile> files = new JavaLanguageTarget()
                .generate(api, TargetOptions.builder("com.example.stubs").modelPackage("com.example.model").build(),
                        warning -> {
                        });
        String source = files.stream()
                .filter(file -> file.relativePath().endsWith("GetMediaTypesStub.java"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no generated file for getMediaTypes"))
                .content();

        assertThat(source)
                .describedAs("a vendor JSON type still needs the header spelled out")
                .contains("return response(200, body).contentType(\"application/vnd.library.v1+json\");");
        assertThat(source)
                .describedAs("declared as text/plain, so the header must say that and not application/json")
                .contains("return response(202, body).contentType(\"text/plain\");");

        String aliases = files.stream()
                .filter(file -> file.relativePath().endsWith("GetAliasesStub.java"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no generated file for getAliases"))
                .content();
        assertThat(aliases)
                .describedAs("already AbstractStub's own default, so calling contentType would say nothing new")
                .doesNotContain("contentType");
    }
}
