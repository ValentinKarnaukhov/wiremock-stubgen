package com.example.stubs.getbyparametersinpath;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import io.github.valentinkarnaukhov.stubgen.runtime.AbstractStub;
import io.github.valentinkarnaukhov.stubgen.runtime.StubTarget;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
    private int status = 200;

    public GetByInPathParametersStub(StubTarget target) {
        super(target);
    }

    public GetByInPathParametersStub stringParam(String value) {
        pathParams.put("stringParam", equalTo(value));
        return self();
    }

    public GetByInPathParametersStub longParam(Long value) {
        pathParams.put("longParam", equalTo(String.valueOf(value)));
        return self();
    }

    public GetByInPathParametersStub code200() {
        this.status = 200;
        return self();
    }

    public GetByInPathParametersStub code(int status) {
        this.status = status;
        return self();
    }

    @Override
    protected MappingBuilder toMappingBuilder() {
        MappingBuilder mappingBuilder = get(urlPathTemplate(PATH_TEMPLATE));
        pathParams.forEach(mappingBuilder::withPathParam);
        return mappingBuilder.willReturn(aResponse().withStatus(status));
    }

    // ── NOTES ─────────────────────────────────────────────────────────────────
    //
    // 1. urlPathTemplate and withPathParam are WireMock 3.x only; 2.x has no
    //    equivalent and needs a hand-built regular expression. This is the first
    //    place where "WireMock 3 only" stops being a preference and becomes a
    //    hard constraint on the generated code.
    //
    // 2. Method names carry no prefix. A specification with a path parameter and
    //    a query parameter of the same name would then produce two methods with
    //    identical signatures and the class would not compile. Either prefix
    //    everything (pathStringParam, queryStringParam — verbose but predictable)
    //    or prefix only on collision (readable, but the name then depends on the
    //    rest of the operation, so generated code churns when the spec changes).
    //    Undecided.
    // ──────────────────────────────────────────────────────────────────────────
}
