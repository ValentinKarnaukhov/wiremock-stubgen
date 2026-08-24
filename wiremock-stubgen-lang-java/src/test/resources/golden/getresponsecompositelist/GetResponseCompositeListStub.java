package com.example.stubs.getresponsecompositelist;

import com.example.model.CompositeBody;
import com.example.model.CompositeDeepField;
import com.example.model.CompositeField;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
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

    /**
     * A flattened accessor: it reaches CompositeBody.composite.innerField without the
     * caller naming CompositeField at all.
     *
     * <p>Recorded rather than applied, so that it works whether or not code200 has
     * been called, and in either order.
     *
     * <p>The body is a list, which forces a policy the specification cannot supply:
     * this writes to every element. Writing to the first, or to an index, would be
     * equally defensible. The reference project always chose every element.
     */
    public GetResponseCompositeListStub compositeInnerField(String value) {
        return mutateBody((List<CompositeBody> body) ->
                body.forEach(item -> item.getComposite().innerField(value)));
    }

    /**
     * Reached only when a flattened accessor is used without a body having been
     * supplied. See AbstractStub#skeletonBody for why it is built lazily.
     *
     * <p>One element, because an accessor that writes to every element of an empty
     * list would silently do nothing — the failure mode a generated API must not
     * have.
     */
    @Override
    protected Object skeletonBody() {
        List<CompositeBody> body = new ArrayList<>();
        body.add(skeletonCompositeBody());
        return body;
    }

    /**
     * Only nested objects are created. Collections are left alone: openapi-generator
     * already initialises list properties to an empty list, verified on its output
     * for this specification.
     */
    private static CompositeBody skeletonCompositeBody() {
        List<CompositeField> compositeList = new ArrayList<>();
        compositeList.add(skeletonCompositeField());
        return new CompositeBody()
                .composite(skeletonCompositeField())
                .compositeList(compositeList);
    }

    private static CompositeField skeletonCompositeField() {
        return new CompositeField().deepField(new CompositeDeepField());
    }

    @Override
    protected MappingBuilder toRequest() {
        return get(urlPathEqualTo(PATH));
    }

    // ── FLATTENING: WHAT IS SETTLED, AND WHAT IS NOT ─────────────────────────
    //
    // SETTLED — the accessor no longer needs a body to exist first. It records a
    // change and AbstractStub applies it at build time, against the supplied body
    // or against skeletonBody(). code200 and the accessors work in either order,
    // which is what the self-type design promised everywhere else.
    //
    // SETTLED — the skeleton is lazy. Built in the constructor it would change what
    // every stub answers by default: measured on openapi-generator's own models for
    // this specification, an eager skeleton turns the default body from
    //
    //     [ { "primitiveList": [], "compositeList": [] } ]
    //
    // into
    //
    //     [ { "composite": { "deepField": {} },
    //         "primitiveList": [],
    //         "compositeList": [ { "deepField": {} } ] } ]
    //
    // — empty nested objects and a phantom list element the service would never
    // send, in every test that only wanted a 200.
    //
    // SETTLED — collections are not part of the skeleton. openapi-generator already
    // initialises list properties to an empty list; only nested objects are null.
    //
    // DECIDED BY FIAT, NOT DERIVED — a flattened accessor writes to every element of
    // a list. Writing to the first, or by index, is equally defensible; the
    // specification says nothing either way. Copied from the reference project
    // because it is what its authors always reached for.
    //
    // OPEN — flattened names collide. compositeInnerField is unambiguous here. On a
    // specification with 149 schemas two distinct paths readily produce the same
    // flat name, and there is no principled winner. Including the full path in the
    // name would be unambiguous and unreadable.
    //
    // OPEN — termination. RecursiveBody refers to itself directly, through a list,
    // and indirectly via RecursiveField, so both the skeleton and the accessor walk
    // must stop somewhere. maxDepth caps the walk but is a cap, not a termination
    // condition. swagger-parser does not inline $ref, so cycle detection is ours in
    // any case.
    //
    // OPEN — how many accessors is too many. Every leaf of every nested object
    // becomes a method. The depth-3 chain here yields a handful; a real schema
    // yields hundreds, and the JVM caps a class at 65535 methods long after the
    // class stops being readable.
    // ──────────────────────────────────────────────────────────────────────────
}
