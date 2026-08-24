package com.example.stubs.multipleoperations;

import io.github.valentinkarnaukhov.stubgen.runtime.StubTarget;

/**
 * GOLDEN REFERENCE — hand-written specification of the generator's output.
 * Source: sample-api.yaml, tag multiple-operations.
 *
 * <p>The case the facade exists for: one tag, several operations. Each operation
 * stays a separate small class, while the facade keeps them discoverable through
 * a single import.
 */
public final class MultipleOperationsStubs {

    private MultipleOperationsStubs() {
    }

    public static GetMultipleOperationsFirstStub getMultipleOperationsFirst(StubTarget target) {
        return new GetMultipleOperationsFirstStub(target);
    }

    public static GetMultipleOperationsSecondStub getMultipleOperationsSecond(StubTarget target) {
        return new GetMultipleOperationsSecondStub(target);
    }
}
