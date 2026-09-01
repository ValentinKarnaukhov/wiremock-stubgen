package io.github.valentinkarnaukhov.wiremockstubgen.spec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParameterTest {

    @Test
    void acceptsACollectionFormatOnAnArrayType() {
        Parameter parameter = new Parameter("tags", ParameterLocation.QUERY,
                TypeRef.array(TypeRef.primitive("string", null)), CollectionFormat.MULTI);

        assertThat(parameter.collectionFormat()).isEqualTo(CollectionFormat.MULTI);
    }

    @Test
    void acceptsNoneOnAnythingThatIsNotAnArray() {
        Parameter parameter = new Parameter("id", ParameterLocation.PATH,
                TypeRef.primitive("string", null), CollectionFormat.NONE);

        assertThat(parameter.collectionFormat()).isEqualTo(CollectionFormat.NONE);
    }

    @Test
    void rejectsACollectionFormatOnSomethingThatIsNotAnArray() {
        assertThatThrownBy(() -> new Parameter("id", ParameterLocation.PATH,
                TypeRef.primitive("string", null), CollectionFormat.CSV))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }

    @Test
    void rejectsNoneOnAnArray() {
        assertThatThrownBy(() -> new Parameter("tags", ParameterLocation.QUERY,
                TypeRef.array(TypeRef.primitive("string", null)), CollectionFormat.NONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tags");
    }

    @Test
    void rejectsNullComponents() {
        TypeRef type = TypeRef.primitive("string", null);
        assertThatThrownBy(() -> new Parameter(null, ParameterLocation.PATH, type, CollectionFormat.NONE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Parameter("id", null, type, CollectionFormat.NONE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Parameter("id", ParameterLocation.PATH, null, CollectionFormat.NONE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Parameter("id", ParameterLocation.PATH, type, null))
                .isInstanceOf(NullPointerException.class);
    }
}
