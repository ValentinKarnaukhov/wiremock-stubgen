package com.example.stubs;

import com.example.model.CompositeBody;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import io.github.valentinkarnaukhov.stubgen.runtime.AbstractStub;
import io.github.valentinkarnaukhov.stubgen.runtime.StubTarget;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * GOLDEN REFERENCE — hand-written specification of the generator's output.
 * Source: sample-api.yaml, tag get-response-composite-list.
 *
 * <p>Demonstrates: a typed response body, and the shape explosion would take.
 */
public final class GetResponseCompositeListStub {

    private final StubTarget target;

    public GetResponseCompositeListStub(StubTarget target) {
        this.target = target;
    }

    public GetResponseCompositeList getResponseCompositeList() {
        return new GetResponseCompositeList(target);
    }

    public static final class GetResponseCompositeList extends AbstractStub<GetResponseCompositeList> {

        private static final String PATH = "/get/response/composite/list";

        private int status = 200;
        private List<CompositeBody> body = new ArrayList<>();

        public GetResponseCompositeList(StubTarget target) {
            super(target);
        }

        /** Typed because the specification says 200 returns exactly this. */
        public GetResponseCompositeList code200(List<CompositeBody> body) {
            this.status = 200;
            this.body = body;
            return self();
        }

        /** Undeclared status codes carry no typed body by definition. */
        public GetResponseCompositeList code(int status) {
            this.status = status;
            this.body = null;
            return self();
        }

        @Override
        protected MappingBuilder toMappingBuilder() {
            return get(urlPathEqualTo(PATH))
                    .willReturn(aResponse()
                            .withStatus(status)
                            .withHeader("Content-Type", "application/json")
                            .withBody(Json.write(body)));
        }
    }

    // ── THE EXPLODE QUESTION ──────────────────────────────────────────────────
    //
    // This is the decision the whole project turns on, and it is NOT settled.
    //
    // What the hand-written code in the reference project actually does is
    // reach into the RESPONSE BODY, not the request:
    //
    //     public Mock withMaterialNumber(String v) {
    //         items.forEach(dto -> dto.getGfAp().materialNumber(v));
    //         return this;
    //     }
    //
    // Two things follow, and both are awkward here.
    //
    // (a) Explosion mutates a body that must already exist. The accessor above
    //     is meaningless on an empty list. So exploded setters are not an
    //     alternative to code200(body) — they are an operation applied AFTER it,
    //     which makes call order significant. That contradicts the self-type
    //     design, where order is deliberately free.
    //
    // (b) Collections force a policy. CompositeBody.compositeList is a list of
    //     CompositeField. Does the exploded setter write to every element, to
    //     the first, or to an index? The reference code always chose "every
    //     element", but that is a domain decision, not a mechanical one.
    //
    // A shape that would work for this specification:
    //
    //     .code200(List.of(new CompositeBody()))
    //     .compositeInnerField("x")        // -> body.forEach(b -> b.getComposite().innerField("x"))
    //
    // Note the name: composite + innerField, flattened. On this specification
    // that is unambiguous. On a real one with 149 schemas it is not — two
    // distinct paths readily produce the same flat name, and the emitter has
    // no principled way to choose a winner.
    //
    // Also unresolved: CompositeField could reference CompositeBody back, and
    // nothing here would stop the walk. maxDepth caps it, but a cap is a
    // workaround, not a termination condition.
    //
    // Proposal: do NOT design explosion from this specification. It is too
    // small to expose the failure modes. Extend the fixture first with a
    // self-referencing schema and a depth-3 chain, then decide.
    // ──────────────────────────────────────────────────────────────────────────
}
