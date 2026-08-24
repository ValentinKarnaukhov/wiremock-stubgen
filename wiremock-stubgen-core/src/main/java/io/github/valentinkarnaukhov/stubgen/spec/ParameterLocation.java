package io.github.valentinkarnaukhov.stubgen.spec;

/**
 * Where a parameter travels in the request. Mirrors the OpenAPI {@code in} keyword.
 */
public enum ParameterLocation {
    PATH,
    QUERY,
    HEADER,
    COOKIE
}
