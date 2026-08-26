package io.github.valentinkarnaukhov.wiremockstubgen.codegen.java;

import io.github.valentinkarnaukhov.wiremockstubgen.spec.TypeRef;
import io.github.valentinkarnaukhov.wiremockstubgen.target.TargetOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A stub sets values on a model openapi-generator wrote, so every scalar has to be spelled
 * the way that generator spells it. Guessing here compiles nowhere.
 *
 * <p>Every row below was measured on generator 7.24.0 by declaring the property and
 * reading the field it wrote, not taken from its documentation.
 */
class JavaTypesTest {

    private final JavaTypes types = new JavaTypes(TargetOptions.builder("demo.stubs")
            .modelPackage("demo.model").build());

    @ParameterizedTest
    @CsvSource({
            "string,          , java.lang.String",
            "string, date,      java.time.LocalDate",
            "string, date-time, java.time.OffsetDateTime",
            "string, uuid,      java.util.UUID",
            "string, uri,       java.net.URI",
            "string, binary,    java.io.File",
            "string, byte,      byte[]",
            // Formats the generator recognises as formats and still writes as String.
            "string, url,       java.lang.String",
            "string, email,     java.lang.String",
            "string, hostname,  java.lang.String",
            "string, ipv4,      java.lang.String",
            "string, password,  java.lang.String",
            "string, not-a-format, java.lang.String",
            "integer,          , java.lang.Integer",
            "integer, int32,    java.lang.Integer",
            "integer, int64,    java.lang.Long",
            "number,           , java.math.BigDecimal",
            "number, float,     java.lang.Float",
            "number, double,    java.lang.Double",
            "boolean,          , java.lang.Boolean",
    })
    void spellsAScalarTheWayTheModelGeneratorSpellsIt(String openApiType, String format, String expected) {
        assertThat(types.nameOf(TypeRef.primitive(openApiType, format))).contains(expected);
    }

    @Test
    void carriesTheFormatThroughAList() {
        assertThat(types.nameOf(TypeRef.array(TypeRef.primitive("string", "uri"))))
                .contains("java.util.List<java.net.URI>");
    }
}
