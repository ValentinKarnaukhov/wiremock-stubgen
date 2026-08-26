package com.example.stubs.getresponseerrors;

import com.example.model.CompositeBody;
import com.example.model.CompositeDeepField;
import com.example.model.CompositeField;
import com.example.model.ErrorBody;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import io.github.valentinkarnaukhov.wiremockstubgen.runtime.AbstractResponseBodyBuilder;
import io.github.valentinkarnaukhov.wiremockstubgen.runtime.AbstractStub;
import io.github.valentinkarnaukhov.wiremockstubgen.runtime.StubTarget;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * GOLDEN REFERENCE — hand-written specification of the generator's output.
 * Source: sample-api.yaml, operation getResponseErrors.
 *
 * <p>Demonstrates: one operation declaring several status codes whose bodies have
 * different schemas — 200 returns CompositeBody, 404 and 500 return ErrorBody.
 *
 * <p>Reads as:
 *
 * <pre>{@code
 * new GetResponseErrorsStub(target).code404().code("E42").message("gone").mock();
 * }</pre>
 */
public final class GetResponseErrorsStub extends AbstractStub<GetResponseErrorsStub> {

    private static final String PATH = "/get/response/errors";

    public GetResponseErrorsStub(StubTarget target) {
        super(target);
    }

    public GetResponseErrorsStub code200(CompositeBody body) {
        return response(200, body);
    }

    public CompositeBodyBuilder<GetResponseErrorsStub> code200() {
        CompositeBody body = new CompositeBody();
        response(200, body);
        return new CompositeBodyBuilder<>(this, body);
    }

    public GetResponseErrorsStub code404(ErrorBody body) {
        return response(404, body);
    }

    public ErrorBodyBuilder<GetResponseErrorsStub> code404() {
        ErrorBody body = new ErrorBody();
        response(404, body);
        return new ErrorBodyBuilder<>(this, body);
    }

    /**
     * 500 declares the same schema as 404. The methods stay separate, being generated
     * per declared code, but hand out the same builder.
     */
    public GetResponseErrorsStub code500(ErrorBody body) {
        return response(500, body);
    }

    public ErrorBodyBuilder<GetResponseErrorsStub> code500() {
        ErrorBody body = new ErrorBody();
        response(500, body);
        return new ErrorBodyBuilder<>(this, body);
    }

    @Override
    protected MappingBuilder toRequest() {
        return get(urlPathEqualTo(PATH));
    }

    // ── BODY BUILDERS ─────────────────────────────────────────────────────────
    //
    // ErrorBodyBuilder is where sharing within an operation pays: one class serves
    // 404 and 500. CompositeFieldListBuilder is where nesting costs: it repeats the
    // class GetResponseCompositeListStub declares, because that copy is not in
    // scope here. Locality was preferred to sharing — one operation, one file.

    public final class CompositeBodyBuilder<P> extends AbstractResponseBodyBuilder<P> {

        private final CompositeBody body;

        CompositeBodyBuilder(P parent, CompositeBody body) {
            super(parent, GetResponseErrorsStub.this);
            this.body = body;
        }

        public CompositeBodyBuilder<P> primitive(String value) {
            body.primitive(value);
            return this;
        }

        public CompositeBodyBuilder<P> compositeInnerField(String value) {
            composite().innerField(value);
            return this;
        }

        public CompositeBodyBuilder<P> compositeDeepFieldDeepestField(String value) {
            compositeDeepField().deepestField(value);
            return this;
        }

        public CompositeBodyBuilder<P> primitiveList(List<String> value) {
            body.primitiveList(value);
            return this;
        }

        public CompositeFieldListBuilder<CompositeBodyBuilder<P>> compositeList() {
            if (body.getCompositeList() == null) {
                body.compositeList(new ArrayList<>());
            }
            return new CompositeFieldListBuilder<>(this, body.getCompositeList());
        }

        private CompositeField composite() {
            if (body.getComposite() == null) {
                body.composite(new CompositeField());
            }
            return body.getComposite();
        }

        private CompositeDeepField compositeDeepField() {
            CompositeField composite = composite();
            if (composite.getDeepField() == null) {
                composite.deepField(new CompositeDeepField());
            }
            return composite.getDeepField();
        }
    }

    public final class CompositeFieldListBuilder<P> extends AbstractResponseBodyBuilder<P> {

        private final List<CompositeField> items;

        private CompositeField current;

        CompositeFieldListBuilder(P parent, List<CompositeField> items) {
            super(parent, GetResponseErrorsStub.this);
            this.items = items;
        }

        public CompositeFieldListBuilder<P> addNew() {
            current = new CompositeField();
            items.add(current);
            return this;
        }

        public CompositeFieldListBuilder<P> innerField(String value) {
            current().innerField(value);
            return this;
        }

        public CompositeFieldListBuilder<P> deepFieldDeepestField(String value) {
            deepField().deepestField(value);
            return this;
        }

        private CompositeField current() {
            if (current == null) {
                addNew();
            }
            return current;
        }

        private CompositeDeepField deepField() {
            CompositeField item = current();
            if (item.getDeepField() == null) {
                item.deepField(new CompositeDeepField());
            }
            return item.getDeepField();
        }
    }

    /**
     * Note code(String): on the stub that name belongs to the status code, and here
     * it belongs to a field. Nesting keeps the two apart without renaming either.
     */
    public final class ErrorBodyBuilder<P> extends AbstractResponseBodyBuilder<P> {

        private final ErrorBody body;

        ErrorBodyBuilder(P parent, ErrorBody body) {
            super(parent, GetResponseErrorsStub.this);
            this.body = body;
        }

        public ErrorBodyBuilder<P> code(String value) {
            body.code(value);
            return this;
        }

        public ErrorBodyBuilder<P> message(String value) {
            body.message(value);
            return this;
        }
    }

    // ── NOTES ─────────────────────────────────────────────────────────────────
    //
    // ONE STUB IS ONE MAPPING, HENCE ONE RESPONSE.
    // Calling two codeNNN methods cannot mean "both" — the last call wins, like any
    // other setter. Failing on the second call would trade a compile-time-shaped
    // API for a runtime exception. Responses in sequence are a WireMock scenario,
    // reached through customize(...).
    //
    // CLOSED — AN ACCESSOR AGAINST THE WRONG CODE.
    // The only way to reach ErrorBodyBuilder is through code404() or code500(),
    // which have already installed an ErrorBody. Nothing left to guard.
    //
    // NAMING. codeNNN cannot collide with a parameter-derived name, because
    // parameter methods carry a location prefix and no location is spelt "code".
    //
    // CLOSED — DEFAULT RESPONSE. codeDefault takes the status as a parameter: the
    // response names none, so any the generator picked would be invented. Covered by
    // the example rather than here, where no operation declares one.
    //
    // OPEN — CONTENT TYPE. AbstractStub defaults to application/json. Anything else
    // would also have to stop serialising as JSON, and several media types for one
    // code have no obvious shape. Uncovered by the fixture.
    //
    // OPEN — NO RESPONSE SELECTED. AbstractStub answers 200 with no body, which is
    // misleading when 200 declares a schema and the client fails to deserialise
    // nothing. Whether that should be a failure is undecided.
    // ──────────────────────────────────────────────────────────────────────────
}
