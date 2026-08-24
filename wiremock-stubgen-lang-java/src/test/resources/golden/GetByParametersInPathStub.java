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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;

/**
 * GOLDEN REFERENCE — hand-written specification of the generator's output.
 * Source: sample-api.yaml, tag get-by-parameters-in-path.
 *
 * <p>Demonstrates: templated paths. The predecessor project declared a urlPattern
 * field and never assigned it, so paths with parameters did not work at all.
 */
public final class GetByParametersInPathStub {

    private final StubTarget target;

    public GetByParametersInPathStub(StubTarget target) {
        this.target = target;
    }

    public GetByInPathParameters getByInPathParameters() {
        return new GetByInPathParameters(target);
    }

    public static final class GetByInPathParameters extends AbstractStub<GetByInPathParameters> {

        /**
         * Kept as a template rather than assembled by concatenation, so that an
         * unset parameter degrades to "match any value in this segment" instead of
         * producing a broken URL.
         */
        private static final String PATH_TEMPLATE = "/get/parameters/in-path/{stringParam}/{longParam}";

        private final Map<String, StringValuePattern> pathParams = new LinkedHashMap<>();
        private int status = 200;

        public GetByInPathParameters(StubTarget target) {
            super(target);
        }

        public GetByInPathParameters stringParam(String value) {
            pathParams.put("stringParam", equalTo(value));
            return self();
        }

        public GetByInPathParameters longParam(Long value) {
            pathParams.put("longParam", equalTo(String.valueOf(value)));
            return self();
        }

        public GetByInPathParameters code200() {
            this.status = 200;
            return self();
        }

        public GetByInPathParameters code(int status) {
            this.status = status;
            return self();
        }

        @Override
        protected MappingBuilder toMappingBuilder() {
            MappingBuilder mappingBuilder = get(urlPathTemplate(PATH_TEMPLATE));
            pathParams.forEach(mappingBuilder::withPathParam);
            return mappingBuilder.willReturn(aResponse().withStatus(status));
        }
    }

    // ── NOTES ─────────────────────────────────────────────────────────────────
    //
    // 1. urlPathTemplate and withPathParam are WireMock 3.x only; 2.x has no
    //    equivalent and needs a hand-built regular expression. This is the first
    //    place where "WireMock 3 only" stops being a preference and becomes a
    //    hard constraint on the generated code.
    //
    // 2. Both parameters are required:true, yet nothing forces the caller to set
    //    them. An unset one silently widens the match to any value. That is
    //    convenient and occasionally right, but it is exactly the class of quiet
    //    mismatch this project exists to eliminate. Alternatives: demand them in
    //    the constructor, or fail in toMappingBuilder. Undecided.
    //
    // 3. Method names carry no prefix. A specification with a path parameter and
    //    a query parameter of the same name would then produce two methods with
    //    identical signatures and the class would not compile. Either prefix
    //    everything (pathStringParam, queryStringParam — verbose but predictable)
    //    or prefix only on collision (readable but the name depends on the rest
    //    of the operation, which makes generated code churn). Undecided.
    // ──────────────────────────────────────────────────────────────────────────
}
