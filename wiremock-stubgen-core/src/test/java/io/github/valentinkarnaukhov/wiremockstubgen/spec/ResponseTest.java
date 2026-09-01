package io.github.valentinkarnaukhov.wiremockstubgen.spec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResponseTest {

    @Test
    void statusCodeNullMeansTheDefaultResponse() {
        Response response = new Response(null, TypeRef.unknown(), null);

        assertThat(response.isDefault()).isTrue();
    }

    @Test
    void aDeclaredStatusCodeIsNotTheDefaultResponse() {
        Response response = new Response(200, TypeRef.unknown(), null);

        assertThat(response.isDefault()).isFalse();
    }

    @Test
    void rejectsANullBody() {
        assertThatThrownBy(() -> new Response(200, null, "application/json"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsAMediaTypeMissingWhenThereIsActuallyABody() {
        // body.kind() != UNKNOWN means a schema was declared, so the media type it was
        // declared under must have been carried through too -- a null here is the reader
        // losing information, not a response that legitimately has none.
        assertThatThrownBy(() -> new Response(200, TypeRef.primitive("string", null), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
