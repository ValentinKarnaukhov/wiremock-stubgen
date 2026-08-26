package io.github.valentinkarnaukhov.wiremockstubgen.target;

/** How generated stubs are laid out under the target package. */
public enum Grouping {

    /**
     * A sub-package per tag, named after the tag with its separators removed:
     * {@code get-response-composite-list} becomes {@code …stubs.getresponsecompositelist}.
     */
    TAG,

    /** Every stub directly in the target package. Bearable for a small specification. */
    NONE
}
