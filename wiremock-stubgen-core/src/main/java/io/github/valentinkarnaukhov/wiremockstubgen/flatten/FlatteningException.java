package io.github.valentinkarnaukhov.wiremockstubgen.flatten;

/**
 * Raised when a specification cannot be flattened into accessors that would compile.
 *
 * <p>Deliberately fatal rather than a warning. What this exception reports ends as a
 * compile error inside generated code, and a compile error in a file the user did not
 * write and cannot edit is the worst failure this generator can produce. Failing here
 * names the schema and the two paths that disagree; failing later names a line number in
 * generated output and leaves the reader to work backwards to the specification.
 */
public class FlatteningException extends RuntimeException {

    public FlatteningException(String message) {
        super(message);
    }
}
