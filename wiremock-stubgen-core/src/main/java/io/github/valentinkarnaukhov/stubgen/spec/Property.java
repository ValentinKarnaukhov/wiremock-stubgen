package io.github.valentinkarnaukhov.stubgen.spec;

import java.util.Objects;

/**
 * One property of an {@link ObjectSchema}.
 *
 * @param name     the wire name, exactly as written in the specification
 * @param type     the property type
 * @param required whether the schema lists this property as required
 * @param readOnly whether the specification says only a server ever sends this. It
 *                 belongs here rather than being dropped during reading because a target
 *                 has to know: a model generator gives a read-only property no setter, so
 *                 the code that writes to it cannot look like the code that writes to any
 *                 other property.
 */
public record Property(String name, TypeRef type, boolean required, boolean readOnly) {

    public Property {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
    }
}
