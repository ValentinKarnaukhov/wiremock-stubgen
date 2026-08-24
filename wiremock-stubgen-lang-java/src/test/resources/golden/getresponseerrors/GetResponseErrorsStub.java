package com.example.stubs.getresponseerrors;

import com.example.model.CompositeBody;
import com.example.model.ErrorBody;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import io.github.valentinkarnaukhov.stubgen.runtime.AbstractStub;
import io.github.valentinkarnaukhov.stubgen.runtime.StubTarget;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * GOLDEN REFERENCE — hand-written specification of the generator's output.
 * Source: sample-api.yaml, operation getResponseErrors.
 *
 * <p>Demonstrates: one operation declaring several status codes whose bodies have
 * different schemas — 200 returns CompositeBody, 404 and 500 return ErrorBody.
 */
public final class GetResponseErrorsStub extends AbstractStub<GetResponseErrorsStub> {

    private static final String PATH = "/get/response/errors";

    public GetResponseErrorsStub(StubTarget target) {
        super(target);
    }

    public GetResponseErrorsStub code200(CompositeBody body) {
        return response(200, body);
    }

    public GetResponseErrorsStub code404(ErrorBody body) {
        return response(404, body);
    }

    /**
     * 500 declares the same schema as 404. The methods stay separate anyway —
     * they are generated per declared code, not per distinct schema, so the
     * generated API keeps the shape of the specification.
     */
    public GetResponseErrorsStub code500(ErrorBody body) {
        return response(500, body);
    }

    @Override
    protected MappingBuilder toRequest() {
        return get(urlPathEqualTo(PATH));
    }

    // ── NOTES ─────────────────────────────────────────────────────────────────
    //
    // ONE STUB IS ONE MAPPING, HENCE ONE RESPONSE.
    // A stub cannot answer 200 and 404 at the same time; the status and the body
    // are a single pair. Calling two codeNNN methods therefore cannot mean "both"
    // — the last call wins, silently, exactly like any other setter. Decided
    // deliberately: failing on the second call would trade a compile-time-shaped
    // API for a runtime exception, and this generator exists to move errors the
    // other way.
    //
    // Returning different responses in sequence is a WireMock scenario, not a
    // property of one mapping. That stays outside the generated surface and is
    // reached through customize(...).
    //
    // NAMING. codeNNN was chosen over respondNNN / willReturnNNN because the
    // number is the whole message and a shorter prefix keeps it prominent. It
    // also cannot collide with a parameter-derived name: parameter methods carry
    // a location prefix (queryStringParam, pathStringParam), and no location is
    // spelt "code".
    //
    // The reserved-names case in the fixture declares a query parameter literally
    // named "code" — which becomes queryCode, not code, so the collision the
    // fixture was built to provoke does not arise here. That is a consequence of
    // the prefix rule, worth stating because it is where the rule pays for itself.
    //
    // OPEN — DEFAULT RESPONSE.
    // This operation declares no "default" response. When one is present the
    // natural mapping is codeDefault(Schema), but the status to send is then
    // unknown and would have to be supplied: codeDefault(503, body). Not settled,
    // and the fixture does not cover it yet.
    //
    // OPEN — CONTENT TYPE.
    // AbstractStub defaults to application/json, which every response in the
    // fixture declares. For anything else the generated code would call
    // contentType(...) — but serialisation would then also have to stop being JSON,
    // and an operation declaring several media types for one code has no obvious
    // shape at all. Uncovered by the fixture.
    //
    // OPEN — NO RESPONSE SELECTED.
    // If the user calls neither codeNNN nor code, AbstractStub answers 200 with no
    // body at all. That is defensible for an operation whose 200 declares no
    // content, and misleading for this one, where 200 declares CompositeBody and
    // the client will fail to deserialise nothing. Whether an unset body should be
    // a failure when the selected code declares a schema is undecided.
    // ──────────────────────────────────────────────────────────────────────────
}
