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
     * 500 declares the same schema as 404. The methods stay separate, because they
     * are generated per declared code and the generated API keeps the shape of the
     * specification — but they hand out the same builder, so the fields behind them
     * are written once.
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
    // 404 and 500, where the flat design had to emit response404Code,
    // response404Message, response500Code and response500Message for the same two
    // fields.
    //
    // CompositeFieldListBuilder below is where nesting costs: it is character for
    // character the class GetResponseCompositeListStub declares, written out again
    // because that stub's copy is not in scope here. Locality was preferred to
    // sharing deliberately — one operation, one file.

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
    // A stub cannot answer 200 and 404 at the same time; the status and the body
    // are a single pair. Calling two codeNNN methods therefore cannot mean "both"
    // — the last call wins, silently, exactly like any other setter. Decided
    // deliberately: failing on the second call would trade a compile-time-shaped
    // API for a runtime exception, and this generator exists to move errors the
    // other way.
    //
    // Returning different responses in sequence is a WireMock scenario, not a
    // property of one mapping. That stays outside the generated surface and is
    // reached through customize(...).
    //
    // CLOSED — AN ACCESSOR AGAINST THE WRONG CODE.
    // The flat design could declare the 200 body and then answer 404, handing a
    // CompositeBody description to an ErrorBody. Binding each accessor to a status
    // fixed it at the price of putting the code in every name, and of emitting the
    // ErrorBody accessors twice. The builder closes it outright: the only way to
    // reach ErrorBodyBuilder is through code404() or code500(), which have already
    // installed an ErrorBody. There is nothing left to guard.
    //
    // NAMING. codeNNN was chosen over respondNNN / willReturnNNN because the
    // number is the whole message and a shorter prefix keeps it prominent. It also
    // cannot collide with a parameter-derived name: parameter methods carry a
    // location prefix (queryStringParam, pathStringParam), and no location is spelt
    // "code".
    //
    // OPEN — DEFAULT RESPONSE.
    // This operation declares no "default" response. When one is present the
    // natural mapping is codeDefault(Schema), but the status to send is then
    // unknown and would have to be supplied: codeDefault(503, body). Not settled,
    // and the fixture does not cover it yet.
    //
    // OPEN — CONTENT TYPE.
    // AbstractStub defaults to application/json, which every response in the
    // fixture declares. For anything else the generated code would call
    // contentType(...) — but serialisation would then also have to stop being JSON,
    // and an operation declaring several media types for one code has no obvious
    // shape at all. Uncovered by the fixture.
    //
    // OPEN — NO RESPONSE SELECTED.
    // If the user calls neither codeNNN nor code, AbstractStub answers 200 with no
    // body at all. That is defensible for an operation whose 200 declares no
    // content, and misleading for this one, where 200 declares CompositeBody and
    // the client will fail to deserialise nothing. Whether an unset body should be
    // a failure when the selected code declares a schema is undecided.
    // ──────────────────────────────────────────────────────────────────────────
}
