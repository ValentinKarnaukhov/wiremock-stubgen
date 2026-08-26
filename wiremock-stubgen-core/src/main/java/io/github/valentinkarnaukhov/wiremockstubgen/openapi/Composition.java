package io.github.valentinkarnaukhov.wiremockstubgen.openapi;

/**
 * What to make of a schema written with {@code oneOf} or {@code anyOf}.
 *
 * <p>This exists because the answer is not ours to give. A stub names the model classes
 * openapi-generator writes, and for these two keywords the generator writes something
 * different depending on how it was invoked — so the shape a stub must agree with is
 * decided by a flag on the other tool, which this one cannot see. The models do not exist
 * yet when stubs are generated, so there is nothing to inspect; the only honest thing is
 * to be told.
 *
 * <p>Both readings were verified on openapi-generator 7.9.0.
 */
public enum Composition {

    /**
     * Fold the members into one flat property list, the way {@code allOf} is folded.
     *
     * <p>What the generator does by default. A {@code oneOf} of a single {@code $ref} with
     * a discriminator — the commonest form in practice — becomes an ordinary class holding
     * that member's properties, with ordinary setters, and a stub that took such a body
     * whole would be refusing accessors the generator was perfectly willing to give. Where
     * two members disagree about a property the generator keeps the last one silently, and
     * merging here does the same, because agreeing with it matters more than being right.
     *
     * <p>A composition of scalars needs no special case: merging finds no properties in it
     * and it falls through to being taken whole, which is also what the generator does —
     * it writes the class with no fields at all.
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
