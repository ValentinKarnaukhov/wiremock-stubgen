package io.github.valentinkarnaukhov.wiremockstubgen.flatten;

/**
 * Which end of an operation a body belongs to.
 *
 * <p>Both sides produce the same accessor names, but differ in three places the flattener
 * has to know about:
 *
 * <ul>
 *   <li>a list of primitives becomes {@code name(List<T>)} on a response, which replaces
 *       the list, and {@code name(T)} on a request, which asks whether some element
 *       equals the value;
 *   <li>a response distinguishes the position a schema sits at, because a list position
 *       needs a class that holds the list and appends to it. A request does not: the
 *       position lives in the JSONPath the matcher carries;
 *   <li>a response materialises objects along an accessor's path. A matcher has nothing
 *       to create.
 * </ul>
 */
public enum BodySide {
    RESPONSE,
    REQUEST;

    /**
     * Whether a schema needs a separate scope for each position it stands at.
     *
     * <p>Asked both when the flattener decides which scopes to build and when an emitter
     * looks up the scope a nested-list accessor hands out; if the two disagree the lookup
     * quietly finds nothing and a nested builder is lost without complaint.
     */
    public boolean distinguishesPosition() {
        return this == RESPONSE;
    }
}
