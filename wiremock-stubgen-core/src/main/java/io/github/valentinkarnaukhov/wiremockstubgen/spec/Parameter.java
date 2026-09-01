package io.github.valentinkarnaukhov.wiremockstubgen.spec;

import java.util.Objects;

/**
 * A single request parameter: path, query, header or cookie.
 *
 * @param name             the wire name, exactly as written in the specification
 * @param location         where the parameter travels
 * @param type             the parameter type
 * @param collectionFormat how several values are written, {@link CollectionFormat#NONE}
 *                         unless the type is an array
 */
public record Parameter(
        String name,
        ParameterLocation location,
        TypeRef type,
        CollectionFormat collectionFormat) {

    public Parameter {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(collectionFormat, "collectionFormat");
        if (collectionFormat.isCollection() != (type.kind() == TypeRef.Kind.ARRAY)) {
            throw new IllegalArgumentException(
                    "parameter " + name + " is " + type.kind() + " but its collection format is "
                            + collectionFormat);
        }
    }
}
