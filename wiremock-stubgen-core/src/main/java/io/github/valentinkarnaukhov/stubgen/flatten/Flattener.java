package io.github.valentinkarnaukhov.stubgen.flatten;

import io.github.valentinkarnaukhov.stubgen.spec.ObjectSchema;
import io.github.valentinkarnaukhov.stubgen.spec.Property;
import io.github.valentinkarnaukhov.stubgen.spec.StubApi;
import io.github.valentinkarnaukhov.stubgen.spec.TypeRef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Turns a body schema into the scopes and accessors a stub is built from.
 *
 * <p>The rules below were not invented here. They were written by hand first, as golden
 * reference stubs, and read back out afterwards; this class is that reading made
 * executable. Where the goldens are silent the choice is marked as such.
 *
 * <ol>
 *   <li><b>A primitive property is a leaf.</b> So is an enum, a map, and an array of
 *       arrays — anything with no described object inside it.</li>
 *   <li><b>A single-valued object property gets no accessor at all.</b> Its own accessors
 *       are lifted into the current scope under a compound name, recursively. A builder
 *       would buy a shorter name at the price of two more {@code exit()} calls, and the
 *       nesting a specification writes is mostly not nesting a caller cares about. It
 *       follows that a schema only ever reached through single-valued properties never
 *       gets a class of its own.</li>
 *   <li><b>An array of leaves is a leaf, and the two sides disagree about it.</b> A
 *       response replaces the whole list, a request asks whether some element equals the
 *       value. The asymmetry is deliberate: producing and matching are different
 *       actions.</li>
 *   <li><b>An array of described objects hands out a scope.</b> Flattening stops there.
 *       Without a scope the generated API would have to decide by itself which element a
 *       value belongs to, and every answer — the first, all of them, one per call — is a
 *       guess the specification cannot support.</li>
 *   <li><b>Depth is counted inside a scope and restarts at every nested list.</b> See
 *       {@link BodyScope} for why this, and not depth from the body root.</li>
 *   <li><b>Names that would land on a runtime method are escaped</b>, and collisions
 *       between two routes that flatten to one name are fatal. See {@link Names} and
 *       {@link FlatteningException}.</li>
 * </ol>
 *
 * <p>There is no cycle detection, and none is needed. A cycle that runs through a list is
 * harmless, because the list hands out a scope and flattening stops; a cycle that runs
 * entirely through single-valued properties is stopped by the depth limit, in the same
 * breath as a merely deep schema. Measured over 673 real schemas, the second kind does
 * not occur at all — the one cycle found runs through a list.
 */
public final class Flattener {

    private final StubApi api;

    private final FlatteningOptions options;

    public Flattener(StubApi api, FlatteningOptions options) {
        this.api = Objects.requireNonNull(api, "api");
        this.options = Objects.requireNonNull(options, "options");
    }

    /**
     * Flattens one body.
     *
     * <p>Empty when the body is not built on a described object schema — no content, a
     * bare string, a map, an array of primitives. Those are not failures and must not be
     * treated as such: the emitter still has a typed whole-body form to offer, it just has
     * nothing to take apart.
     */
    public Optional<BodyModel> flatten(TypeRef body, BodySide side) {
        Objects.requireNonNull(side, "side");
        if (body == null) {
            return Optional.empty();
        }
        boolean rootIsList = body.kind() == TypeRef.Kind.ARRAY;
        TypeRef rootType = rootIsList ? body.items() : body;
        if (rootType == null || rootType.kind() != TypeRef.Kind.OBJECT) {
            return Optional.empty();
        }
        Optional<ObjectSchema> rootSchema = api.schemaOf(rootType);
        if (rootSchema.isEmpty()) {
            return Optional.empty();
        }

        // A request matcher is one class per schema wherever it stands, so the root's
        // list-ness is recorded on the model and never on the scope.
        boolean rootScopeIsList = rootIsList && side.distinguishesPosition();

        List<BodyScope> scopes = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        Deque<PendingScope> queue = new ArrayDeque<>();
        PendingScope rootScope = new PendingScope(rootSchema.get().name(), rootScopeIsList);
        queue.add(rootScope);
        visited.add(rootScope.id());

        while (!queue.isEmpty()) {
            PendingScope pending = queue.poll();
            Flattened flattened = flattenSchema(api.schemas().get(pending.schemaName()));
            scopes.add(new BodyScope(
                    pending.schemaName(), pending.listPosition(), flattened.accessors(), flattened.intermediates()));

            for (Accessor accessor : flattened.accessors()) {
                if (accessor.kind() != Accessor.Kind.NESTED_LIST) {
                    continue;
                }
                PendingScope next = new PendingScope(accessor.targetSchema(), side.distinguishesPosition());
                if (visited.add(next.id())) {
                    queue.add(next);
                }
            }
        }
        return Optional.of(new BodyModel(side, rootSchema.get().name(), rootIsList, scopes));
    }

    /**
     * The accessor set does not depend on the side. That is the symmetry the two goldens
     * were written to have: the same schema reads the same way whether it is being
     * produced or matched, and only the signatures behind the names differ.
     */
    private Flattened flattenSchema(ObjectSchema schema) {
        Map<String, Accessor> byName = new LinkedHashMap<>();
        List<Intermediate> intermediates = new ArrayList<>();
        collect(schema, List.of(), byName, intermediates, schema.name());
        return new Flattened(List.copyOf(byName.values()), List.copyOf(intermediates));
    }

    private void collect(ObjectSchema schema, List<String> prefix, Map<String, Accessor> byName,
                         List<Intermediate> intermediates, String scopeName) {
        if (prefix.size() >= options.maxDepth()) {
            return;
        }
        for (Property property : schema.properties()) {
            List<String> path = append(prefix, property.name());
            TypeRef type = property.type();

            Optional<ObjectSchema> nested = singleValuedObject(type);
            if (nested.isPresent()) {
                // Recorded before descending, so the list reads in the order a builder has
                // to create them: an object always precedes what it contains.
                intermediates.add(new Intermediate(
                        Names.escape(Names.join(path), options.reservedNames()), path, nested.get().name(),
                        property.readOnly()));
                collect(nested.get(), path, byName, intermediates, scopeName);
                continue;
            }

            Accessor accessor = leafOrTransition(path, type, property.readOnly());
            Accessor clash = byName.putIfAbsent(accessor.name(), accessor);
            if (clash != null) {
                throw new FlatteningException(
                        "schema '%s' flattens two different properties onto one accessor named '%s': %s and %s. "
                                .formatted(scopeName, accessor.name(), join(clash.path()), join(accessor.path()))
                                + "Generated code would not compile. Rename one of the properties in the "
                                + "specification, or lower maxDepth so the deeper one is not reached.");
            }
        }
    }

    private Accessor leafOrTransition(List<String> path, TypeRef type, boolean readOnly) {
        String name = Names.escape(Names.join(path), options.reservedNames());
        if (type.kind() == TypeRef.Kind.ARRAY && type.items() != null) {
            TypeRef element = type.items();
            Optional<ObjectSchema> described = describedObject(element);
            if (described.isPresent()) {
                return new Accessor(
                        Accessor.Kind.NESTED_LIST, name, path, element, described.get().name(), readOnly);
            }
            return new Accessor(Accessor.Kind.VALUE_LIST, name, path, element, null, readOnly);
        }
        return new Accessor(Accessor.Kind.VALUE, name, path, type, null, readOnly);
    }

    /**
     * The one property shape that disappears instead of becoming a method.
     *
     * <p>An object property whose schema the catalogue does not hold stays a leaf on
     * purpose. It cannot be flattened — there is nothing to flatten — and a typed
     * one-argument accessor is strictly better than dropping the property, which is the
     * only other option.
     */
    private Optional<ObjectSchema> singleValuedObject(TypeRef type) {
        return type.kind() == TypeRef.Kind.OBJECT ? describedObject(type) : Optional.empty();
    }

    private Optional<ObjectSchema> describedObject(TypeRef type) {
        if (type.kind() != TypeRef.Kind.OBJECT) {
            return Optional.empty();
        }
        return Optional.ofNullable(api.schemas().get(type.schemaName()));
    }

    private static List<String> append(List<String> prefix, String segment) {
        List<String> path = new ArrayList<>(prefix.size() + 1);
        path.addAll(prefix);
        path.add(segment);
        return List.copyOf(path);
    }

    private static String join(List<String> path) {
        return String.join(".", path);
    }

    private record PendingScope(String schemaName, boolean listPosition) {
        String id() {
            return listPosition ? schemaName + "[]" : schemaName;
        }
    }

    private record Flattened(List<Accessor> accessors, List<Intermediate> intermediates) {
    }
}
