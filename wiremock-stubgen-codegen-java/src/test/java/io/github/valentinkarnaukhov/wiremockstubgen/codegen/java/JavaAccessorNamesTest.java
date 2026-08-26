package io.github.valentinkarnaukhov.wiremockstubgen.codegen.java;

import io.github.valentinkarnaukhov.wiremockstubgen.fixtures.Fixtures;
import io.github.valentinkarnaukhov.wiremockstubgen.flatten.Accessor;
import io.github.valentinkarnaukhov.wiremockstubgen.flatten.BodyScope;
import io.github.valentinkarnaukhov.wiremockstubgen.flatten.BodySide;
import io.github.valentinkarnaukhov.wiremockstubgen.flatten.Flattener;
import io.github.valentinkarnaukhov.wiremockstubgen.flatten.FlatteningException;
import io.github.valentinkarnaukhov.wiremockstubgen.flatten.FlatteningOptions;
import io.github.valentinkarnaukhov.wiremockstubgen.openapi.OpenApiReader;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.ObjectSchema;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.Property;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.StubApi;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.TypeRef;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks what Java calls a flattened accessor.
 *
 * <p>The flattener asks the target to spell the name, so these rules are checked here and
 * not against the flattener's own default.
 */
class JavaAccessorNamesTest {

    private static StubApi api;

    @BeforeAll
    static void readFixture() {
        api = new OpenApiReader(warning -> {
        }).read(Fixtures.sampleApi());
    }

    @Test
    void movesAccessorsOutOfTheWayOfTheRuntimesOwnMethods() {
        BodyScope root = flatten(TypeRef.object("ReservedNamesBody"));

        assertThat(names(root)).containsExactly(
                "_exit", "_mock", "_addNew", "safeExit", "safeInnerField", "xDashedProperty");
    }

    @Test
    void escapesReservedNamesWhateverTheirArity() {
        BodyScope root = flatten(TypeRef.object("ReservedNamesBody"));

        assertThat(accessor(root, "_exit").kind())
                .describedAs("one argument: a legal overload, but renamed anyway so the same "
                        + "property never reads two ways")
                .isEqualTo(Accessor.Kind.VALUE);
        assertThat(accessor(root, "_addNew").kind())
                .describedAs("zero arguments: an identical signature, and a compile error if left alone")
                .isEqualTo(Accessor.Kind.NESTED_LIST);
    }

    @Test
    void leavesNamesAloneOnceFlatteningHasPrefixedThem() {
        BodyScope root = flatten(TypeRef.object("ReservedNamesBody"));

        assertThat(accessor(root, "safeExit").path()).containsExactly("safe", "exit");
    }

    @Test
    void turnsWireNamesThatAreNotIdentifiersIntoOnes() {
        BodyScope root = flatten(TypeRef.object("ReservedNamesBody"));

        assertThat(accessor(root, "xDashedProperty").path()).containsExactly("x-dashed-property");
    }

    @Test
    void reportsACollisionCausedByEscapingTheSameWayAsAnyOther() {
        ObjectSchema root = new ObjectSchema("Root", List.of(
                new Property("exit", TypeRef.primitive("string", null), false),
                new Property("_exit", TypeRef.primitive("string", null), false)));
        StubApi colliding = new StubApi("hand-built", List.of(), Map.of("Root", root));

        assertThatThrownBy(() -> new Flattener(colliding, javaOptions(), warning -> {
        })
                .flatten(TypeRef.object("Root"), BodySide.REQUEST))
                .isInstanceOf(FlatteningException.class)
                .hasMessageContaining("'_exit'");
    }

    private static FlatteningOptions javaOptions() {
        return FlatteningOptions.defaults()
                .withAccessorName(new JavaAccessorNames(StubEmitter.RESERVED));
    }

    private static BodyScope flatten(TypeRef body) {
        return new Flattener(api, javaOptions(), warning -> {
        })
                .flatten(body, BodySide.RESPONSE).orElseThrow().root();
    }

    private static List<String> names(BodyScope scope) {
        return scope.accessors().stream().map(Accessor::name).toList();
    }

    private static Accessor accessor(BodyScope scope, String name) {
        return scope.accessors().stream()
                .filter(a -> a.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no accessor '" + name + "' in " + names(scope)));
    }
}
