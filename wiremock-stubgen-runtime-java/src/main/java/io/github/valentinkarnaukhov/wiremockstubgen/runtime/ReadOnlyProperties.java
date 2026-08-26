package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import java.lang.reflect.Field;

/**
 * Writes a property the model generator deliberately left without a setter.
 *
 * <p>A property marked {@code readOnly} in the specification is one a server sends and a
 * client never submits, so openapi-generator emits a getter and no setter at all. The only
 * public way in is a constructor taking every read-only property of the class at once —
 * verified on 7.24.0, which also confirmed that the same thing happens when {@code
 * readOnly} sits on a referenced schema, on an {@code allOf}, or on an array.
 *
 * <p>That constructor cannot serve a builder. A builder is told one property at a time and
 * has already created the object by the time the second arrives, so using the constructor
 * would mean either buffering every value until the body is finished — which the nested
 * scopes hand out eagerly — or rebuilding the object and copying across everything set so
 * far. Both trade a working accessor for a large amount of generated machinery.
 *
 * <p>Skipping these properties instead was the other option and is worse. A stub exists to
 * play the server, and read-only properties are precisely the ones only a server produces:
 * a response body that cannot carry them is missing the fields the test came for.
 *
 * <p>So the field is written directly. This is the one place in the generated output that
 * is not checked by the compiler, which is why it is here, named, and documented, rather
 * than inlined into every stub that needs it.
 */
public final class ReadOnlyProperties {

    private ReadOnlyProperties() {
    }

    /**
     * Sets {@code field} on {@code target}, whatever its visibility.
     *
     * @param field the Java field name, which is the model generator's camel-cased form of
     *              the property name — the same name its fluent setter would have had
     * @throws IllegalStateException if the field cannot be found or written, always naming
     *                               the class and property so the specification can be
     *                               matched against the model that was actually generated
     */
    public static void set(Object target, String field, Object value) {
        Class<?> owner = target.getClass();
        for (Class<?> type = owner; type != null && type != Object.class; type = type.getSuperclass()) {
            Field declared;
            try {
                declared = type.getDeclaredField(field);
            } catch (NoSuchFieldException missing) {
                continue;
            }
            write(declared, target, value, owner, field);
            return;
        }
        throw new IllegalStateException(owner.getName() + " has no field '" + field
                + "'. The specification says it is a read-only property, so the stub writes"
                + " it directly; the model class beside it does not have it, which means the"
                + " two were generated from different specifications.");
    }

    private static void write(Field field, Object target, Object value, Class<?> owner, String name) {
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (RuntimeException | IllegalAccessException refused) {
            throw new IllegalStateException("Cannot write the read-only property '" + name
                    + "' on " + owner.getName() + ". If the models are in a named module,"
                    + " that module has to open their package to "
                    + ReadOnlyProperties.class.getPackageName() + ".", refused);
        }
    }
}
