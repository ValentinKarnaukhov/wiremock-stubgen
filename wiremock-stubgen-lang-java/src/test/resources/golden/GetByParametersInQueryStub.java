package com.example.stubs;

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
 * Source: sample-api.yaml, tag get-by-parameters-in-query.
 *
 * <p>Demonstrates: every primitive query parameter type, plus an inline enum.
 */
public final class GetByParametersInQueryStub {

    private final StubTarget target;

    public GetByParametersInQueryStub(StubTarget target) {
        this.target = target;
    }

    public GetByInQueryParameters getByInQueryParameters() {
        return new GetByInQueryParameters(target);
    }

    public static final class GetByInQueryParameters extends AbstractStub<GetByInQueryParameters> {

        private static final String PATH = "/get/parameters/in-query";

        private final Map<String, StringValuePattern> queryParams = new LinkedHashMap<>();
        private int status = 200;

        public GetByInQueryParameters(StubTarget target) {
            super(target);
        }

        // Typed setters. The type comes from the schema; the value always ends up
        // as a string, because that is what an HTTP query parameter is.

        public GetByInQueryParameters stringParam(String value) {
            queryParams.put("stringParam", equalTo(value));
            return self();
        }

        public GetByInQueryParameters integerParam(Integer value) {
            queryParams.put("integerParam", equalTo(String.valueOf(value)));
            return self();
        }

        public GetByInQueryParameters longParam(Long value) {
            queryParams.put("longParam", equalTo(String.valueOf(value)));
            return self();
        }

        public GetByInQueryParameters booleanParam(Boolean value) {
            queryParams.put("booleanParam", equalTo(String.valueOf(value)));
            return self();
        }

        public GetByInQueryParameters floatParam(Float value) {
            queryParams.put("floatParam", equalTo(String.valueOf(value)));
            return self();
        }

        public GetByInQueryParameters doubleParam(Double value) {
            queryParams.put("doubleParam", equalTo(String.valueOf(value)));
            return self();
        }

        public GetByInQueryParameters enumParam(EnumParam value) {
            queryParams.put("enumParam", equalTo(value.value()));
            return self();
        }

        // Pattern overloads. Without these the typed API is strictly weaker than
        // raw WireMock: matching "any value", a prefix or a regex is routine.

        public GetByInQueryParameters stringParam(StringValuePattern pattern) {
            queryParams.put("stringParam", pattern);
            return self();
        }

        public GetByInQueryParameters code200() {
            this.status = 200;
            return self();
        }

        public GetByInQueryParameters code(int status) {
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
    }

    // ── OPEN QUESTIONS ────────────────────────────────────────────────────────
    //
    // 1. Pattern overloads double the method count. Written out for every one of
    //    the seven parameters this class would be twice its size. Options:
    //    generate them only for string parameters (as here), generate for all,
    //    or drop them and rely on customize(). Undecided.
    //
    // 2. Optional versus required. Every parameter here is required:false, and
    //    the generated code treats absence as "do not match on it". A required
    //    parameter arguably deserves enforcement — but failing at mock() time
    //    is a runtime error, which is exactly what this project exists to avoid.
    //    A constructor argument would enforce it at compile time, at the cost of
    //    an unusable signature once there are five required parameters.
    //
    // 3. Name collision. A parameter named "code" or "customize" would collide
    //    with the inherited API. The emitter needs an escaping rule.
    // ──────────────────────────────────────────────────────────────────────────
}
