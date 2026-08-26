package io.github.valentinkarnaukhov.stubgen.spec;

import java.util.Objects;
import java.util.Optional;

/**
 * A language-neutral reference to a type described by the specification.
 *
 * <p>Deliberately expressed in OpenAPI terms ({@code type}/{@code format}/schema name)
 * rather than in target-language terms. Mapping {@code string+date-time} onto
 * {@code OffsetDateTime} or {@code LocalDateTime} is a decision that belongs to a
 * language target, because it must agree with whatever model generator the user runs.
 *
 * @param kind        broad shape of the type
 * @param schemaName  component schema name for {@link Kind#OBJECT} and {@link Kind#ENUM}, otherwise {@code null}
 * @param openApiType the OpenAPI {@code type} keyword for {@link Kind#PRIMITIVE}, otherwise {@code null}
 * @param format      the OpenAPI {@code format} keyword, may be {@code null}
 * @param items       element type for {@link Kind#ARRAY} and value type for {@link Kind#MAP}, otherwise {@code null}
 * @param declaringSchema the schema a {@link Kind#ENUM} belongs to when it was written
 *                    inside that schema rather than declared on its own, otherwise
 *                    {@code null}. Kept apart from {@code schemaName} because how the two
 *                    are joined is a language's business: Java nests the enum inside the
 *                    model class, and another target may well flatten the two names into
 *                    one.
 */
public record TypeRef(
        Kind kind,
        String schemaName,
        String openApiType,
        String format,
        TypeRef items,
        String declaringSchema) {

    public enum Kind {
        PRIMITIVE,
        OBJECT,
        ENUM,
        ARRAY,
        MAP,
        /** The specification gave no usable schema (for example an empty response body). */
        UNKNOWN
    }

    public TypeRef {
        Objects.requireNonNull(kind, "kind");
    }

    public static TypeRef primitive(String openApiType, String format) {
        return new TypeRef(Kind.PRIMITIVE, null, Objects.requireNonNull(openApiType, "openApiType"), format, null, null);
    }

    public static TypeRef object(String schemaName) {
        return new TypeRef(Kind.OBJECT, Objects.requireNonNull(schemaName, "schemaName"), null, null, null, null);
    }

    public static TypeRef enumeration(String schemaName) {
        return new TypeRef(Kind.ENUM, Objects.requireNonNull(schemaName, "schemaName"), null, null, null, null);
    }

    /**
     * An enum written inside a schema rather than declared on its own.
     *
     * <p>It needs a name of its own because the model generator gives it one, and stubs
     * have to say the same name or the caller cannot pass the value at all.
     */
    public static TypeRef nestedEnumeration(String declaringSchema, String schemaName) {
        return new TypeRef(Kind.ENUM,
                Objects.requireNonNull(schemaName, "schemaName"),
                null, null, null,
                Objects.requireNonNull(declaringSchema, "declaringSchema"));
    }

    public static TypeRef array(TypeRef items) {
        return new TypeRef(Kind.ARRAY, null, null, null, Objects.requireNonNull(items, "items"), null);
    }

    public static TypeRef map(TypeRef values) {
        return new TypeRef(Kind.MAP, null, null, null, Objects.requireNonNull(values, "values"), null);
    }

    public static TypeRef unknown() {
        return new TypeRef(Kind.UNKNOWN, null, null, null, null, null);
    }

    public Optional<TypeRef> itemsIfPresent() {
        return Optional.ofNullable(items);
    }

    /** {@code true} when this type nests other named schemas and therefore can be exploded. */
    public boolean isExplodable() {
        return switch (kind) {
            case OBJECT -> true;
            case ARRAY, MAP -> items != null && items.isExplodable();
            default -> false;
        };
    }
}
