package io.github.valentinkarnaukhov.wiremockstubgen.spec;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A single API operation — the unit a generated stub builder corresponds to.
 *
 * @param operationId stable identifier; synthesised from method and path when the spec omits it
 * @param tag         grouping key; falls back to the first path segment when the spec has no tags
 * @param path        templated path, for example {@code /users/{id}}
 * @param method      HTTP method
 * @param parameters  path, query, header and cookie parameters
 * @param requestBody request body type, or {@code null} when the operation takes no body
 * @param responses   documented responses
 */
public record Operation(
        String operationId,
        String tag,
        String path,
        HttpMethod method,
        List<Parameter> parameters,
        TypeRef requestBody,
        List<Response> responses) {

    public Operation {
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(method, "method");
        parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
        responses = List.copyOf(Objects.requireNonNull(responses, "responses"));
    }

    public List<Parameter> parametersIn(ParameterLocation location) {
        return parameters.stream().filter(p -> p.location() == location).toList();
    }

    public Optional<TypeRef> requestBodyIfPresent() {
        return Optional.ofNullable(requestBody);
    }
}
