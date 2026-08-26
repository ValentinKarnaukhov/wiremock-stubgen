package com.example.stubs.postbyrequestbodycomposite;

import com.example.model.CompositeBody;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import io.github.valentinkarnaukhov.wiremockstubgen.runtime.AbstractRequestBodyMatcher;
import io.github.valentinkarnaukhov.wiremockstubgen.runtime.AbstractStub;
import io.github.valentinkarnaukhov.wiremockstubgen.runtime.StubTarget;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * GOLDEN REFERENCE — hand-written specification of the generator's output.
 * Source: sample-api.yaml, operation postByRequestBodyComposite.
 *
 * <p>Demonstrates: a request body constrained through nested matchers, mirroring the
 * way a response body is described.
 *
 * <p>Reads as:
 *
 * <pre>{@code
 * new PostByRequestBodyCompositeStub(target)
 *         .requestBody()
 *             .primitive("FIRST")
 *             .compositeDeepFieldDeepestField("DEEP")
 *             .compositeList().innerField("A")
 *         .mock();
 * }</pre>
 */
public final class PostByRequestBodyCompositeStub extends AbstractStub<PostByRequestBodyCompositeStub> {

    private static final String PATH = "/post/request-body/composite";

    public PostByRequestBodyCompositeStub(StubTarget target) {
        super(target);
    }

    /**
     * Matches a request whose body equals this object as JSON. The model type comes
     * from the consumer's own client — we only reference it, see the modelPackage
     * option.
     *
     * <p>Named requestBody rather than body: it stands next to codeNNN(), which says
     * which direction it describes, and body() would not.
     */
    public PostByRequestBodyCompositeStub requestBody(CompositeBody body) {
        return matchWholeRequestBody(body);
    }

    /**
     * Constrains the request body field by field. Partial by construction, which is
     * what the whole-document form above cannot be.
     */
    public CompositeBodyMatcher<PostByRequestBodyCompositeStub> requestBody() {
        return new CompositeBodyMatcher<>(this, "$");
    }

    /** 200 declares no content, so the method takes no body and returns no builder. */
    public PostByRequestBodyCompositeStub code200() {
        return response(200, null);
    }

    @Override
    protected MappingBuilder toRequest() {
        return post(urlPathEqualTo(PATH));
    }

    // ── REQUEST BODY MATCHERS ─────────────────────────────────────────────────
    //
    // Inner classes for the same reason the body builders are: one operation, one
    // file. Being inner also lets a matcher reach addRequestBodyPattern through the
    // enclosing instance, so that method stays protected.
    //
    // A matcher carries the JSONPath it is rooted at instead of baking it into the
    // method names, which is why one class covers both the body itself and the
    // elements of a nested list.

    public final class CompositeBodyMatcher<P> extends AbstractRequestBodyMatcher<P> {

        CompositeBodyMatcher(P parent, String path) {
            super(parent, PostByRequestBodyCompositeStub.this, path,
                    PostByRequestBodyCompositeStub.this::addRequestBodyPattern);
        }

        public CompositeBodyMatcher<P> primitive(String value) {
            match(".primitive", equalTo(value));
            return this;
        }

        public CompositeBodyMatcher<P> compositeInnerField(String value) {
            match(".composite.innerField", equalTo(value));
            return this;
        }

        public CompositeBodyMatcher<P> compositeDeepFieldDeepestField(String value) {
            match(".composite.deepField.deepestField", equalTo(value));
            return this;
        }

        /**
         * A list of primitives has nothing inside it to reach for, so the question is
         * whether any element equals the value. Equality against the whole list is a
         * different question, which requestBody(...) already answers.
         */
        public CompositeBodyMatcher<P> primitiveList(String value) {
            match(".primitiveList[?(@ == '" + value + "')]");
            return this;
        }

        /**
         * Descends into the elements of a list of objects. There is no addNew() and
         * there cannot be: matching creates nothing. [*] means any element, so this
         * matches a request where at least one element qualifies.
         */
        public CompositeFieldMatcher<CompositeBodyMatcher<P>> compositeList() {
            return new CompositeFieldMatcher<>(this, path() + ".compositeList[*]");
        }
    }

    public final class CompositeFieldMatcher<P> extends AbstractRequestBodyMatcher<P> {

        CompositeFieldMatcher(P parent, String path) {
            super(parent, PostByRequestBodyCompositeStub.this, path,
                    PostByRequestBodyCompositeStub.this::addRequestBodyPattern);
        }

        public CompositeFieldMatcher<P> innerField(String value) {
            match(".innerField", equalTo(value));
            return this;
        }

        public CompositeFieldMatcher<P> deepFieldDeepestField(String value) {
            match(".deepField.deepestField", equalTo(value));
            return this;
        }
    }

    // ── THE TWO ENDS OF A STUB READ ALIKE ─────────────────────────────────────
    //
    // requestBody() and codeNNN() hand out builders over the same schema with the
    // same method names; neither shares a namespace with the other, so the accessors
    // need no request/response prefix. The prefix stays on the entry point, because
    // codeNNN() names its direction and a bare body() would name nothing.
    //
    // The symmetry is in the names only. Underneath, a body builder constructs an
    // object and a matcher appends a JSONPath expression; a matcher has nothing to
    // create. The difference surfaces at lists: the builder has addNew(), the matcher
    // has [*].
    //
    // SETTLED — PARTIAL MATCHING. equalToJson compares the whole document, so the
    // matcher is the only way to say "any request whose composite.innerField is x",
    // and it does so without naming the field as a string.
    //
    // SETTLED — WHY THERE IS NO equalTo ON THE MATCHER.
    // WireMock applies a value pattern on a path ending in [*] to the entire
    // selection rendered as an array, not to each element, so
    // matchingJsonPath("$.compositeList[*]", equalToJson(oneElement)) does not match a
    // document containing that element, while equalToJson(theWholeList) does. Since a
    // matcher class serves every position its schema occupies, and the nested
    // positions here are list elements, such a method would read as "some element
    // equals this" and mean "the whole list equals this".
    //
    // OPEN — CONDITIONS ON THE SAME ELEMENT.
    // compositeList().innerField("A").deepFieldDeepestField("B") emits two
    // independent matchers, so a request whose first element carries A and whose
    // second carries B matches. Asking for one element carrying both needs a single
    // filter — $.compositeList[?(@.innerField == 'A' && …)] — which this shape cannot
    // build, because each call has already been sent to the stub. WireMock does
    // understand the filter form; the obstacle is when the expression is assembled.
    //
    // OPEN — ESCAPING.
    // primitiveList interpolates the value into the filter expression, so a quote in
    // the value breaks it. The others pass the value to equalTo, which does not parse
    // it.
    //
    // OPEN — SERIALISATION.
    // serialize(body) is AbstractStub's, so matcher and response body go through one
    // mapper. Which mapper is undecided: the default delegates to WireMock's own
    // Json, whose configuration need not match the consumer's client; the consumer's
    // models carry Jackson annotations from openapi-generator that a foreign mapper
    // will not honour; and wiremock-standalone relocates Jackson, so under that
    // artifact the default cannot see those annotations at all. Overriding serialize
    // is today's escape hatch.
    //
    // OPEN — NON-OBJECT REQUEST BODIES.
    // A list body would be rooted at $[*] with requestBody(List<CompositeBody>). For a
    // bare string body equalToJson is wrong and there is nothing to build a matcher
    // over, so the emitter must pick from the schema kind rather than assume JSON
    // objects.
    // ──────────────────────────────────────────────────────────────────────────
}
