package io.github.valentinkarnaukhov.wiremockstubgen.spec;

import java.util.Objects;

/**
 * One property of an {@link ObjectSchema}.
 *
 * @param name     the wire name, exactly as written in the specification
 * @param type     the property type
 * @param readOnly whether the specification says only a server ever sends this. A target
 *                 has to know: a model generator gives a read-only property no setter, so
 *                 writing to it cannot look like writing to any other property.
 */
public record Property(String name, TypeRef type, boolean readOnly) {

    public Property {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
    }
}
