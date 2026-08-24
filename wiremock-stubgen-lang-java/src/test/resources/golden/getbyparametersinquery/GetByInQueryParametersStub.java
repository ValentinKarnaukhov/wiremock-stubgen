package com.example.stubs.getbyparametersinquery;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import io.github.valentinkarnaukhov.stubgen.runtime.AbstractStub;
import io.github.valentinkarnaukhov.stubgen.runtime.StubTarget;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * GOLDEN REFERENCE — hand-written specification of the generator's output.
 * Source: sample-api.yaml, operation getByInQueryParameters.
 *
 * <p>Demonstrates: every primitive query parameter type, plus an inline enum.
 */
public final class GetByInQueryParametersStub extends AbstractStub<GetByInQueryParametersStub> {

    private static final String PATH = "/get/parameters/in-query";

    private final Map<String, StringValuePattern> queryParams = new LinkedHashMap<>();
    private int status = 200;

    public GetByInQueryParametersStub(StubTarget target) {
        super(target);
    }

    // Typed setters. The type comes from the schema; the value always ends up as
    // a string, because that is what an HTTP query parameter is.

    public GetByInQueryParametersStub stringParam(String value) {
        queryParams.put("stringParam", equalTo(value));
        return self();
    }

    public GetByInQueryParametersStub integerParam(Integer value) {
        queryParams.put("integerParam", equalTo(String.valueOf(value)));
        return self();
    }

    public GetByInQueryParametersStub longParam(Long value) {
        queryParams.put("longParam", equalTo(String.valueOf(value)));
        return self();
    }

    public GetByInQueryParametersStub booleanParam(Boolean value) {
        queryParams.put("booleanParam", equalTo(String.valueOf(value)));
        return self();
    }

    public GetByInQueryParametersStub floatParam(Float value) {
        queryParams.put("floatParam", equalTo(String.valueOf(value)));
        return self();
    }

    public GetByInQueryParametersStub doubleParam(Double value) {
        queryParams.put("doubleParam", equalTo(String.valueOf(value)));
        return self();
    }

    public GetByInQueryParametersStub enumParam(EnumParam value) {
        queryParams.put("enumParam", equalTo(value.value()));
        return self();
    }

    /**
     * Pattern overload. Without it the typed API is strictly weaker than raw
     * WireMock: matching any value, a prefix or a regular expression is routine.
     */
    public GetByInQueryParametersStub stringParam(StringValuePattern pattern) {
        queryParams.put("stringParam", pattern);
        return self();
    }

    public GetByInQueryParametersStub code200() {
        this.status = 200;
        return self();
    }

    /** Status codes the specification does not declare, for negative testing. */
    public GetByInQueryParametersStub code(int status) {
        this.status = status;
        return self();
    }

    @Override
    protected MappingBuilder toMappingBuilder() {
        return get(urlPathEqualTo(PATH))
                .withQueryParams(queryParams)
                .willReturn(aResponse().withStatus(status));
    }

    /**
     * Inline enum from the specification. Nested, because it belongs to one
     * parameter of one operation and would collide at package level.
     *
     * <p>Nesting is used only here — for types that have no meaning outside the
     * operation. Operations themselves are separate top-level classes, so that a
     * tag with forty operations does not become one unreadable file.
     */
    public enum EnumParam {

        ENUM_VALUE1("EnumValue1"),
        ENUM_VALUE2("EnumValue2");

        private final String value;

        EnumParam(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    // ── OPEN QUESTIONS ────────────────────────────────────────────────────────
    //
    // 1. Pattern overloads double the method count. Written out for all seven
    //    parameters this class would be twice its size. Options: string
    //    parameters only, as here; all parameters; or none, relying on
    //    customize(). Undecided.
    //
    // 2. Optional versus required. Every parameter here is required:false, and
    //    the generated code treats absence as "do not match on it". A required
    //    parameter arguably deserves enforcement — but failing at mock() time is
    //    a runtime error, which is exactly what this project exists to avoid. A
    //    constructor argument would enforce it at compile time, at the cost of an
    //    unusable signature once there are five required parameters.
    // ──────────────────────────────────────────────────────────────────────────
}
