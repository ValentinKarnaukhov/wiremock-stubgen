package io.github.valentinkarnaukhov.stubgen.flatten;

import io.github.valentinkarnaukhov.stubgen.openapi.OpenApiReader;
import io.github.valentinkarnaukhov.stubgen.spec.ObjectSchema;
import io.github.valentinkarnaukhov.stubgen.spec.Property;
import io.github.valentinkarnaukhov.stubgen.spec.StubApi;
import io.github.valentinkarnaukhov.stubgen.spec.TypeRef;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the flattener against the rules the golden stubs were written to.
 *
 * <p>The everyday rules are checked against the fixture, so that a change to the reader
 * or to the specification shows up here rather than in generated output. The three
 * hostile cases — a name collision, a schema wide enough to need a low depth limit, a
 * body that is not an object — are built by hand: a fixture that contained a collision
 * could not be flattened at all, and every other test would fail with it.
 */
class FlattenerTest {

    private static final Path SPEC = Path.of("src/test/resources/specs/sample-api.yaml");

    private static StubApi api;

    @BeforeAll
    static void readFixture() {
        api = new OpenApiReader(warning -> {
        }).read(SPEC);
    }

    // ── F1, F2, F3, F4: what each property shape becomes ──────────────────────

    @Test
    void liftsSingleValuedObjectsIntoCompoundNamesAndLeavesEverythingElseInPlace() {
        BodyScope root = flatten(compositeBodyList(), BodySide.RESPONSE).root();

        assertThat(names(root)).containsExactly(
                "primitive",
                "compositeInnerField",
                "compositeDeepFieldDeepestField",
                "primitiveList",
                "compositeList");

        assertThat(accessor(root, "compositeDeepFieldDeepestField").path())
                .containsExactly("composite", "deepField", "deepestField");
        assertThat(accessor(root, "primitive").kind()).isEqualTo(Accessor.Kind.VALUE);
    }

    @Test
    void treatsAnArrayOfLeavesAsALeafAndReportsTheElementType() {
        Accessor primitiveList = accessor(flatten(compositeBodyList(), BodySide.RESPONSE).root(), "primitiveList");

        assertThat(primitiveList.kind()).isEqualTo(Accessor.Kind.VALUE_LIST);
        assertThat(primitiveList.type().openApiType()).isEqualTo("string");
        assertThat(primitiveList.targetSchemaIfPresent()).isEmpty();
    }

    @Test
    void handsOutAScopeForAnArrayOfObjectsAndStopsFlatteningThere() {
        BodyModel model = flatten(compositeBodyList(), BodySide.RESPONSE);
        Accessor compositeList = accessor(model.root(), "compositeList");

        assertThat(compositeList.kind()).isEqualTo(Accessor.Kind.NESTED_LIST);
        assertThat(compositeList.targetSchema()).isEqualTo("CompositeField");

        BodyScope nested = model.target(compositeList).orElseThrow();
        assertThat(nested.listPosition()).isTrue();
        assertThat(names(nested)).containsExactly("innerField", "deepFieldDeepestField");
    }

    // ── F5, F6: how many scopes each side needs, and where the root sits ───────

    @Test
    void countsScopesPerPositionOnAResponseAndPerSchemaOnARequest() {
        BodyModel response = flatten(compositeBodyList(), BodySide.RESPONSE);
        assertThat(response.rootIsList()).isTrue();
        assertThat(response.root().listPosition()).isTrue();

        BodyModel request = flatten(compositeBodyList(), BodySide.REQUEST);
        assertThat(request.rootIsList()).isTrue();
        assertThat(request.root().listPosition())
                .describedAs("a matcher is one class wherever it stands; the position lives in its path")
                .isFalse();
        assertThat(request.scopes()).allSatisfy(scope -> assertThat(scope.listPosition()).isFalse());
    }

    @Test
    void readsBothSidesOfOneSchemaUnderTheSameNames() {
        BodyModel response = flatten(compositeBodyList(), BodySide.RESPONSE);
        BodyModel request = flatten(compositeBodyList(), BodySide.REQUEST);

        assertThat(names(request.root())).isEqualTo(names(response.root()));
    }

    @Test
    void producesNoModelForABodyItCannotTakeApart() {
        Flattener flattener = new Flattener(api, FlatteningOptions.defaults());

        assertThat(flattener.flatten(null, BodySide.RESPONSE)).isEmpty();
        assertThat(flattener.flatten(TypeRef.primitive("string", null), BodySide.RESPONSE)).isEmpty();
        assertThat(flattener.flatten(TypeRef.array(TypeRef.primitive("string", null)), BodySide.RESPONSE)).isEmpty();
        assertThat(flattener.flatten(TypeRef.object("NoSuchSchema"), BodySide.RESPONSE)).isEmpty();
    }

    // ── F8: names that would land on a method the scope inherits ──────────────

    @Test
    void movesAccessorsOutOfTheWayOfTheRuntimesOwnMethods() {
        BodyScope root = flatten(TypeRef.object("ReservedNamesBody"), BodySide.RESPONSE, javaOptions()).root();

        assertThat(names(root)).containsExactly(
                "_exit", "_mock", "_addNew", "safeExit", "safeInnerField", "xDashedProperty");
    }

    @Test
    void escapesReservedNamesWhateverTheirArity() {
        BodyScope root = flatten(TypeRef.object("ReservedNamesBody"), BodySide.RESPONSE, javaOptions()).root();

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
        BodyScope root = flatten(TypeRef.object("ReservedNamesBody"), BodySide.RESPONSE, javaOptions()).root();

        assertThat(accessor(root, "safeExit").path()).containsExactly("safe", "exit");
    }

    @Test
    void turnsWireNamesThatAreNotIdentifiersIntoOnes() {
        BodyScope root = flatten(TypeRef.object("ReservedNamesBody"), BodySide.RESPONSE, javaOptions()).root();

        assertThat(accessor(root, "xDashedProperty").path()).containsExactly("x-dashed-property");
    }

    // ── depth, and the cycles it makes harmless ───────────────────────────────

    @Test
    void stopsFlatteningAtTheDepthLimitAndCountsItInsideEachScope() {
        BodyScope shallow = flatten(TypeRef.object("RecursiveBody"), BodySide.RESPONSE, depth(1)).root();
        assertThat(names(shallow)).containsExactly("primitive", "recursiveList");

        BodyScope deeper = flatten(TypeRef.object("RecursiveBody"), BodySide.RESPONSE, depth(3)).root();
        assertThat(names(deeper)).contains("recursivePrimitive", "recursiveRecursiveFieldInnerField");
        assertThat(deeper.accessors()).allSatisfy(a -> assertThat(a.depth()).isLessThanOrEqualTo(3));
    }

    @Test
    void terminatesOnASchemaThatContainsItself() {
        BodyModel model = flatten(TypeRef.object("RecursiveBody"), BodySide.RESPONSE, depth(4));

        assertThat(model.scopes()).extracting(BodyScope::id)
                .describedAs("a cycle through a list needs one scope per position, and no more")
                .containsExactly("RecursiveBody", "RecursiveBody[]");
    }

    @Test
    void givesADeepSchemaTheSameAccessorsWhereverItIsReached() {
        BodyModel model = flatten(TypeRef.object("RecursiveBody"), BodySide.RESPONSE, depth(3));

        assertThat(names(model.scope("RecursiveBody", true).orElseThrow()))
                .isEqualTo(names(model.root()));
    }

    // ── the fatal case ────────────────────────────────────────────────────────

    @Test
    void refusesToGenerateTwoAccessorsThatWouldShareAName() {
        StubApi colliding = apiOf(
                schema("Root",
                        property("composite", TypeRef.object("Inner")),
                        property("compositeInner", TypeRef.object("Outer"))),
                schema("Inner", property("innerField", string())),
                schema("Outer", property("field", string())));

        assertThatThrownBy(() -> new Flattener(colliding, FlatteningOptions.defaults())
                .flatten(TypeRef.object("Root"), BodySide.RESPONSE))
                .isInstanceOf(FlatteningException.class)
                .hasMessageContaining("compositeInnerField")
                .hasMessageContaining("composite.innerField")
                .hasMessageContaining("compositeInner.field");
    }

    @Test
    void reportsACollisionCausedByEscapingTheSameWayAsAnyOther() {
        StubApi colliding = apiOf(
                schema("Root", property("exit", string()), property("_exit", string())));

        assertThatThrownBy(() -> new Flattener(colliding, javaOptions())
                .flatten(TypeRef.object("Root"), BodySide.REQUEST))
                .isInstanceOf(FlatteningException.class)
                .hasMessageContaining("'_exit'");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static TypeRef compositeBodyList() {
        return TypeRef.array(TypeRef.object("CompositeBody"));
    }

    private static FlatteningOptions javaOptions() {
        return FlatteningOptions.defaults()
                .withReservedNames(Set.of("exit", "mock", "root", "buildStub", "addNew", "path", "match"));
    }

    private static FlatteningOptions depth(int maxDepth) {
        return FlatteningOptions.defaults().withMaxDepth(maxDepth);
    }

    private static BodyModel flatten(TypeRef body, BodySide side) {
        return flatten(body, side, FlatteningOptions.defaults());
    }

    private static BodyModel flatten(TypeRef body, BodySide side, FlatteningOptions options) {
        return new Flattener(api, options).flatten(body, side).orElseThrow();
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

    private static TypeRef string() {
        return TypeRef.primitive("string", null);
    }

    private static Property property(String name, TypeRef type) {
        return new Property(name, type, false);
    }

    private static ObjectSchema schema(String name, Property... properties) {
        return new ObjectSchema(name, List.of(properties));
    }

    private static StubApi apiOf(ObjectSchema... schemas) {
        Map<String, ObjectSchema> byName = new LinkedHashMap<>();
        for (ObjectSchema schema : schemas) {
            byName.put(schema.name(), schema);
        }
        return new StubApi("hand-built", List.of(), byName);
    }
}
