# wiremock-stubgen

[![build](https://github.com/ValentinKarnaukhov/wiremock-stubgen/actions/workflows/build.yml/badge.svg)](https://github.com/ValentinKarnaukhov/wiremock-stubgen/actions/workflows/build.yml)
[![License: Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange)](#building--testing)

Generate **WireMock stub builders from your OpenAPI specification** instead of
writing and maintaining stub mappings by hand.

Describe your API once in OpenAPI. The Maven plugin generates the stub classes,
request matchers, response builders, and nested body accessors for you. In your
tests, you only provide the values that matter.

## Before and after

Suppose the API contains this operation:

```yaml
paths:
  /books/{bookId}:
    get:
      operationId: getBook
      parameters:
        - name: bookId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Book'
components:
  schemas:
    Book:
      type: object
      properties:
        title:
          type: string
        author:
          $ref: '#/components/schemas/Author'
    Author:
      type: object
      properties:
        name:
          type: string
        country:
          type: string
```

### Without wiremock-stubgen

You have to maintain the URL, HTTP method, response status, headers, JSON
serialization, and every nested JSON field yourself:

```java
stubFor(get(urlPathEqualTo("/books/978-0201616224"))
    .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("""
            {
              "title": "The Pragmatic Programmer",
              "author": {
                "name": "Andrew Hunt",
                "country": "US"
              }
            }
            """)));
```

This is easy to get wrong and needs to be updated manually whenever the
contract changes.

### With wiremock-stubgen

The same contract generates `GetBookStub`, including the path parameter and
nested body methods:

```java
new GetBookStub(target)
    .pathBookId("978-0201616224")
    .code200()
        .title("The Pragmatic Programmer")
        .authorName("Andrew Hunt")
        .authorCountry("US")
    .mock();
```

There is no hand-written JSON, URL construction, status code, or WireMock
mapping to maintain. The generated code takes care of registering the stub.
The fluent API is also type-safe: renaming `bookId`, removing `author.country`,
or changing the operation in the OpenAPI document makes the affected test fail
at compile time instead of producing a mysterious `404` at runtime.

For request bodies, generated builders can match only the fields a test cares
about:

```java
new BorrowBookStub(target)
    .requestBody()
        .bookId("978-0201616224")
        .borrowerEmail("reader@example.com")
        .exit()
    .code201()
        .id("loan-1")
    .mock();
```

The generated matcher checks `bookId` and `borrower.email`, while leaving other
request fields, such as `days`, unconstrained.

## Getting started

The following setup is enough for a Maven project that already has an OpenAPI
specification. Copy the plugin and dependency configuration, adjust the paths
and package names, and run your tests.

### 1. Add the Maven plugins

The OpenAPI Generator creates the model classes. `wiremock-stubgen` uses the
same specification and those models to create the WireMock stubs.

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.openapitools</groupId>
            <artifactId>openapi-generator-maven-plugin</artifactId>
            <version>7.24.0</version>
            <executions>
                <execution>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                    <configuration>
                        <inputSpec>${project.basedir}/src/main/openapi/my-api.yaml</inputSpec>
                        <generatorName>java</generatorName>
                        <library>resttemplate</library>
                        <modelPackage>com.example.client.model</modelPackage>
                        <generateApis>true</generateApis>
                    </configuration>
                </execution>
            </executions>
        </plugin>

        <plugin>
            <groupId>io.github.valentinkarnaukhov</groupId>
            <artifactId>wiremock-stubgen-maven-plugin</artifactId>
            <version>1.0.0</version>
            <executions>
                <execution>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                    <configuration>
                        <inputSpec>${project.basedir}/src/main/openapi/my-api.yaml</inputSpec>
                        <stubPackage>com.example.client.stubs</stubPackage>
                        <modelPackage>com.example.client.model</modelPackage>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

`modelPackage` must point to the model classes generated by
`openapi-generator`, or to compatible model classes already present in your
project. `wiremock-stubgen` does not generate a second copy of your models.

### 2. Add the test dependencies

Generated stubs are test sources by default, so add the runtime and WireMock
dependencies with test scope:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.valentinkarnaukhov</groupId>
        <artifactId>wiremock-stubgen-runtime-java</artifactId>
        <version>1.0.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.wiremock</groupId>
        <artifactId>wiremock</artifactId>
        <version>3.13.1</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Run `mvn generate-test-sources`, `mvn test`, or any later Maven phase. The
plugin generates one stub class per operation under
`target/generated-test-sources/wiremock-stubgen` and registers that directory as
a test source root automatically.

### 3. Use the generated stubs

```java
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.valentinkarnaukhov.wiremockstubgen.runtime.StubTarget;

WireMockServer server = new WireMockServer(options().dynamicPort());
server.start();

try {
    StubTarget target = StubTarget.of(server);

    new GetBookStub(target)
        .pathBookId("978-0201616224")
        .code200()
            .title("The Pragmatic Programmer")
            .authorName("Andrew Hunt")
        .mock();

    // Run the code under test. It will receive the generated response.
} finally {
    server.stop();
}
```

`StubTarget` is the small hand-written runtime API consumed by generated
stubs. It can target an in-process `WireMockServer` or a remote WireMock
instance through its admin API. See
[`wiremock-stubgen-runtime-java`](wiremock-stubgen-runtime-java) for
`StubTarget.of(WireMock)` and the `BodySerializer` customization point.

## What gets generated

- One stub builder for each OpenAPI operation.
- Typed path, query, header, and cookie parameter methods.
- Response builders for every declared response status.
- Field-by-field request matchers and response body builders for generated
  models, including nested properties.
- The correct HTTP method, URL, status, content type, and WireMock mapping.

The generated stubs mirror the client rather than making assumptions from the
specification alone. Query parameter list formats, `OffsetDateTime` formatting,
media types, and other wire-format decisions are measured against the
`openapi-generator` Java client's behavior.

## Features

- **Generate instead of maintain.** Your OpenAPI specification is the source of
  truth; test scaffolding is produced automatically.
- **Less test code.** Set only the parameters and body fields relevant to a
  scenario. There is no hand-written JSON or repetitive WireMock setup.
- **Compile-time contract safety.** A renamed parameter, removed operation, or
  deleted body field breaks the test where the generated API is used.
- **Field-by-field bodies.** Match request bodies and build response bodies
  without asserting on fields a test does not care about.
- **No duplicate models.** Generated stubs compile against the model classes
  your existing OpenAPI Generator build already produces.
- **Graceful escape hatches.** Without `modelPackage`, or for unsupported body
  shapes, generation falls back to a whole-object form instead of making the
  project unusable.
- **Extensible targets.** Language targets are discovered with `ServiceLoader`,
  so another target can be added as a module without changing the core or build
  tool plugins.

## How it differs from existing approaches

| Approach | What you maintain | Contract drift is caught |
|---|---|---|
| Prism, Microcks, MockServer, Mockoon | A dynamic mock server configuration | At runtime, if at all |
| `openapi-generator`'s `java-wiremock` | Generated mappings with string parameters | Usually not at compile time |
| **wiremock-stubgen** | The OpenAPI specification | **At compile time, as a bonus** |

## Configuration options

Every option below is a `<configuration>` element of the plugin's `generate`
goal.

| Option | Default | Meaning |
|---|---|---|
| `inputSpec` | — (required) | The OpenAPI document to read. |
| `stubPackage` | `io.github.valentinkarnaukhov.wiremockstubgen.generated` | Package for generated stubs. |
| `modelPackage` | *(none)* | Package containing the consumer's model classes. If omitted, body values use the whole-object form. |
| `grouping` | `TAG` | `TAG` places each operation in a sub-package named after its first tag; `NONE` places all stubs directly in `stubPackage`. |
| `explode` | `true` | Generates field-by-field body APIs in addition to whole-object forms. Set to `false` to keep only whole-object bodies. |
| `maxDepth` | `5` | Maximum number of nested property hops to flatten before falling back to a whole-object form. |
| `composition` | `MERGE` | How `oneOf` and `anyOf` schemas are read. `MERGE` flattens alternatives; `OPAQUE` keeps the model as a whole object. |
| `outputDirectory` | `${project.build.directory}/generated-test-sources/wiremock-stubgen` | Directory for generated `.java` files. |
| `addTestCompileSourceRoot` | `true` | Registers `outputDirectory` as a test source root. |
| `addCompileSourceRoot` | `false` | Registers it as a main source root. Mutually exclusive with `addTestCompileSourceRoot`. |
| `options` | *(empty map)* | Passes target-specific options through to the selected language target. |

## Test scope vs. shared client libraries

By default, generated stubs are test scaffolding. They are written to a test
source directory and the runtime dependencies are declared with test scope.
This is the recommended setup when a service tests its own handlers or clients.

If you publish a client library and want its consumers to use the generated
stubs, make the following changes:

1. Set `addCompileSourceRoot` to `true` and `addTestCompileSourceRoot` to
   `false`.
2. Use compile scope for `wiremock-stubgen-runtime-java` and `org.wiremock:wiremock`
   so they are available transitively to consumers.
3. Keep generation bound before `compile` (the plugin's default phase is
   `generate-sources`).

## Known limitations

- A response declared under a non-JSON media type gets the correct
  `Content-Type` header, but its body is still matched and built as JSON.
- Request bodies declared as non-JSON are not covered end to end yet.
- `JsonNullable` fields (`openapi-generator`'s `useJsonNullable`) are not
  covered end to end yet.
- For nested lists of objects in request matchers, conditions produced by a
  nested list scope are independent rather than being folded into the enclosing
  list condition. Such combinations may be unreliable in WireMock.

## Modules

| Module | Purpose |
|---|---|
| `wiremock-stubgen-core` | Language-neutral OpenAPI representation and the `LanguageTarget` contract. |
| `wiremock-stubgen-codegen-java` | Java type mapping and source emission. Runs at generation time. |
| `wiremock-stubgen-runtime-java` | Runtime API used by generated Java stubs. |
| `wiremock-stubgen-maven-plugin` | Maven integration and source generation. |
| `wiremock-stubgen-fixtures` | Specifications used by the test suite. |
| `wiremock-stubgen-example` | A complete consumer-style example. |

`codegen-java` reads specifications and writes source; it is not placed on the
consumer's classpath. `runtime-java` is the published API that generated code
compiles against. Language targets are discovered with `ServiceLoader`, so
supporting another language does not require changes to the core or Maven
plugin. Gradle delivery can use the same target mechanism.

See [`wiremock-stubgen-example`](wiremock-stubgen-example) for a complete
buildable project: OpenAPI Generator creates the models, this plugin creates
the stubs, and tests exercise them against a live WireMock server.

## Building & testing

Requires JDK 17 or later; CI also runs on JDK 21.

```bash
mvn verify
```

The modules with production logic (`core`, `codegen-java`, `runtime-java`, and
`maven-plugin`) enforce minimum JaCoCo coverage at `verify`.

## Contributing

Issues and pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md)
for the contribution workflow and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for
community guidelines. Security issues should be reported through
[SECURITY.md](SECURITY.md), not a public issue.

## Roadmap

1. Close the gaps under [Known limitations](#known-limitations).
2. Extend golden-file coverage from the current fixture operations towards all
   supported operations.
3. Add a Gradle plugin using the existing `LanguageTarget` service-provider
   mechanism.

## License

Apache License 2.0 — see [LICENSE](LICENSE).
