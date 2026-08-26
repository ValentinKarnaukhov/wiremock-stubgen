# wiremock-stubgen

Generates **type-safe WireMock stub builders** from OpenAPI specifications.

## Why

Stubbing an HTTP dependency with WireMock is stringly typed. A renamed query
parameter or a removed operation does not break the build — it produces a stub that
silently never matches, and a confusing `404` somewhere far from the cause.

`wiremock-stubgen` derives builders from the specification, so a contract change
becomes a **compile error** in the tests that depend on it.

```java
new UserApiStub.GetUserById(target)
    .pathId(42)
    .queryExpand("address")
    .code200().body(new User().name("Alice"))
    .mock();
```

## How it differs from what already exists

| | Approach | Contract drift is caught |
|---|---|---|
| Prism, Microcks, MockServer, Mockoon | dynamic mock server from the spec | at runtime, if at all |
| `openapi-generator`'s `java-wiremock` | code generation, every parameter a `String` | not at all |
| **wiremock-stubgen** | code generation, typed per operation | **at compile time** |

## Status

Early development. The module layout and extension points are in place; the
specification reader, the explode resolver and the emitter are being built.

## Modules

| Module | Purpose |
|---|---|
| `wiremock-stubgen-core` | Language-neutral: OpenAPI → a description of the API, plus the `LanguageTarget` contract |
| `wiremock-stubgen-codegen-java` | Java target: type mapping and source emission. Runs at generate time only |
| `wiremock-stubgen-runtime-java` | Hand-written runtime the generated Java code builds on. Ships to the consumer |
| `wiremock-stubgen-maven-plugin` | Maven delivery |
| `wiremock-stubgen-it` | Integration tests: generate, compile, run against WireMock |

The two `-java` modules share nothing but the word. `codegen-java` reads specifications
and writes source; it never reaches the consumer's classpath. `runtime-java` is what
generated code compiles against, so it is a published API with a compatibility
obligation, and it depends on nothing but WireMock.

Language targets are discovered with `ServiceLoader`, so supporting another language
means adding a module — the core and the build-tool plugins stay untouched. Gradle
delivery is planned alongside the existing Maven plugin.

`wiremock-stubgen-runtime-java` declares WireMock as `provided`: consumers supply
their own version, including patched or internally forked builds.

## Building

Requires JDK 17 or later.

```bash
mvn verify
```

## License

Apache License 2.0 — see [LICENSE](LICENSE).
