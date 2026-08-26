package io.github.valentinkarnaukhov.wiremockstubgen.target;

/**
 * How generated stubs are laid out under the target package.
 *
 * <p>One stub per operation is not in question — that is what a stub is. This decides
 * what goes around them.
 */
public enum Grouping {

    /**
     * A sub-package per tag, named after the tag with its separators removed:
     * {@code get-response-composite-list} becomes {@code …stubs.getresponsecompositelist}.
     * What the golden stubs do, and what a specification with forty operations needs if
     * its stubs are to be findable at all.
     */
    TAG,

    /** Every stub directly in the target package. Bearable for a small specification. */
    NONE
}
