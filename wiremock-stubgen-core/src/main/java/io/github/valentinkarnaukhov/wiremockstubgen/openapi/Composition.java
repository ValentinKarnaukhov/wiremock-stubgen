package io.github.valentinkarnaukhov.wiremockstubgen.openapi;

/**
 * What to make of a schema written with {@code oneOf} or {@code anyOf}.
 *
 * <p>The answer is not ours to give: openapi-generator writes a different shape depending
 * on how it was invoked, and the models do not exist yet when stubs are generated, so
 * there is nothing to inspect. Both readings were verified on openapi-generator 7.9.0.
 */
public enum Composition {

    /**
     * Fold the members into one flat property list, the way {@code allOf} is folded.
     *
     * <p>What the generator does by default: it writes an ordinary class holding the
     * members' properties, with ordinary setters. Where two members disagree about a
     * property the generator keeps the last one, and merging here does the same. A
     * composition of scalars needs no special case — merging finds no properties and the
     * body is taken whole, which is also what the generator produces.
     */
    MERGE,

    /**
     * Take bodies of such a type whole, offering no accessors into them.
     *
     * <p>Required with {@code useOneOfInterfaces=true}, under which the generator writes an
     * empty interface instead of a class. There are no setters to call, so merged accessors
     * would not compile.
     */
    OPAQUE
}
