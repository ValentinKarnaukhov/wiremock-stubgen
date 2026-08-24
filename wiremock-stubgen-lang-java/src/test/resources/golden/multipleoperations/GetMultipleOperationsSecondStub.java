package com.example.stubs.multipleoperations;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import io.github.valentinkarnaukhov.stubgen.runtime.AbstractStub;
import io.github.valentinkarnaukhov.stubgen.runtime.StubTarget;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * GOLDEN REFERENCE — hand-written specification of the generator's output.
 * Source: sample-api.yaml, operation getMultipleOperationsSecond.
 */
public final class GetMultipleOperationsSecondStub extends AbstractStub<GetMultipleOperationsSecondStub> {

    private static final String PATH = "/get/multiple-operations/second";

    private int status = 200;

    public GetMultipleOperationsSecondStub(StubTarget target) {
        super(target);
    }

    public GetMultipleOperationsSecondStub code200() {
        this.status = 200;
        return self();
    }

    public GetMultipleOperationsSecondStub code(int status) {
        this.status = status;
        return self();
    }

    @Override
    protected MappingBuilder toMappingBuilder() {
        return get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(status));
    }
}
