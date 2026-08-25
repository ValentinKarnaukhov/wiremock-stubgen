package com.example.stubs.getresponsecompositelist;

import com.example.model.CompositeBody;
import com.example.model.CompositeDeepField;
import com.example.model.CompositeField;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import io.github.valentinkarnaukhov.stubgen.runtime.AbstractResponseBodyBuilder;
import io.github.valentinkarnaukhov.stubgen.runtime.AbstractStub;
import io.github.valentinkarnaukhov.stubgen.runtime.StubTarget;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * GOLDEN REFERENCE — hand-written specification of the generator's output.
 * Source: sample-api.yaml, operation getResponseCompositeList.
 *
 * <p>Demonstrates: a response body described through nested builders rather than
 * flattened onto the stub, and an array at the root of that body.
 *
 * <p>Reads as:
 *
 * <pre>{@code
 * new GetResponseCompositeListStub(target)
 *         .code200()
 *             .primitive("FIRST")
 *             .compositeDeepFieldDeepestField("DEEP")
 *             .compositeList().addNew().innerField("A")
 *                             .addNew().deepFieldDeepestField("B").exit()
 *             .addNew().primitive("SECOND")
 *         .mock();
 * }</pre>
 */
public final class GetResponseCompositeListStub extends AbstractStub<GetResponseCompositeListStub> {

    private static final String PATH = "/get/response/composite/list";

    public GetResponseCompositeListStub(StubTarget target) {
        super(target);
    }

    /**
     * Answers 200 with a body the caller already has. Typed because the
     * specification says 200 returns exactly this.
     */
    public GetResponseCompositeListStub code200(List<CompositeBody> body) {
        return response(200, body);
    }

    /**
     * Answers 200 with a body described field by field. The list starts empty and
     * the builder writes into it, so nothing is sent that was not asked for.
     *
     * <p>Both forms of code200 exist because they answer different questions —
     * "send this object" and "send a body shaped like this" — and neither reduces
     * to the other. The argument form is the only way to reuse a body built
     * elsewhere, the builder form the only way to avoid naming every intermediate
     * type.
     */
    public CompositeBodyListBuilder<GetResponseCompositeListStub> code200() {
        List<CompositeBody> body = new ArrayList<>();
        response(200, body);
        return new CompositeBodyListBuilder<>(this, body);
    }

    @Override
    protected MappingBuilder toRequest() {
        return get(urlPathEqualTo(PATH));
    }

    // ── BODY BUILDERS ─────────────────────────────────────────────────────────
    //
    // Inner classes, not a shared package: one operation stays one file. A reader
    // of this stub never has to look elsewhere to find out what code200() hands
    // back, and a schema used by forty operations is written out forty times.
    // Measured over 62 real specifications, sharing would have produced 831
    // builder classes against 1454 nested ones — 1.7x, paid for locality.
    //
    // The type parameter stays. Nesting alone does not shorten anything: javac
    // prints the simple name of a nested class even across packages, so the
    // generic form reads identically wherever it is declared. Dropping <P> is what
    // would shorten it, and that is what cannot be afforded — see the note at the
    // bottom.

    /**
     * Writes into the elements of the response list. The list builder is also the
     * element builder: splitting them would be tidier, but it costs an exit() per
     * element, and describing three elements would end in three of them in a row.
     */
    public final class CompositeBodyListBuilder<P> extends AbstractResponseBodyBuilder<P> {

        private final List<CompositeBody> items;

        private CompositeBody current;

        CompositeBodyListBuilder(P parent, List<CompositeBody> items) {
            super(parent, GetResponseCompositeListStub.this);
            this.items = items;
        }

        /** Appends an element and makes it the one the accessors below write to. */
        public CompositeBodyListBuilder<P> addNew() {
            current = new CompositeBody();
            items.add(current);
            return this;
        }

        public CompositeBodyListBuilder<P> primitive(String value) {
            current().primitive(value);
            return this;
        }

        // A nested object is flattened into the name rather than given a builder:
        // a builder would buy a shorter name at the price of two more exit() calls.

        public CompositeBodyListBuilder<P> compositeInnerField(String value) {
            composite().innerField(value);
            return this;
        }

        public CompositeBodyListBuilder<P> compositeDeepFieldDeepestField(String value) {
            compositeDeepField().deepestField(value);
            return this;
        }

        /**
         * A list of primitives is a leaf: there is nothing inside it to reach for.
         *
         * <p>It sits here, and not next to primitive(), because accessors are emitted
         * in the order the specification declares the properties they came from. That
         * order is what the request matcher over this same schema also follows, and
         * the two reading alike matters more than grouping by shape.
         */
        public CompositeBodyListBuilder<P> primitiveList(List<String> value) {
            current().primitiveList(value);
            return this;
        }

        /**
         * A list of objects does get a builder. Without one the generated API has
         * to decide by itself which element a value belongs to, and every answer —
         * the first, all of them, one per call — is a guess the specification
         * cannot support. addNew() makes the caller say it.
         */
        public CompositeFieldListBuilder<CompositeBodyListBuilder<P>> compositeList() {
            CompositeBody item = current();
            if (item.getCompositeList() == null) {
                item.compositeList(new ArrayList<>());
            }
            return new CompositeFieldListBuilder<>(this, item.getCompositeList());
        }

        // ── LAZY CREATION ─────────────────────────────────────────────────────
        //
        // Each accessor materialises the objects on its own path and nothing else.
        // That replaces the whole-body skeleton an earlier draft installed: a
        // skeleton has to decide in advance how deep to go, which a recursive
        // schema makes unanswerable, and it leaves empty objects in bodies nobody
        // described.

        private CompositeBody current() {
            if (current == null) {
                addNew();
            }
            return current;
        }

        private CompositeField composite() {
            CompositeBody item = current();
            if (item.getComposite() == null) {
                item.composite(new CompositeField());
            }
            return item.getComposite();
        }

        private CompositeDeepField compositeDeepField() {
            CompositeField composite = composite();
            if (composite.getDeepField() == null) {
                composite.deepField(new CompositeDeepField());
            }
            return composite.getDeepField();
        }
    }

    /**
     * Writes into the elements of the nested compositeList.
     */
    public final class CompositeFieldListBuilder<P> extends AbstractResponseBodyBuilder<P> {

        private final List<CompositeField> items;

        private CompositeField current;

        CompositeFieldListBuilder(P parent, List<CompositeField> items) {
            super(parent, GetResponseCompositeListStub.this);
            this.items = items;
        }

        public CompositeFieldListBuilder<P> addNew() {
            current = new CompositeField();
            items.add(current);
            return this;
        }

        public CompositeFieldListBuilder<P> innerField(String value) {
            current().innerField(value);
            return this;
        }

        public CompositeFieldListBuilder<P> deepFieldDeepestField(String value) {
            deepField().deepestField(value);
            return this;
        }

        /**
         * An accessor called before addNew() creates the element it needs. Without
         * this the call would write to nothing at all, which is the one failure
         * mode a generated API must not have.
         */
        private CompositeField current() {
            if (current == null) {
                addNew();
            }
            return current;
        }

        private CompositeDeepField deepField() {
            CompositeField item = current();
            if (item.getDeepField() == null) {
                item.deepField(new CompositeDeepField());
            }
            return item.getDeepField();
        }
    }

    // ── WHY THE BODY MOVED OFF THE STUB ───────────────────────────────────────
    //
    // An earlier draft put one method per leaf on the stub itself, named
    // response200CompositeDeepFieldDeepestField and so on, recorded each call as a
    // deferred mutation, and installed a skeleton body so the mutations had
    // something to walk. All of that was machinery to keep flat names working. The
    // builders delete it:
    //
    //   - the status code leaves the name, because a builder is only reachable
    //     through the code that created the body. Two codes declaring one schema
    //     stop producing two identical method sets — see GetResponseErrorsStub;
    //   - mutations need not be deferred, because there is no order in which the
    //     body does not yet exist: code200() creates it before returning;
    //   - the skeleton is gone, replaced by creation along the accessor's own path;
    //   - the request/response prefix is gone with it, since request matchers and
    //     response fields no longer share a namespace.
    //
    // SETTLED — WHICH ELEMENT OF A LIST A VALUE BELONGS TO.
    // The flat design had to pick, and picked "every element" by fiat, copied from
    // the reference project. addNew() removes the question: the caller says which.
    //
    // THE PRICE — ORDER.
    // Inside code200() the stub's own methods are out of scope until exit(). A stub
    // with query parameters must therefore be configured before its response. That
    // is a compile error, not a wrong result, which is the trade this generator
    // exists to make — but it does contradict the order-free chaining the self-type
    // gives everywhere else.
    //
    // WHY THE TYPE PARAMETER SURVIVES.
    // Without <P> these classes would nest concretely and exit() would name its
    // parent outright, which reads far better in compiler errors:
    // Stub.Code200.CompositeList.Rec against
    // RecursiveBuilder<RecursiveBuilder<CompositeFieldListBuilder<...>>>.
    //
    // The argument that settles it is a schema sitting at two positions in one
    // operation — 7% of positions measured over 62 specifications. Each position
    // needs its own class, because each has a different exit() type, so the concrete
    // form pays in suffixed duplicates exactly where the generic form pays nothing.
    //
    // A recursive schema is the second count, and a weaker one than an earlier draft
    // of this comment claimed. It only bites if the flattening cap hands out a
    // sub-builder when it stops, because then the chain is unbounded and a concrete
    // class per level is impossible — exit() starts skipping levels, which javac
    // accepts, making it a silent divergence. If the cap instead just stops
    // generating accessors, this count does not arise at all. That choice is open.
    // Cycles are in any case 0.7% of schemas, an order of magnitude rarer than the
    // multi-position case above.
    //
    // Generating the concrete form only where it is safe was rejected for the
    // reason D15 rejected prefix-on-collision: the shape of a class would then
    // depend on its neighbours in the schema graph.
    //
    // OPEN — FLAT NAMES STILL COLLIDE.
    // Nesting shrinks the namespace to one schema, which removes the collisions
    // between operations and between codes. Two paths within one schema can still
    // meet: composite.innerField and compositeInner.field both give
    // compositeInnerField. Rarer, but still without a principled winner.
    // ──────────────────────────────────────────────────────────────────────────
}
