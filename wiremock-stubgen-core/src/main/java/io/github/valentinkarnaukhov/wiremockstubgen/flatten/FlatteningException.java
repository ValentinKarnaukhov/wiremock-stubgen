package io.github.valentinkarnaukhov.wiremockstubgen.flatten;

/**
 * Raised when a specification cannot be flattened into accessors that would compile.
 *
 * <p>Deliberately fatal rather than a warning: what it reports would otherwise become a
 * compile error inside generated code the user cannot edit. Failing here names the schema
 * and the two paths that disagree.
 */
public class FlatteningException extends RuntimeException {

    public FlatteningException(String message) {
        super(message);
    }
}
