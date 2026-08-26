package io.github.valentinkarnaukhov.wiremockstubgen.spec;

import java.util.Objects;

/**
 * A single request parameter: path, query, header or cookie.
 *
 * @param name     the wire name, exactly as written in the specification
 * @param location where the parameter travels
 * @param type     the parameter type
 * @param required whether the specification marks it required
 */
public record Parameter(
        String name,
        ParameterLocation location,
        TypeRef type,
        boolean required) {

    public Parameter {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(type, "type");
    }
}
