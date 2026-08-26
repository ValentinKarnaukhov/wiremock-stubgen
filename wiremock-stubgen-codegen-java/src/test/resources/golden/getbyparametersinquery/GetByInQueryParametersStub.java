package com.example.stubs.getbyparametersinquery;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import io.github.valentinkarnaukhov.stubgen.runtime.AbstractStub;
import io.github.valentinkarnaukhov.stubgen.runtime.StubTarget;

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
     * String, not a generated enum. openapi-generator renders an inline enum
     * parameter as a plain String in every Java client library, so there is no
     * enum type to reuse — and inventing one here would make the consumer convert
     * between our type and the String their own client hands them.
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
    // 1. No pattern overloads, for now. Every setter matches on equality, so
    //    anything else — a regular expression, a prefix, "absent", "any value" —
    //    is only reachable through customize():
    //
    //        .customize(b -> b.withQueryParam("stringParam", matching("ORD-\\d+")))
    //
    //    That is a real loss, and not only of convenience: the parameter name is
    //    back as a string literal, so a rename in the specification stops being a
    //    compile error. Keeping the name in one place is most of the reason this
    //    generator exists.
    //
    //    Deferred rather than decided. An overload per parameter doubles the
    //    method count — fourteen methods here for seven parameters — and offering
    //    it for strings alone, as an earlier draft did, is arbitrary: matching
    //    "\\d{3}" against an integer parameter is just as reasonable. Revisit once
    //    there is evidence of how often patterns are actually wanted.
    //
    // 3. Enum parameters are Strings, deliberately. Verified against
    //    openapi-generator 7.9.0: an inline enum in a parameter produces no type
    //    at all — resttemplate, webclient, native and okhttp-gson all render it as
    //    String. An enum inside a SCHEMA is different: it is generated, nested in
    //    the model (ErrorBody.EnumFieldEnum), and we reference that rather than
    //    duplicate it. So the rule is not "no enums", it is "no types we would be
    //    the only ones to have".
    //
    //    The cost is real: nothing stops an invalid value, and the specification's
    //    permitted values survive only as documentation. Revisit if it bites.
    //
    // 2. Optional versus required. Every parameter here is required:false, and
    //    the generated code treats absence as "do not match on it". A required
    //    parameter arguably deserves enforcement — but failing at mock() time is
    //    a runtime error, which is exactly what this project exists to avoid. A
    //    constructor argument would enforce it at compile time, at the cost of an
    //    unusable signature once there are five required parameters.
    // ──────────────────────────────────────────────────────────────────────────
}
