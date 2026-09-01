package io.github.valentinkarnaukhov.wiremockstubgen.openapi;

import io.github.valentinkarnaukhov.wiremockstubgen.fixtures.Fixtures;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reads the fixture and checks what the reader made of it.
 *
 * <p>The fixture is built out of awkward cases — reserved names, several status codes with
 * unrelated schemas, three kinds of recursion — so these assertions are less about the
 * parser working and more about those cases surviving the trip into our own description.
 */
class OpenApiReaderTest {

    private static final Path SPEC = Fixtures.sampleApi();

    private static StubApi api;
    private static List<String> warnings;

    @BeforeAll
    static void readFixture() {
        warnings = new ArrayList<>();
        api = new OpenApiReader(warnings::add).read(SPEC);
    }

    @Test
    void readsEveryOperationWithoutComplaining() {
        assertThat(api.title()).isEqualTo("wiremock-stub-generator test swagger");
        assertThat(api.operations()).hasSize(17);
        assertThat(warnings).isEmpty();
    }

    @Test
    void keepsParameterNamesAndLocationsExactlyAsWritten() {
        Operation inQuery = operation("getByInQueryParameters");
        assertThat(inQuery.method()).isEqualTo(HttpMethod.GET);
        assertThat(inQuery.parametersIn(ParameterLocation.QUERY))
                .extracting(Parameter::name)
                .containsExactly("stringParam", "integerParam", "longParam", "booleanParam",
                        "floatParam", "doubleParam", "enumParam", "repeatedParam", "joinedParam");

        // Renaming the wire name here would silently stop the generated stub from matching
        // the request the client actually sends.
        Operation inHeader = operation("getByInHeaderParameters");
        assertThat(inHeader.parametersIn(ParameterLocation.HEADER))
                .extracting(Parameter::name)
                .contains("X-Dashed-Header");
    }

    @Test
    void readsAnInlineParameterEnumAsItsBaseType() {
        // No Java client library generates a type for an enum declared inline in a
        // parameter, so a named type here would exist only in the stub.
        TypeRef enumParam = operation("getByInQueryParameters").parameters().stream()
                .filter(p -> p.name().equals("enumParam"))
                .findFirst().orElseThrow().type();

        assertThat(enumParam.kind()).isEqualTo(TypeRef.Kind.PRIMITIVE);
        assertThat(enumParam.openApiType()).isEqualTo("string");
    }

    @Test
    void distinguishesFormatsThatShareAnOpenApiType() {
        // integer/int64 and bare integer are the same OpenAPI type and different Java
        // types, so dropping the format would make that undecidable later.
        assertThat(parameterType("getByInQueryParameters", "integerParam").format()).isNull();
        assertThat(parameterType("getByInQueryParameters", "longParam").format()).isEqualTo("int64");
        assertThat(parameterType("getByInQueryParameters", "floatParam").format()).isEqualTo("float");
        assertThat(parameterType("getByInQueryParameters", "doubleParam").format()).isEqualTo("double");
    }

    @Test
    void readsSeveralStatusCodesWithUnrelatedSchemas() {
        Operation errors = operation("getResponseErrors");

        assertThat(errors.responses())
                .extracting(r -> r.statusCode(), r -> r.body().schemaName())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(200, "CompositeBody"),
                        org.assertj.core.groups.Tuple.tuple(404, "ErrorBody"),
                        org.assertj.core.groups.Tuple.tuple(500, "ErrorBody"));
    }

    @Test
    void marksAnOperationWithoutAResponseBodyAsHavingNone() {
        assertThat(operation("getByInQueryParameters").responses())
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.body().kind()).isEqualTo(TypeRef.Kind.UNKNOWN);
                });
    }

    @Test
    void readsAnArrayBodyAsAnArrayOfTheReferencedSchema() {
        TypeRef body = operation("getResponseCompositeList").responses().get(0).body();

        assertThat(body.kind()).isEqualTo(TypeRef.Kind.ARRAY);
        assertThat(body.items().kind()).isEqualTo(TypeRef.Kind.OBJECT);
        assertThat(body.items().schemaName()).isEqualTo("CompositeBody");
        assertThat(api.schemaOf(body)).map(ObjectSchema::name).contains("CompositeBody");
    }

    @Test
    void readsRequestBodies() {
        assertThat(operation("postByRequestBodyComposite").requestBody())
                .isEqualTo(TypeRef.object("CompositeBody"));
        assertThat(operation("getResponseComposite").requestBody()).isNull();
    }

    @Test
    void resolvesReferencesOneStepAtATime() {
        // swagger-parser does not inline $ref even with setResolve(true), so a property
        // pointing at another schema is a name here, not a nested structure.
        ObjectSchema composite = api.schemas().get("CompositeBody");

        assertThat(composite.properties())
                .extracting(Property::name, p -> p.type().kind())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("primitive", TypeRef.Kind.PRIMITIVE),
                        org.assertj.core.groups.Tuple.tuple("composite", TypeRef.Kind.OBJECT),
                        org.assertj.core.groups.Tuple.tuple("primitiveList", TypeRef.Kind.ARRAY),
                        org.assertj.core.groups.Tuple.tuple("compositeList", TypeRef.Kind.ARRAY));

        assertThat(composite.property("composite").orElseThrow().type().schemaName())
                .isEqualTo("CompositeField");
        assertThat(composite.property("compositeList").orElseThrow().type().items().schemaName())
                .isEqualTo("CompositeField");
        assertThat(composite.property("primitiveList").orElseThrow().type().items().openApiType())
                .isEqualTo("string");
    }

    @Test
    void readsASchemaThatOmitsTheObjectTypeKeyword() {
        // CompositeField declares properties but no "type: object". Deciding objecthood
        // from the type keyword alone would drop it.
        assertThat(api.schemas()).containsKey("CompositeField");
        assertThat(api.schemas().get("CompositeField").properties())
                .extracting(Property::name)
                .containsExactly("innerField", "deepField");
    }

    @Test
    void reachabilityTerminatesAndDoesNotDoubleCount() {
        SchemaGraph graph = SchemaGraph.of(api.schemas());

        // RecursiveBody reaches itself directly, through a list, and through
        // RecursiveField. Nothing here detects that; the visited set only makes the walk
        // finite.
        assertThat(graph.reachableFrom("RecursiveBody"))
                .containsExactlyInAnyOrder("RecursiveBody", "RecursiveField");
        assertThat(graph.reachableFrom("CompositeBody"))
                .containsExactlyInAnyOrder("CompositeBody", "CompositeField", "CompositeDeepField");
    }

    @Test
    void keepsOnlyTheSchemasTheOperationsCanReach() {
        // Every schema in the fixture is reachable, so this checks the mechanism:
        // reachability is computed from the operations, not from the components block.
        assertThat(api.reachableSchemas()).containsOnlyKeys(
                "CompositeBody", "CompositeField", "CompositeDeepField",
                "ErrorBody", "RecursiveBody", "RecursiveField",
                "ReservedNamesBody", "ReservedNamesItem");
    }

    @Test
    void failsLoudlyOnASpecificationItCannotRead() {
        assertThatThrownBy(() -> new OpenApiReader().read("does-not-exist.yaml"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does-not-exist.yaml");
    }

    @Test
    void dropsAResponseWhoseStatusCodeCannotBeParsedRatherThanTreatingItAsDefault(@TempDir Path dir) throws Exception {
        Path spec = writeSpec(dir, """
                openapi: 3.0.3
                info: { title: t, version: "1" }
                paths:
                  /x:
                    get:
                      operationId: getX
                      responses:
                        "2XX":
                          description: a range this reader does not understand
                """);

        List<String> warnings = new ArrayList<>();
        StubApi result = new OpenApiReader(warnings::add).read(spec);

        assertThat(result.operations()).singleElement()
                .extracting(Operation::responses).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .isEmpty();
        assertThat(warnings).anySatisfy(w -> assertThat(w).contains("2XX").contains("getX"));
    }

    @Test
    void refusesTwoResponsesThatWouldBothResolveToTheDefault(@TempDir Path dir) throws Exception {
        // Two distinct map keys -- YAML/JSON keys are case-sensitive -- that both fold to
        // "the default response" once read, which is exactly the case a stub cannot have
        // two of: codeDefault() can only ever be one method.
        Path spec = writeSpec(dir, """
                openapi: 3.0.3
                info: { title: t, version: "1" }
                paths:
                  /x:
                    get:
                      operationId: getX
                      responses:
                        default:
                          description: the default
                        Default:
                          description: a second one, differently cased
                """);

        assertThatThrownBy(() -> new OpenApiReader().read(spec))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("getX");
    }

    private static Path writeSpec(Path dir, String yaml) throws java.io.IOException {
        Path spec = dir.resolve("spec.yaml");
        java.nio.file.Files.writeString(spec, yaml);
        return spec;
    }

    private static Operation operation(String operationId) {
        return api.operations().stream()
                .filter(o -> o.operationId().equals(operationId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no operation " + operationId));
    }

    private static TypeRef parameterType(String operationId, String parameterName) {
        return operation(operationId).parameters().stream()
                .filter(p -> p.name().equals(parameterName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no parameter " + parameterName))
                .type();
    }
}
