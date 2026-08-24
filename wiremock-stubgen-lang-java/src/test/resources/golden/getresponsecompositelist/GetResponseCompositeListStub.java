package com.example.stubs.getresponsecompositelist;

import com.example.model.CompositeBody;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import io.github.valentinkarnaukhov.stubgen.runtime.AbstractStub;
import io.github.valentinkarnaukhov.stubgen.runtime.StubTarget;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * GOLDEN REFERENCE — hand-written specification of the generator's output.
 * Source: sample-api.yaml, operation getResponseCompositeList.
 *
 * <p>Demonstrates: a typed response body, and the shape explosion would take.
 */
public final class GetResponseCompositeListStub extends AbstractStub<GetResponseCompositeListStub> {

    private static final String PATH = "/get/response/composite/list";

    public GetResponseCompositeListStub(StubTarget target) {
        super(target);
    }

    /** Typed because the specification says 200 returns exactly this. */
    public GetResponseCompositeListStub code200(List<CompositeBody> body) {
        return response(200, body);
    }

    @Override
    protected MappingBuilder toRequest() {
        return get(urlPathEqualTo(PATH));
    }

    // ── THE EXPLODE QUESTION ──────────────────────────────────────────────────
    //
    // This is the decision the whole project turns on, and it is NOT settled.
    //
    // What the hand-written code in the reference project actually does is reach
    // into the RESPONSE BODY, not the request:
    //
    //     public Mock withMaterialNumber(String v) {
    //         items.forEach(dto -> dto.getNested().materialNumber(v));
    //         return this;
    //     }
    //
    // Two things follow, and both are awkward.
    //
    // (a) Explosion mutates a body that must already exist. The accessor above is
    //     meaningless on an empty list. So exploded setters are not an
    //     alternative to code200(body) — they are an operation applied AFTER it,
    //     which makes call order significant. That contradicts the self-type
    //     design, where order is deliberately free.
    //
    // (b) Collections force a policy. CompositeBody.compositeList is a list of
    //     CompositeField. Does the exploded setter write to every element, to the
    //     first, or to an index? The reference code always chose "every element",
    //     but that is a domain decision, not a mechanical one.
    //
    // A shape that would work here:
    //
    //     .code200(List.of(new CompositeBody()))
    //     .compositeInnerField("x")    // -> body.forEach(b -> b.getComposite().innerField("x"))
    //
    // On this specification the flattened name is unambiguous. On a real one with
    // 149 schemas it is not: two distinct paths readily produce the same flat
    // name, and the emitter has no principled way to pick a winner.
    //
    // Termination is a separate problem. RecursiveBody in the fixture refers to
    // itself directly, through a list, and indirectly via RecursiveField.
    // maxDepth caps the walk but is a workaround, not a termination condition —
    // and swagger-parser does not inline $ref, so cycle detection is ours.
    // ──────────────────────────────────────────────────────────────────────────
}
