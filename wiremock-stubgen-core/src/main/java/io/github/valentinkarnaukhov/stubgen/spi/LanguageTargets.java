package io.github.valentinkarnaukhov.stubgen.spi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Locates {@link LanguageTarget} implementations on the classpath.
 */
public final class LanguageTargets {

    private LanguageTargets() {
    }

    /**
     * All targets visible to the given class loader, ordered by {@link LanguageTarget#id()}.
     */
    public static List<LanguageTarget> available(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        List<LanguageTarget> targets = new ArrayList<>();
        ServiceLoader.load(LanguageTarget.class, classLoader).forEach(targets::add);
        targets.sort(Comparator.comparing(LanguageTarget::id));
        return List.copyOf(targets);
    }

    public static List<LanguageTarget> available() {
        return available(Thread.currentThread().getContextClassLoader());
    }

    public static Optional<LanguageTarget> find(String id, ClassLoader classLoader) {
        Objects.requireNonNull(id, "id");
        return available(classLoader).stream().filter(t -> t.id().equals(id)).findFirst();
    }

    /**
     * Looks up a target, failing with a message that lists what is actually available —
     * the common case is a missing dependency rather than a typo.
     */
    public static LanguageTarget require(String id, ClassLoader classLoader) {
        return find(id, classLoader).orElseThrow(() -> new IllegalArgumentException(
                "No language target with id '" + id + "' on the classpath. Available: "
                        + available(classLoader).stream().map(LanguageTarget::id).toList()));
    }
}
