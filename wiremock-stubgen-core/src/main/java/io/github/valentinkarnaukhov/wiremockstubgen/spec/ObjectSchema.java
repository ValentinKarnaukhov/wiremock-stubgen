package io.github.valentinkarnaukhov.wiremockstubgen.spec;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A named object schema and the properties it declares.
 *
 * <p>This description used to carry only the <em>name</em> of a schema, which was enough while a
 * stub's body was a single typed argument. It is not enough now: a generated stub
 * declares a builder per schema with a method per leaf, so the emitter has to know what
 * is inside. This is that catalogue.
 *
 * <p>Only schemas with properties appear here. An array or an enum declared at the top
 * of {@code components/schemas} is described by the {@link TypeRef} that mentions it,
 * not by an entry of its own.
 *
 * @param name       the component schema name, exactly as written in the specification
 * @param properties declared properties, in specification order
 */
public record ObjectSchema(String name, List<Property> properties) {

    public ObjectSchema {
        Objects.requireNonNull(name, "name");
        properties = List.copyOf(Objects.requireNonNull(properties, "properties"));
    }

    public Optional<Property> property(String propertyName) {
        return properties.stream().filter(p -> p.name().equals(propertyName)).findFirst();
    }
}
