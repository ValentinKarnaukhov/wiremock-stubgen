package io.github.valentinkarnaukhov.wiremockstubgen.flatten;

import io.github.valentinkarnaukhov.wiremockstubgen.spec.TypeRef;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One method on a generated body scope, and the route through the schema it stands for.
 *
 * <p>The name is flattened and the path is not: {@code compositeDeepFieldDeepestField}
 * against {@code [composite, deepField, deepestField]}. Both are needed, and the path
 * cannot be recovered from the name — camel-joining is not reversible once a property
 * name contains a capital of its own.
 *
 * @param kind         what shape of method this becomes
 * @param name         the accessor name, already flattened, sanitised and escaped
 * @param path         wire names from the scope root to the property, never empty
 * @param type         for {@link Kind#VALUE} the property type; for {@link Kind#VALUE_LIST}
 *                     and {@link Kind#NESTED_LIST} the <em>element</em> type, because that
 *                     is what the signature is built from on both sides
 * @param targetSchema for {@link Kind#NESTED_LIST} the schema of the elements, otherwise
 *                     {@code null}
 * @param readOnly     whether the property this ends at is read-only; only the last hop of
 *                     the path decides how the value is written
 */
public record Accessor(Kind kind, String name, List<String> path, TypeRef type, String targetSchema,
                       boolean readOnly) {

    public enum Kind {
        /** A single value: primitive, enum, map, or an object whose schema is not in the catalogue. */
        VALUE,
        /**
         * An array of anything that is not a described object. A leaf, because there is
         * nothing inside an element to reach for, which is why the two sides may disagree
         * about the signature.
         */
        VALUE_LIST,
        /** An array of described objects: hands out the scope named by {@link #targetSchema}. */
        NESTED_LIST
    }

    public Accessor {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        path = List.copyOf(Objects.requireNonNull(path, "path"));
        if (path.isEmpty()) {
            throw new IllegalArgumentException("accessor path must not be empty");
        }
        if ((kind == Kind.NESTED_LIST) != (targetSchema != null)) {
            throw new IllegalArgumentException("targetSchema is required exactly for NESTED_LIST, got " + kind);
        }
    }

    public Optional<String> targetSchemaIfPresent() {
        return Optional.ofNullable(targetSchema);
    }

    /** How many property hops this accessor reaches through, counted inside its own scope. */
    public int depth() {
        return path.size();
    }
}
