package io.github.valentinkarnaukhov.wiremockstubgen.runtime;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

/**
 * Base class for generated stub builders.
 *
 * <p>Carries everything that does not depend on the operation: where to register, how the
 * response is assembled, and an escape hatch onto the raw WireMock API.
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
    private final Map<Object, StringValuePattern> fieldPatterns = new LinkedHashMap<>();

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
     * Backs the generated per-code methods, each a single typed delegation. The body is
     * {@code Object} here since one operation's declared schemas share no supertype; the
     * generated method reintroduces the type. A second call replaces the first, since a
     * stub is one mapping; a sequence of responses is a WireMock scenario, reachable
     * through {@link #customize(Consumer)}.
     */
    protected final S response(int status, Object body) {
        this.status = status;
        this.body = body;
        return self();
    }

    // ── REQUEST BODY ──────────────────────────────────────────────────────────

    /**
     * Requires the request body to equal the given object as JSON. Backs the generated
     * {@code requestBody(Schema)} method — not named {@code requestBody} itself, since
     * that would recurse into a {@code StackOverflowError} instead of a compile error.
     * Replaces rather than accumulates, since two whole-document matchers describing
     * different bodies could never both hold.
     */
    protected final S matchWholeRequestBody(Object body) {
        this.wholeBodyPattern = equalToJson(serialize(body));
        return self();
    }

    /**
     * Adds one condition on the request body, or replaces the one previously added under
     * the same owner. {@code owner} is usually a fresh key, so conditions accumulate the
     * way repeated WireMock {@code withRequestBody} calls do; a matcher for some element
     * of an array instead passes itself as the owner on every call, folding every
     * condition it is asked for into one replaced filter, since an array position could
     * otherwise be satisfied by a different element per condition.
     */
    protected final void addRequestBodyPattern(Object owner, StringValuePattern pattern) {
        fieldPatterns.put(Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(pattern, "pattern"));
    }

    private void applyRequestBody(MappingBuilder mappingBuilder) {
        if (wholeBodyPattern != null) {
            mappingBuilder.withRequestBody(wholeBodyPattern);
        }
        fieldPatterns.values().forEach(mappingBuilder::withRequestBody);
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
     * Applies arbitrary WireMock configuration the generated API does not model — delays,
     * priorities, scenarios, fault injection. Customisers accumulate in registration order
     * and see the finished mapping builder, so they can override anything decided here.
     */
    public final S customize(Consumer<MappingBuilder> customizer) {
        Objects.requireNonNull(customizer, "customizer");
        this.customizer = this.customizer.andThen(customizer);
        return self();
    }

    /** Builds the mapping described by this builder, without registering it. */
    public final StubMapping buildStub() {
        MappingBuilder mappingBuilder = toRequest();
        applyRequestBody(mappingBuilder);
        mappingBuilder.willReturn(toResponse());
        customizer.accept(mappingBuilder);
        return mappingBuilder.build();
    }

    /** Builds and registers the mapping. */
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

    /** Assembles the response from the accumulated status, body and media type. */
    protected ResponseDefinitionBuilder toResponse() {
        ResponseDefinitionBuilder response = aResponse().withStatus(status);
        if (body != null) {
            response.withHeader(CONTENT_TYPE, contentType).withBody(serialize(body));
        }
        return response;
    }

    /**
     * Turns a body into the JSON text a request is matched against or a response is sent
     * as. Delegates to {@link StubTarget#serializer()} (see {@link BodySerializer}) and
     * stays overridable for the rare case that isn't enough.
     */
    protected String serialize(Object body) {
        return target.serializer().serialize(body);
    }
}
