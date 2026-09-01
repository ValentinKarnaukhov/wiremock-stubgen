package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import java.lang.reflect.Field;

/**
 * Writes a read-only property via reflection. openapi-generator gives such a property a
 * getter but no setter, and a stub still has to fill it in as the server would.
 */
public final class ReadOnlyProperties {

    private ReadOnlyProperties() {
    }

    /**
     * Sets {@code field} on {@code target}, whatever its visibility.
     *
     * @param field the Java field name, which is the model generator's camel-cased form of
     *              the property name — the same name its fluent setter would have had
     * @throws IllegalStateException if the field cannot be found or written
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
