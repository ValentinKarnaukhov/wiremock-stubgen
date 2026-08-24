package io.github.valentinkarnaukhov.stubgen.spec;

import java.util.Objects;

/**
 * One property of an {@link ObjectSchema}.
 *
 * @param name     the wire name, exactly as written in the specification
 * @param type     the property type
 * @param required whether the schema lists this property as required
 */
public record Property(String name, TypeRef type, boolean required) {

    public Property {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
    }
}
