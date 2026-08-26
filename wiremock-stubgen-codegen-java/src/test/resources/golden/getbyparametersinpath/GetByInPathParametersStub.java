package com.example.stubs.getbyparametersinpath;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import io.github.valentinkarnaukhov.wiremockstubgen.runtime.AbstractStub;
import io.github.valentinkarnaukhov.wiremockstubgen.runtime.StubTarget;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;

/**
 * GOLDEN REFERENCE — hand-written specification of the generator's output.
 * Source: sample-api.yaml, operation getByInPathParameters.
 *
 * <p>Demonstrates: templated paths.
 */
public final class GetByInPathParametersStub extends AbstractStub<GetByInPathParametersStub> {

    /**
     * Kept as a template rather than assembled by concatenation, so that an unset
     * parameter degrades to "match any value in this segment" instead of producing
     * a broken URL.
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
    // 1. urlPathTemplate and withPathParam are WireMock 3.x only; 2.x needs a
    //    hand-built regular expression. "WireMock 3 only" is a hard constraint on
    //    the generated code, not a preference.
    //
    // 2. Method names carry the parameter's location as a prefix, because a
    //    specification may declare a path and a query parameter of the same name,
    //    which would otherwise be two identical signatures. Prefixing only on
    //    collision would make a name depend on the rest of the operation.
    //
    // 3. Nothing here builds the response: status, body, media type, serialisation
    //    and code(int) live in AbstractStub.
    // ──────────────────────────────────────────────────────────────────────────
}
