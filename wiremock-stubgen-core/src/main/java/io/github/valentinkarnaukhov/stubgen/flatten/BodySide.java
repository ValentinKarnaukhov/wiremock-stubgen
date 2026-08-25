package io.github.valentinkarnaukhov.stubgen.flatten;

/**
 * Which end of an operation a body belongs to.
 *
 * <p>The two sides read the same schema and produce the same accessor names — that
 * symmetry is the point — but they differ in three places, and the flattener has to know
 * which one it is working for:
 *
 * <ul>
 *   <li>a list of primitives becomes {@code name(List<T>)} on a response, which replaces
 *       the list, and {@code name(T)} on a request, which asks whether some element
 *       equals the value. Producing and matching are different actions;
 *   <li>a response distinguishes the position a schema sits at, because a list position
 *       needs a class that holds the list and appends to it. A request does not: the
 *       position lives in the JSONPath the matcher carries;
 *   <li>a response materialises objects along an accessor's path. A matcher has nothing
 *       to create.
 * </ul>
 */
public enum BodySide {
    RESPONSE,
    REQUEST
}
