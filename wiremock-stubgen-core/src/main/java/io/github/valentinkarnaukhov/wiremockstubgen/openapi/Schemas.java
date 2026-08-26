package io.github.valentinkarnaukhov.wiremockstubgen.openapi;

import io.github.valentinkarnaukhov.wiremockstubgen.naming.Identifiers;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.ObjectSchema;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.Property;
import io.github.valentinkarnaukhov.wiremockstubgen.spec.TypeRef;
import io.swagger.v3.oas.models.media.Schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The schemas of one document, resolved into something a stub can be built from.
 *
 * <p>Generated stubs reference the model classes openapi-generator writes, so this must
 * invent the same names and merge compositions the same way. Two rules follow from that
 * and are not ours to choose:
 *
 * <ul>
 *   <li><b>Composition is flattened.</b> {@code allOf} of two references produces one
 *       class holding every property of both, in member order — not a subclass. Members
 *       may themselves be compositions.</li>
 *   <li><b>An inline object is named after where it sits.</b> Property {@code inner} of
 *       schema {@code Nested} becomes {@code NestedInner}; the response body of
 *       {@code postInlineRoot} at 200 becomes {@code PostInlineRoot200Response}, its
 *       property {@code origin} becomes {@code PostInlineRoot200ResponseOrigin}, and the
 *       element type of an array becomes that name plus {@code Inner}.</li>
 * </ul>
 */
final class Schemas {

    private final Map<String, Schema> declared;

    private final Map<String, ObjectSchema> resolved = new LinkedHashMap<>();

    /** The name already given to an inline object of each shape. See {@link #inline}. */
    private final Map<String, String> inlineNames = new LinkedHashMap<>();

    private final Consumer<String> warnings;

    private final Composition composition;

    Schemas(Map<String, Schema> declared, Consumer<String> warnings, Composition composition) {
        this.declared = Objects.requireNonNull(declared, "declared");
        this.warnings = Objects.requireNonNull(warnings, "warnings");
        this.composition = Objects.requireNonNull(composition, "composition");
    }

    /**
     * Resolves every schema the document declares.
     */
    void readDeclared() {
        declared.forEach(this::register);
    }

    Map<String, ObjectSchema> resolved() {
        return Map.copyOf(resolved);
    }

    /**
     * Turns a schema into a type reference.
     *
     * @param nameHint what an inline object here would be called. Null where there is no
     *                 sensible name, in which case an inline object is reported and dropped
     *                 rather than named badly.
     */
    TypeRef typeOf(Schema<?> schema, String nameHint) {
        return typeOf(schema, nameHint, null);
    }

    /**
     * @param objectName what an inline object here would be called
     * @param enumName   what an inline enum here would be called, as
     *                   {@code Owner.MemberEnum}. Unlike the object name this does not
     *                   change as an array is entered: an enum written as the items of
     *                   property {@code many} is called {@code ManyEnum}, not
     *                   {@code ManyInnerEnum}.
     */
    private TypeRef typeOf(Schema<?> schema, String objectName, String enumName) {
        if (schema == null) {
            return TypeRef.unknown();
        }
        if (schema.get$ref() != null) {
            return referenced(schema.get$ref(), new LinkedHashSet<>());
        }
        if ("array".equals(schema.getType()) || schema.getItems() != null) {
            return TypeRef.array(typeOf(schema.getItems(), element(objectName), enumName));
        }
        if (schema.getAdditionalProperties() instanceof Schema<?> values) {
            return TypeRef.map(typeOf(values, objectName, enumName));
        }
        String alias = alternativeAlias(schema);
        if (alias != null) {
            return referenced(alias, new LinkedHashSet<>());
        }
        if (schema.getProperties() != null || schema.getAllOf() != null || merging(schema)) {
            // An allOf wrapping one reference and adding nothing of its own is not a type,
            // it is that reference: specifications write it to hang a description or a
            // readOnly flag on a $ref. openapi-generator resolves it to the referenced
            // class and writes no new one.
            List<Schema> members = schema.getAllOf();
            if (schema.getProperties() == null && members != null && members.size() == 1
                    && members.get(0).get$ref() != null) {
                return referenced(members.get(0).get$ref(), new LinkedHashSet<>());
            }
            return inline(schema, objectName);
        }
        if (schema.getEnum() != null && enumName != null) {
            int separator = enumName.indexOf('.');
            return TypeRef.nestedEnumeration(
                    enumName.substring(0, separator), enumName.substring(separator + 1));
        }
        if (schema.getOneOf() != null || schema.getAnyOf() != null) {
            // Only reachable under OPAQUE; merging sends these to inline() above.
            warnings.accept(describe(objectName) + " is composed with oneOf or anyOf, which"
                    + " is being read as opaque; the stub will take the body whole and offer"
                    + " no accessors into it");
            return TypeRef.unknown();
        }
        if (schema.getType() == null || "object".equals(schema.getType())) {
            // An object with nothing in it describes no shape, and openapi-generator
            // passes it through as Object rather than writing a class for it.
            return TypeRef.unknown();
        }
        // An inline enum in a position that gives it no name — a parameter, or the whole
        // body — is read as its base type: nothing generates a class for one of those.
        return TypeRef.primitive(schema.getType(), schema.getFormat());
    }

    /**
     * Resolves a reference to what the referenced schema actually is.
     *
     * <p>openapi-generator writes a model only for schemas with a shape of their own —
     * properties, a composition, or an enum. A name given to a boolean, or to an array of
     * something else, is an alias: the generator inlines {@code Boolean} or
     * {@code List<Thing>} into signatures and no class by that name exists. Naming the
     * class here anyway is how a generated stub stops compiling.
     *
     * @param visiting guards a chain of aliases that refers back into itself.
     */
    private TypeRef referenced(String ref, Set<String> visiting) {
        String name = referencedName(ref);
        if (name == null) {
            warnings.accept("Ignoring the unsupported reference " + ref);
            return TypeRef.unknown();
        }
        Schema<?> target = declared.get(name);
        if (target == null) {
            warnings.accept("Ignoring the dangling reference " + ref);
            return TypeRef.unknown();
        }
        if (target.getEnum() != null) {
            return TypeRef.enumeration(name);
        }
        String alias = alternativeAlias(target);
        if (alias != null) {
            if (!visiting.add(name)) {
                warnings.accept("Schema " + name + " is an alias for itself; ignoring it");
                return TypeRef.unknown();
            }
            return referenced(alias, visiting);
        }
        if (hasShapeOfItsOwn(target)) {
            return TypeRef.object(name);
        }
        if (target.get$ref() != null) {
            if (!visiting.add(name)) {
                warnings.accept("Schema " + name + " is an alias for itself; ignoring it");
                return TypeRef.unknown();
            }
            return referenced(target.get$ref(), visiting);
        }
        return typeOf(target, null, null);
    }

    private boolean hasShapeOfItsOwn(Schema<?> schema) {
        return schema.getProperties() != null
                || schema.getAllOf() != null
                || schema.getOneOf() != null
                || schema.getAnyOf() != null;
    }

    /**
     * Whether this schema's {@code oneOf} or {@code anyOf} members are to be folded in.
     */
    private boolean merging(Schema<?> schema) {
        return composition == Composition.MERGE
                && (schema.getOneOf() != null || schema.getAnyOf() != null);
    }

    /**
     * The reference a {@code oneOf} or {@code anyOf} of exactly one member stands for.
     *
     * <p>The generator versions disagree: 7.9.0 writes a flattened class of its own, 7.24.0
     * resolves it to the member. Reading it as the member is the only reading that compiles
     * under both, since 7.9.0 emits the member class as well.
     *
     * <p>True of both readings. Under {@code useOneOfInterfaces=true}, which is what the
     * opaque reading exists for, the generator writes no interface for a composition
     * naming a single alternative either, so the composition's own name refers to nothing.
     *
     * @return null if this is not such a composition
     */
    private String alternativeAlias(Schema<?> schema) {
        boolean composed = schema.getOneOf() != null || schema.getAnyOf() != null;
        if (!composed || schema.getProperties() != null || schema.getAllOf() != null) {
            return null;
        }
        List<Schema> members = alternatives(schema);
        return members.size() == 1 ? members.get(0).get$ref() : null;
    }

    static String referencedName(String ref) {
        if (!ref.startsWith("#/components/schemas/") && !ref.startsWith("#/definitions/")) {
            return null;
        }
        return ref.substring(ref.lastIndexOf('/') + 1);
    }

    /**
     * Registers an object the document wrote out in place rather than declaring.
     *
     * <p>openapi-generator will generate a model class for this same inline schema, so the
     * name must match what it picks; inventing one of our own is how the two drift apart.
     *
     * <p>The generator writes one class per <em>shape</em>, not per place: two inline
     * objects written out identically, whether in one schema or in schemas far apart,
     * become a single class named after whichever came first. Naming the second one after
     * where it sits would name a class the generator never wrote. Identical means written
     * identically — a description, an order of properties or a {@code required} entry is
     * enough to tell two shapes apart.
     */
    private TypeRef inline(Schema<?> schema, String nameHint) {
        if (nameHint == null || nameHint.isBlank()) {
            warnings.accept("Ignoring an inline object in a position that gives it no name");
            return TypeRef.unknown();
        }
        String shape = String.valueOf(schema);
        String taken = inlineNames.get(shape);
        if (taken != null) {
            return TypeRef.object(taken);
        }
        String name = Identifiers.pascalJoin(nameHint);
        if (declared.containsKey(name)) {
            warnings.accept("An inline object would be called " + name + ", which the"
                    + " specification already declares; taking the body whole instead");
            return TypeRef.unknown();
        }
        register(name, schema);
        if (!resolved.containsKey(name)) {
            return TypeRef.unknown();
        }
        inlineNames.put(shape, name);
        return TypeRef.object(name);
    }

    private String element(String objectName) {
        return objectName == null ? null : objectName + "Inner";
    }

    // ── REGISTRATION ──────────────────────────────────────────────────────────

    private void register(String name, Schema<?> schema) {
        if (resolved.containsKey(name)) {
            return;
        }
        if (alternativeAlias(schema) != null) {
            // Nothing to register: the generator writes no class for it, and one declared
            // here would be a name the stub could refer to and nothing could satisfy.
            return;
        }
        Merged merged = merge(schema, name, new LinkedHashSet<>());
        if (merged.properties.isEmpty()) {
            reportUndescribable(name, schema);
            return;
        }
        // Placed before the properties are read so that a schema reaching itself finds a
        // registration in progress rather than recursing. Replaced below; nothing reads it
        // in between, because typeOf only records a name.
        resolved.put(name, new ObjectSchema(name, List.of()));

        List<Property> properties = new ArrayList<>();
        merged.properties.forEach((propertyName, declared) -> properties.add(new Property(
                propertyName,
                typeOf(declared.schema(),
                        // An inline object is one class, named after the schema that
                        // declared it and reused wherever the composition is merged. An
                        // inline enum is not: the generator nests a copy in every class
                        // that has the property, so this one is named after the merge.
                        Identifiers.pascalJoin(declared.owner(), propertyName),
                        name + "." + Identifiers.pascalJoin(propertyName) + "Enum"),
                readOnly(declared.schema(), new LinkedHashSet<>()))));
        resolved.put(name, new ObjectSchema(name, properties));
    }

    /**
     * Whether only a server ever sends this property.
     *
     * <p>Follows references, because a specification may mark a shared schema
     * {@code readOnly} and point several properties at it. openapi-generator honours that
     * and drops the setter, so a stub that missed it would not compile.
     */
    private boolean readOnly(Schema<?> schema, Set<String> visiting) {
        if (schema == null) {
            return false;
        }
        if (Boolean.TRUE.equals(schema.getReadOnly())) {
            return true;
        }
        if (schema.get$ref() == null) {
            return false;
        }
        String name = referencedName(schema.get$ref());
        return name != null && visiting.add(name) && readOnly(declared.get(name), visiting);
    }

    /**
     * Says out loud that a schema will have no accessors, unless it plainly never could.
     * An enum or a string alias has nothing to describe and is silent.
     */
    private void reportUndescribable(String name, Schema<?> schema) {
        if (schema.getEnum() != null) {
            return;
        }
        if (composition == Composition.OPAQUE
                && (schema.getOneOf() != null || schema.getAnyOf() != null)) {
            if (alternativeAlias(schema) != null) {
                // One alternative is not opaque: it is that alternative.
                return;
            }
            warnings.accept("Schema " + name + " is composed with oneOf or anyOf, which is"
                    + " being read as opaque; stubs will take bodies of this type whole and"
                    + " offer no accessors into them");
            return;
        }
        if (hasShapeOfItsOwn(schema)) {
            warnings.accept("Schema " + name + " resolved to no properties at all;"
                    + " stubs will take bodies of this type whole");
        }
    }

    /**
     * Flattens a composition into one property list.
     *
     * <p>{@code allOf} members are merged in the order they are written, and a member may
     * be a composition itself. A property declared twice keeps the position of its first
     * declaration and the definition of its last, which is what openapi-generator settles
     * on. {@code oneOf} and {@code anyOf} members are folded in the same way under
     * {@link Composition#MERGE}, after the {@code allOf} members and before the schema's
     * own properties.
     *
     * @param visiting guards composition cycles. Unlike {@link #typeOf}, which stops at a
     *                 name, this follows references, so mutually composed schemas would
     *                 otherwise not terminate.
     */
    private Merged merge(Schema<?> schema, String owner, Set<String> visiting) {
        Merged merged = new Merged();
        if (schema.getAllOf() != null) {
            // An inline allOf member is a schema with no name of its own, and the
            // generator gives it one by inserting AllOf: an object written inline on it
            // becomes MultiAllOfA, not MultiA. The infix does not carry an index, so every
            // inline member of the same schema shares it. oneOf and anyOf get no such
            // infix, which is why the owner is adjusted here and not in memberOf.
            for (Schema<?> member : schema.getAllOf()) {
                merged.addAll(memberOf(member, owner + "AllOf", visiting));
            }
        }
        if (merging(schema)) {
            for (Schema<?> member : alternatives(schema)) {
                merged.addAll(memberOf(member, owner, visiting));
            }
        }
        if (schema.getProperties() != null) {
            schema.getProperties().forEach((key, value) -> merged.properties
                    .put(String.valueOf(key), new Declared((Schema<?>) value, owner)));
        }
        return merged;
    }

    /**
     * The members of a {@code oneOf} or {@code anyOf}, in the order they are written. A
     * schema may legally write both, and folding both is the only reading that does not
     * silently drop half of what was said.
     */
    private List<Schema> alternatives(Schema<?> schema) {
        List<Schema> members = new ArrayList<>();
        if (schema.getOneOf() != null) {
            members.addAll(schema.getOneOf());
        }
        if (schema.getAnyOf() != null) {
            members.addAll(schema.getAnyOf());
        }
        return members;
    }

    private Merged memberOf(Schema<?> member, String owner, Set<String> visiting) {
        if (member.get$ref() == null) {
            return merge(member, owner, visiting);
        }
        String name = referencedName(member.get$ref());
        if (name == null || !declared.containsKey(name)) {
            warnings.accept("Ignoring the composition member " + member.get$ref()
                    + ", which does not resolve");
            return new Merged();
        }
        if (!visiting.add(name)) {
            warnings.accept("Schema " + name + " takes part in a cycle of composition"
                    + " members; stopping there");
            return new Merged();
        }
        Merged merged = merge(declared.get(name), name, visiting);
        visiting.remove(name);
        return merged;
    }

    private String describe(String nameHint) {
        return nameHint == null ? "A body" : "Schema " + nameHint;
    }

    private static final class Merged {

        private final Map<String, Declared> properties = new LinkedHashMap<>();

        void addAll(Merged other) {
            properties.putAll(other.properties);
        }
    }

    /**
     * A property together with the schema that declared it, which is not the schema it
     * ends up on once a composition is flattened.
     *
     * <p>The distinction decides what an inline object or enum written here will be
     * called. openapi-generator names it after the declaring schema and reuses that one
     * class everywhere the composition is merged, so naming it after the merging schema
     * invents a class nothing generates.
     */
    private record Declared(Schema<?> schema, String owner) {
    }
}
