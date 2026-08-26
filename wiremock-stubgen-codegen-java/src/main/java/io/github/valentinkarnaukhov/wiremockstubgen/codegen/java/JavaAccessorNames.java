package io.github.valentinkarnaukhov.wiremockstubgen.codegen.java;

import io.github.valentinkarnaukhov.wiremockstubgen.naming.Identifiers;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * What Java calls the accessor reached by a route through a schema.
 *
 * <p>Camel-joined, then moved out of the way of the methods the generated scope inherits.
 * Reserved means a method, not a language keyword: a property named {@code exit} or
 * {@code addNew} is what breaks.
 *
 * <p>Escaping happens regardless of arity — only a zero-argument clash is actually fatal,
 * but renaming only that case would make a name depend on whether the property is a list,
 * so the same property in two schemas would surface under two names.
 *
 * <p>The underscore can itself collide with a property genuinely named {@code _exit}; that
 * is left to the flattener's collision check rather than special-cased.
 */
final class JavaAccessorNames implements Function<List<String>, String> {

    private final Set<String> reserved;

    JavaAccessorNames(Set<String> reserved) {
        this.reserved = Set.copyOf(reserved);
    }

    @Override
    public String apply(List<String> path) {
        String name = Identifiers.camelJoin(path);
        return reserved.contains(name) ? "_" + name : name;
    }
}
