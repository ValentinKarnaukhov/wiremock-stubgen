package io.github.valentinkarnaukhov.wiremockstubgen.flatten;

import io.github.valentinkarnaukhov.wiremockstubgen.naming.Identifiers;

import java.util.List;
import java.util.Set;

/**
 * Turns a route through a schema into an accessor name. The general shaping of wire names
 * into identifiers lives in {@link Identifiers}; what is left here is getting out of the
 * way of the methods a generated scope inherits.
 */
final class Names {

    private Names() {
    }

    /** Camel-joins wire names into one accessor name. */
    static String join(List<String> segments) {
        return Identifiers.camelJoin(segments);
    }

    /**
     * Moves a name out of the way of the runtime base class it would land on.
     *
     * <p>Reserved means a method the generated scope inherits, not a language keyword: a
     * property named {@code exit} or {@code addNew} is what breaks. Escaping happens
     * regardless of arity — only a zero-argument clash is actually fatal, but renaming
     * only that case would make a name depend on whether the property is a list, so the
     * same property in two schemas would surface under two names.
     *
     * <p>The underscore can itself collide with a property genuinely named {@code _exit};
     * that is left to the collision check rather than special-cased.
     */
    static String escape(String name, Set<String> reserved) {
        return reserved.contains(name) ? "_" + name : name;
    }
}
