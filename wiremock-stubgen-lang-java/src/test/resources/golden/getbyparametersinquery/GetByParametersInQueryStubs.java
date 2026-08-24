package com.example.stubs.getbyparametersinquery;

import io.github.valentinkarnaukhov.stubgen.runtime.StubTarget;

/**
 * GOLDEN REFERENCE — hand-written specification of the generator's output.
 * Source: sample-api.yaml, tag get-by-parameters-in-query.
 *
 * <p>Facade over the operations of one tag. Exists purely for discovery: one
 * import and autocompletion lists everything the tag offers. Using the operation
 * classes directly is equivalent.
 */
public final class GetByParametersInQueryStubs {

    private GetByParametersInQueryStubs() {
    }

    public static GetByInQueryParametersStub getByInQueryParameters(StubTarget target) {
        return new GetByInQueryParametersStub(target);
    }
}
