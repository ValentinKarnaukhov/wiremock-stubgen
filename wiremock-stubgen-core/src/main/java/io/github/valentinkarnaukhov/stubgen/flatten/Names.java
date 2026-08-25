package io.github.valentinkarnaukhov.stubgen.flatten;

import java.util.List;
import java.util.Set;

/**
 * Turns a route through a schema into an accessor name.
 *
 * <p>This is the one place in {@code spec}-adjacent code that knows anything about
 * identifiers, and it knows the least it can: an accessor name comes out as ASCII
 * letters and digits starting with a lower-case letter, which every target language this
 * generator is likely to grow can accept. A target is free to re-case the result, but it
 * will not have to repair it.
 */
final class Names {

    private Names() {
    }

    /**
     * Camel-joins wire names into one accessor name.
     *
     * <p>Wire names are not identifiers. JSON permits {@code x-dashed}, {@code some.field}
     * and {@code 3d}, and specifications do use the first two, so the join splits each
     * segment on anything that is not a letter or a digit and capitalises what follows.
     * A name that would start with a digit gets an underscore, because that is the only
     * repair that does not invent a word.
     */
    static String join(List<String> segments) {
        StringBuilder result = new StringBuilder();
        for (String segment : segments) {
            for (String word : segment.split("[^A-Za-z0-9]+")) {
                if (word.isEmpty()) {
                    continue;
                }
                result.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
            }
        }
        if (result.isEmpty()) {
            return "_";
        }
        result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
        if (Character.isDigit(result.charAt(0))) {
            result.insert(0, '_');
        }
        return result.toString();
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
