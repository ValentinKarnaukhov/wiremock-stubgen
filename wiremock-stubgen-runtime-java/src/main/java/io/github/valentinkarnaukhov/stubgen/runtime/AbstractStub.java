package io.github.valentinkarnaukhov.stubgen.runtime;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

/**
 * Base class for generated stub builders.
 *
 * <p>Carries everything that does not depend on the operation: where to register, how the
 * response is assembled, and an escape hatch onto the raw WireMock API. A generated
 * subclass is therefore left with the one thing that is genuinely per-operation — the
 * request matcher — plus a typed one-liner per declared status code.
 *
 * <p>Keeping this hand-written means fixing a bug here is a runtime version bump, not a
 * regeneration of every stub in every consuming project.
 *
 * @param <S> the concrete builder type, so fluent methods keep the caller's type
 */
public abstract class AbstractStub<S extends AbstractStub<S>> {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private final StubTarget target;

    private int status = 200;
    private Object body;
    private boolean bodySet;
    private String contentType = APPLICATION_JSON;

    private final List<Consumer<Object>> bodyMutations = new ArrayList<>();

    private Consumer<MappingBuilder> customizer = mappingBuilder -> {
    };

    protected AbstractStub(StubTarget target) {
        this.target = Objects.requireNonNull(target, "target");
    }

    @SuppressWarnings("unchecked")
    protected final S self() {
        return (S) this;
    }

    /**
     * Responds with a status code the specification does not declare, and therefore with
     * no body — there is no schema to type it against.
     */
    public final S code(int status) {
        this.status = status;
        this.body = null;
        this.bodySet = true;
        return self();
    }

    /**
     * Backs the generated per-code methods.
     *
     * <p>A generated subclass exposes one typed method per declared status code, each of
     * which is a single delegation:
     *
     * <pre>{@code
     * public GetResponseErrorsStub code404(ErrorBody body) {
     *     return response(404, body);
     * }
     * }</pre>
     *
     * <p>The body is Object here because the declared schemas of one operation share no
     * supertype. The erasure never reaches the caller: it is reintroduced by the typed
     * method above.
     *
     * <p>A stub is one mapping and therefore one response, so a second call replaces the
     * first rather than adding to it. Sequences of responses are WireMock scenarios,
     * reachable through {@link #customize(Consumer)}.
     */
    protected final S response(int status, Object body) {
        this.status = status;
        this.body = body;
        this.bodySet = true;
        return self();
    }

    /**
     * Records a change to be applied to the response body when the mapping is built,
     * rather than to whatever object happens to be present now.
     *
     * <p>Backs the flattened accessors a generated stub exposes for nested fields:
     *
     * <pre>{@code
     * public GetResponseCompositeListStub compositeInnerField(String value) {
     *     return mutateBody((List<CompositeBody> body) ->
     *             body.forEach(item -> item.getComposite().innerField(value)));
     * }
     * }</pre>
     *
     * <p>Deferring is what keeps call order free. Applied eagerly, an accessor would
     * write into the body present at that moment, and a later code200(...) would
     * throw those writes away — so the fluent chain would only work in one order,
     * and the wrong order would fail silently.
     *
     * <p>The mutation runs against the body the caller supplied, or against
     * {@link #skeletonBody()} if they supplied none. It mutates that object in
     * place; a caller who passes a shared instance will see it change.
     */
    protected final <T> S mutateBody(Consumer<T> mutation) {
        Objects.requireNonNull(mutation, "mutation");
        bodyMutations.add(body -> {
            @SuppressWarnings("unchecked")
            T typed = (T) body;
            mutation.accept(typed);
        });
        return self();
    }

    /**
     * Overrides the media type of the response. Generated code calls this when the
     * specification declares something other than JSON for the selected code.
     */
    public final S contentType(String contentType) {
        this.contentType = Objects.requireNonNull(contentType, "contentType");
        return self();
    }

    /**
     * Applies arbitrary WireMock configuration that the generated API does not model —
     * delays, priorities, scenarios, fault injection and so on.
     *
     * <p>Deliberate escape hatch: the generated surface covers the common cases, and
     * anything beyond them stays reachable without abandoning the generated builder.
     * Customisers accumulate in registration order and see the finished mapping builder,
     * so they can override anything decided here.
     */
    public final S customize(Consumer<MappingBuilder> customizer) {
        Objects.requireNonNull(customizer, "customizer");
        this.customizer = this.customizer.andThen(customizer);
        return self();
    }

    /**
     * Builds the mapping described by this builder, without registering it.
     */
    public final StubMapping buildStub() {
        MappingBuilder mappingBuilder = toRequest().willReturn(toResponse());
        customizer.accept(mappingBuilder);
        return mappingBuilder.build();
    }

    /**
     * Builds and registers the mapping.
     */
    public final StubMapping mock() {
        StubMapping mapping = buildStub();
        target.register(mapping);
        return mapping;
    }

    /**
     * Matches the request this operation describes: method, path, parameters, headers and
     * request body. Implemented by generated subclasses — it is the only part of a
     * mapping that differs per operation.
     */
    protected abstract MappingBuilder toRequest();

    /**
     * Assembles the response from the accumulated status, body and media type.
     */
    protected ResponseDefinitionBuilder toResponse() {
        Object effectiveBody = effectiveBody();
        ResponseDefinitionBuilder response = aResponse().withStatus(status);
        if (effectiveBody != null) {
            response.withHeader(CONTENT_TYPE, contentType).withBody(serialize(effectiveBody));
        }
        return response;
    }

    private Object effectiveBody() {
        Object effectiveBody = bodySet || bodyMutations.isEmpty() ? body : skeletonBody();
        bodyMutations.forEach(mutation -> mutation.accept(effectiveBody));
        return effectiveBody;
    }

    /**
     * An instance of the response body with every nested object present and every
     * primitive left unset, used only when a flattened accessor is called without a
     * body having been supplied.
     *
     * <p>A flattened accessor has to traverse the structure — getComposite() then
     * getDeepField() — and on a freshly constructed model those return null. The
     * skeleton is what makes the traversal possible; it is a precondition, not a
     * convenience.
     *
     * <p>Built lazily on purpose. Installed eagerly it would change what a stub
     * answers by default, replacing an empty body with one full of empty objects
     * and phantom list elements the real service would never send.
     *
     * <p>It is structure only, never plausible data. Measured over 35 specifications
     * from a real project: 7% of schema properties carry an example, 1% of schemas
     * do, and no response media type did. Plausible data is domain knowledge and has
     * to be supplied.
     *
     * <p>Generated stubs override this. The default returns null, which makes a
     * flattened accessor on a bodyless operation fail loudly rather than silently do
     * nothing.
     *
     * <p>OPEN: the skeleton does not depend on the selected status code, although an
     * operation may declare a different schema per code. Adequate while flattened
     * accessors describe the success body only.
     */
    protected Object skeletonBody() {
        return null;
    }

    /**
     * Turns a response body into the bytes WireMock will send.
     *
     * <p>UNRESOLVED, and centralised here precisely because it is: the mapper used must
     * agree with the one the consumer's HTTP client uses to deserialise, or the test
     * fails on a difference the stub introduced. Consumer models are generated by
     * openapi-generator and carry Jackson annotations — date formats, {@code @JsonInclude},
     * naming strategies — that a foreign mapper will not honour.
     *
     * <p>Note also that {@code org.wiremock:wiremock} bundles Jackson unshaded, whereas
     * {@code wiremock-standalone} relocates it to {@code wiremock.com.fasterxml.jackson}.
     * Under the standalone artifact the mapper below cannot see the consumer's
     * annotations at all, since those are in the unrelocated package.
     *
     * <p>Overridable so a consumer can supply their own mapper today; a first-class hook
     * is still owed.
     */
    protected String serialize(Object body) {
        return Json.write(body);
    }
}
