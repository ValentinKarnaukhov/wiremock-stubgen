package io.github.valentinkarnaukhov.wiremockstubgen.flatten;

import io.github.valentinkarnaukhov.wiremockstubgen.naming.Identifiers;

import java.util.List;
import java.util.Set;

/**
 * Turns a route through a schema into an accessor name.
 *
 * <p>The shaping of wire names into identifiers lives in {@link Identifiers}, shared with
 * everything else that has to do it. What is left here is the part specific to
 * flattening: getting out of the way of the methods a generated scope inherits.
 */
final class Names {

    private Names() {
    }

    /**
     * Camel-joins wire names into one accessor name.
     */
    static String join(List<String> segments) {
        return Identifiers.camelJoin(segments);
    }

    /**
     * Moves a name out of the way of the runtime base class it would land on.
     *
     * <p>Reserved here means a method the generated scope inherits, not a keyword of the
     * language: a property named {@code exit} or {@code addNew} is what breaks, and
     * {@code class} never reaches this point because it is flattened as a wire name.
     * Verified with javac, the damage depends on arity — a one-argument {@code exit(String)}
     * is a legal overload and merely reads badly, while a zero-argument transition
     * accessor is {@code cannot override exit() in Scope, overridden method is final}.
     *
     * <p>Escaping regardless of arity is the deliberate choice. Renaming only the fatal
     * case would make a name depend on whether the property happens to be a list, so the
     * same property in two schemas would surface under two different names.
     *
     * <p>The underscore can itself collide, with a property genuinely named {@code _exit}.
     * That is left to the collision check rather than special-cased, so there is one
     * report for one kind of problem.
     */
    static String escape(String name, Set<String> reserved) {
        return reserved.contains(name) ? "_" + name : name;
    }
}
