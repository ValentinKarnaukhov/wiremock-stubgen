package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

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

    private StringValuePattern wholeBodyPattern;
    private final List<StringValuePattern> fieldPatterns = new ArrayList<>();

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
     * <p>The no-argument form of the same method installs an empty instance of the
     * declared schema and hands it to a body builder, so the caller can describe the
     * body field by field instead of constructing it. Both forms end here: the builder
     * writes into the very object this method stored.
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

    // ── REQUEST BODY ──────────────────────────────────────────────────────────

    /**
     * Requires the request body to equal the given object as JSON. Backs the generated
     * {@code requestBody(Schema)} method.
     *
     * <p>Not called {@code requestBody}, which would read better, because the generated
     * method is named that: {@code requestBody(Schema)} would be a legal overload of
     * {@code requestBody(Object)}, and the {@code return requestBody(body)} inside it
     * would then resolve to itself. That is a StackOverflowError at runtime rather than
     * an error at compile time, so the name is kept apart on purpose.
     *
     * <p>Replaces rather than accumulates: two whole-document matchers describing
     * different bodies could never both hold, so a second call can only be a
     * correction of the first. The field matchers below are the opposite — each is a
     * separate condition, so they add up.
     */
    protected final S matchWholeRequestBody(Object body) {
        this.wholeBodyPattern = equalToJson(serialize(body));
        return self();
    }

    /**
     * Adds one condition on the request body. Generated matcher builders reach this
     * through a method reference the generated stub hands them, which is why it can
     * stay protected: the raw WireMock surface does not have to be reopened to make
     * the matchers work.
     *
     * <p>Verified against WireMock: repeated withRequestBody calls accumulate into a
     * bodyPatterns array which all have to hold.
     */
    protected final void addRequestBodyPattern(StringValuePattern pattern) {
        fieldPatterns.add(Objects.requireNonNull(pattern, "pattern"));
    }

    private void applyRequestBody(MappingBuilder mappingBuilder) {
        if (wholeBodyPattern != null) {
            mappingBuilder.withRequestBody(wholeBodyPattern);
        }
        fieldPatterns.forEach(mappingBuilder::withRequestBody);
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
        MappingBuilder mappingBuilder = toRequest();
        applyRequestBody(mappingBuilder);
        mappingBuilder.willReturn(toResponse());
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
