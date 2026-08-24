package com.example.stubs.getbyparametersinpath;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import io.github.valentinkarnaukhov.stubgen.runtime.AbstractStub;
import io.github.valentinkarnaukhov.stubgen.runtime.StubTarget;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;

/**
 * GOLDEN REFERENCE — hand-written specification of the generator's output.
 * Source: sample-api.yaml, operation getByInPathParameters.
 *
 * <p>Demonstrates: templated paths. The predecessor project declared a urlPattern
 * field and never assigned it, so paths with parameters did not work at all.
 */
public final class GetByInPathParametersStub extends AbstractStub<GetByInPathParametersStub> {

    /**
     * Kept as a template rather than assembled by concatenation, so that an unset
     * parameter degrades to "match any value in this segment" instead of
     * producing a broken URL.
     */
    private static final String PATH_TEMPLATE = "/get/parameters/in-path/{stringParam}/{longParam}";

    private final Map<String, StringValuePattern> pathParams = new LinkedHashMap<>();

    public GetByInPathParametersStub(StubTarget target) {
        super(target);
    }

    public GetByInPathParametersStub pathStringParam(String value) {
        pathParams.put("stringParam", equalTo(value));
        return self();
    }

    public GetByInPathParametersStub pathLongParam(Long value) {
        pathParams.put("longParam", equalTo(String.valueOf(value)));
        return self();
    }

    /** 200 declares no content, so the method takes no body. */
    public GetByInPathParametersStub code200() {
        return response(200, null);
    }

    @Override
    protected MappingBuilder toRequest() {
        MappingBuilder request = get(urlPathTemplate(PATH_TEMPLATE));
        pathParams.forEach(request::withPathParam);
        return request;
    }

    // ── NOTES ─────────────────────────────────────────────────────────────────
    //
    // 1. urlPathTemplate and withPathParam are WireMock 3.x only; 2.x has no
    //    equivalent and needs a hand-built regular expression. This is the first
    //    place where "WireMock 3 only" stops being a preference and becomes a
    //    hard constraint on the generated code.
    //
    // 2. Method names carry the parameter's location as a prefix — pathStringParam,
    //    queryStringParam, headerXRequestId. A specification is free to declare a
    //    path parameter and a query parameter of the same name; without the prefix
    //    that produces two methods with identical signatures and the class does not
    //    compile. Prefixing only on collision would keep names shorter, but then a
    //    name depends on the rest of the operation, so unrelated edits to the
    //    specification churn the generated code. Types derived from a parameter are
    //    prefixed for the same reason: see QueryEnumParam.
    //
    // 3. Nothing here builds the response. Status, body, media type, serialisation
    //    and code(int) live in AbstractStub, which is why toRequest returns a bare
    //    matcher. Only the typed per-code methods are generated, and each is a
    //    single delegation to response(status, body).
    // ──────────────────────────────────────────────────────────────────────────
}
