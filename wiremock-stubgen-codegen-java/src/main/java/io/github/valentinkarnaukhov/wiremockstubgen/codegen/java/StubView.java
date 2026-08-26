package io.github.valentinkarnaukhov.wiremockstubgen.codegen.java;

import java.util.List;

/**
 * Everything one stub template needs, and nothing it has to work out for itself.
 *
 * <p>Type names arrive already shortened and already imported: a template produces the
 * file in one pass, header first, so it cannot ask for an import halfway through. The
 * price is that a consumer overriding a template and reaching for a type the default
 * template never mentions must import it themselves.
 *
 * <p>The body classes come in two shapes that deliberately do not share a record: a
 * builder constructs an object and creates the objects on the way to it, a matcher appends
 * a JSONPath expression and creates nothing. A common record would leave half its fields
 * null either way.
 */
record StubView(
        String packageName,
        List<String> imports,
        String className,
        String abstractStub,
        String stubTarget,
        String pathConstant,
        String path,
        ParameterFields parameterFields,
        List<ParameterMethod> parameterMethods,
        BodyMethod requestBody,
        MatcherEntry requestMatcher,
        List<ResponseMethod> responses,
        Request request,
        List<MatcherClass> matchers,
        List<BuilderClass> builders) {

    /** The parameter maps a stub collects matchers in. All four share one set of types. */
    record ParameterFields(
            String mapType,
            String keyType,
            String valueType,
            String implType,
            List<String> names) {
    }

    record ParameterMethod(
            String name,
            String type,
            String field,
            String wireName,
            String equalTo,
            String valueExpression) {
    }

    record BodyMethod(String type) {
    }

    /** The {@code requestBody()} that hands out a matcher over the request body. */
    record MatcherEntry(String matcherClass, String rootPath) {
    }

    /**
     * A {@code null} type means the response declares no body, and the template emits a
     * no-argument method rather than one taking {@code Object}. A {@code null} builder
     * means the body is there but cannot be taken apart — a bare string, a map, an array
     * of primitives — or that no model package was configured to name its pieces.
     */
    /**
     * @param status           the expression put on the wire, which is a literal for a
     *                         declared code and the caller's argument for {@code default}
     * @param statusParameter  whether the method takes that status as a parameter
     */
    record ResponseMethod(
            String name,
            String status,
            boolean statusParameter,
            String type,
            BuilderEntry builder) {
    }

    record BuilderEntry(String builderClass, String declaredType, String initExpression) {
    }

    /**
     * Query parameters go in one call and can stay on the chain; the others are applied
     * one at a time and need a variable. {@code local} says which shape the method takes.
     */
    record Request(
            String mappingBuilder,
            String expression,
            boolean local,
            boolean hasPath,
            boolean hasQuery,
            boolean hasHeader,
            boolean hasCookie) {
    }

    // ── response builders ─────────────────────────────────────────────────────

    /**
     * @param list     a scope entered through a list holds the list and appends to it; a
     *                 single one holds the object
     * @param itemType the model type the scope writes into
     * @param listType {@code List<itemType>}, needed only by a list scope
     * @param holders  the private methods that create the objects accessors reach through
     */
    record BuilderClass(
            String name,
            String stubClass,
            String base,
            boolean list,
            String itemType,
            String listType,
            boolean hasCurrent,
            List<BuilderAccessor> accessors,
            List<BuilderHolder> holders) {
    }

    /**
     * @param owner    the builder that declares this, so the method can return {@code this}
     *                 under its own type
     * @param receiver the expression a value is written to — the body field, the current
     *                 element, or a call to the holder that owns the property
     * @param readOnly whether the model has no setter for this property, so it has to be
     *                 written through {@code writer} instead
     * @param writer   the runtime helper that writes a read-only property, or {@code null}
     */
    record BuilderAccessor(
            String owner,
            String name,
            String type,
            boolean nested,
            String targetBuilder,
            String property,
            String getter,
            Local local,
            String receiver,
            boolean readOnly,
            String writer) {
    }

    record BuilderHolder(
            String name,
            String holderType,
            String property,
            String getter,
            Local local,
            String receiver,
            boolean readOnly,
            String writer) {
    }

    /**
     * A local variable a creating method needs because it reads its receiver twice: once
     * to test it and once to write to it.
     */
    record Local(String localType, String localName, String localInit) {
    }

    // ── request matchers ──────────────────────────────────────────────────────

    record MatcherClass(
            String name,
            String stubClass,
            String base,
            List<MatcherAccessor> accessors) {
    }

    /**
     * @param jsonPath  the path relative to the one the matcher is rooted at, leading dot
     *                  included
     * @param valueList a list of leaves is asked "does any element equal this", which is a
     *                  filter expression rather than a pattern on a path
     */
    record MatcherAccessor(
            String owner,
            String name,
            String type,
            boolean nested,
            boolean valueList,
            String targetMatcher,
            String jsonPath,
            String equalTo,
            String valueExpression) {
    }
}
