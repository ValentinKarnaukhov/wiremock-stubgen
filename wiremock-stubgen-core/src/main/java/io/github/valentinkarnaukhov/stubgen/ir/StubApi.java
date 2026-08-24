package io.github.valentinkarnaukhov.stubgen.ir;

import java.util.List;
import java.util.Objects;

/**
 * A whole API as understood by the generator: a flat list of operations plus the
 * spec-level title used to derive default naming.
 *
 * <p>This type — and everything else under {@code ir} — is deliberately free of any
 * target-language concepts. It describes what the specification says, not what any
 * particular language should emit.
 */
public record StubApi(String title, List<Operation> operations) {

    public StubApi {
        Objects.requireNonNull(title, "title");
        operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
    }
}
