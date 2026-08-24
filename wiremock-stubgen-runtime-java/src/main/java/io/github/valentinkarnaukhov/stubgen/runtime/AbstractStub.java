package io.github.valentinkarnaukhov.stubgen.runtime;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

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
    private String contentType = APPLICATION_JSON;

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
        ResponseDefinitionBuilder response = aResponse().withStatus(status);
        if (body != null) {
            response.withHeader(CONTENT_TYPE, contentType).withBody(serialize(body));
        }
        return response;
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
