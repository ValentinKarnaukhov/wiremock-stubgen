package com.example.stubs.getbyparametersinquery;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import io.github.valentinkarnaukhov.wiremockstubgen.runtime.AbstractStub;
import io.github.valentinkarnaukhov.wiremockstubgen.runtime.StubTarget;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * GOLDEN REFERENCE — hand-written specification of the generator's output.
 * Source: sample-api.yaml, operation getByInQueryParameters.
 *
 * <p>Demonstrates: every primitive query parameter type, and why an inline enum
 * is not one of them.
 */
public final class GetByInQueryParametersStub extends AbstractStub<GetByInQueryParametersStub> {

    private static final String PATH = "/get/parameters/in-query";

    private final Map<String, StringValuePattern> queryParams = new LinkedHashMap<>();

    public GetByInQueryParametersStub(StubTarget target) {
        super(target);
    }

    // Typed setters. The type comes from the schema; the value always ends up as
    // a string, because that is what an HTTP query parameter is.

    public GetByInQueryParametersStub queryStringParam(String value) {
        queryParams.put("stringParam", equalTo(value));
        return self();
    }

    public GetByInQueryParametersStub queryIntegerParam(Integer value) {
        queryParams.put("integerParam", equalTo(String.valueOf(value)));
        return self();
    }

    public GetByInQueryParametersStub queryLongParam(Long value) {
        queryParams.put("longParam", equalTo(String.valueOf(value)));
        return self();
    }

    public GetByInQueryParametersStub queryBooleanParam(Boolean value) {
        queryParams.put("booleanParam", equalTo(String.valueOf(value)));
        return self();
    }

    public GetByInQueryParametersStub queryFloatParam(Float value) {
        queryParams.put("floatParam", equalTo(String.valueOf(value)));
        return self();
    }

    public GetByInQueryParametersStub queryDoubleParam(Double value) {
        queryParams.put("doubleParam", equalTo(String.valueOf(value)));
        return self();
    }

    /**
     * String, not a generated enum: openapi-generator renders an inline enum
     * parameter as a plain String in every Java client library, so there is no enum
     * type to reuse and inventing one would force the consumer to convert.
     */
    public GetByInQueryParametersStub queryEnumParam(String value) {
        queryParams.put("enumParam", equalTo(value));
        return self();
    }

    /** 200 declares no content, so the method takes no body. */
    public GetByInQueryParametersStub code200() {
        return response(200, null);
    }

    @Override
    protected MappingBuilder toRequest() {
        return get(urlPathEqualTo(PATH)).withQueryParams(queryParams);
    }

    // ── OPEN QUESTIONS ────────────────────────────────────────────────────────
    //
    // 1. No pattern overloads yet. Every setter matches on equality, so anything
    //    else is only reachable through customize(), which puts the parameter name
    //    back as a string literal and loses the rename check. Deferred: an overload
    //    per parameter doubles the method count, and offering it for strings alone
    //    is arbitrary.
    //
    // 2. Enum parameters are Strings. openapi-generator 7.9.0 produces no type at
    //    all for an inline enum in a parameter. An enum inside a SCHEMA is
    //    different: it is generated and nested in the model (ErrorBody.EnumFieldEnum)
    //    and we reference that. The rule is "no types we would be the only ones to
    //    have". The cost: nothing stops an invalid value.
    //
    // 3. Optional versus required. Absence is treated as "do not match on it".
    //    Enforcing required at mock() time would be a runtime error, which is what
    //    this project exists to avoid; a constructor argument enforces it at
    //    compile time but is unusable past a few parameters.
    // ──────────────────────────────────────────────────────────────────────────
}
