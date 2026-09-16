# wiremock-stubgen

[![build](https://github.com/ValentinKarnaukhov/wiremock-stubgen/actions/workflows/build.yml/badge.svg)](https://github.com/ValentinKarnaukhov/wiremock-stubgen/actions/workflows/build.yml)
[![License: Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange)](#building--testing)

Generate WireMock stubs from your OpenAPI contract instead of writing and
maintaining boilerplate by hand.

`wiremock-stubgen` turns your API spec into Java stub builders that are fluent,
typed, and aligned with the exact shape of the generated client models. The code
is produced for you; your tests only set the fields that matter.

```java
new GetBookStub(target)
    .pathBookId("978-0201616224")
    .code200()
        .title("The Pragmatic Programmer")
        .authorName("Andrew Hunt")
        .authorCountry("US")
    .mock();
```

No hand-written JSON, no brittle URL strings, no repeated WireMock boilerplate,
and no silent `404` because a renamed field or removed operation was never caught
by the compiler.

## Why this exists

Testing an HTTP client usually means writing stubbed responses by hand. That is
fragile:

- renamed query params or path variables still compile, but never match;
- removed response fields silently drift from the real API;
- request bodies are often asserted as a giant JSON string;
- every contract change turns into a manual grep-and-edit exercise.

`wiremock-stubgen` generates stubs from the OpenAPI document itself, so the test
code stays short and the contract stays checked by the compiler.

## Before and after

### Without wiremock-stubgen

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

### With wiremock-stubgen

```java
new GetBookStub(target)
    .pathBookId("978-0201616224")
    .code200()
        .title("The Pragmatic Programmer")
        .authorName("Andrew Hunt")
        .authorCountry("US")
    .mock();
```

You still get compile-time safety as a bonus, but the real win is that you do
not have to write the stub mapping by hand.

## From spec to stub

<table>
<tr>
<th>OpenAPI (abridged)</th>
<th>Generated stub, in use</th>
<th>The stub mapping it registers</th>
</tr>
<tr>
<td>

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
```

</td>
<td>

```java
new GetBookStub(target)
    .pathBookId("978-0201616224")
    .code200()
    .title("The Pragmatic Programmer")
    .authorName("Andrew Hunt")
    .mock();
```

`pathBookId`, `title`, and `authorName` are generated because they exist in the
contract. Rename or remove them in the spec and the call stops compiling at the
point where it is used.

</td>
<td>

```json
{
  "request": {
    "urlPathTemplate": "/books/{bookId}",
    "method": "GET",
    "pathParameters": {
      "bookId": { "equalTo": "978-0201616224" }
    }
  },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "body": "{\"title\":\"The Pragmatic Programmer\",\"author\":{\"name\":\"Andrew Hunt\"}}"
  }
}
```

This is the actual `StubMapping` WireMock receives, generated for you.

</td>
</tr>
<tr>
<td>

```yaml
paths:
  /loans:
    post:
      operationId: borrowBook
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LoanRequest'
      responses:
        '201':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Loan'

components:
  schemas:
    LoanRequest:
      type: object
      required: [ bookId ]
      properties:
        bookId:
          type: string
        borrower:
          $ref: '#/components/schemas/Borrower'
        days:
          type: integer
```

</td>
<td>

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

The matcher checks only the fields a test cares about — the generated request
body builder ignores `days` unless the test mentions it.

</td>
<td>

```json
{
  "request": {
    "urlPath": "/loans",
    "method": "POST",
    "bodyPatterns": [
      { "matchesJsonPath": { "expression": "$['bookId']", "equalTo": "978-0201616224" } },
      { "matchesJsonPath": { "expression": "$['borrower']['email']", "equalTo": "reader@example.com" } }
    ]
  },
  "response": {
    "status": 201,
    "headers": { "Content-Type": "application/json" },
    "body": "{\"id\":\"loan-1\"}"
  }
}
```

Notice how the generated matcher only asserts on the fields that were requested.

</td>
</tr>
</table>

## Getting started

### 1. Add the plugin and OpenAPI generator

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

### 2. Add the runtime dependencies

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

### 3. Use the generated API in a test

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
} finally {
    server.stop();
}
```

The plugin generates one stub class per operation. By default, they land under
`target/generated-test-sources/wiremock-stubgen`, and Maven registers that
folder as a test source root automatically.

## Features

- Generate WireMock stub builders from an OpenAPI document.
- Keep tests concise and focused on what matters in the scenario.
- Reduce manual JSON and URL boilerplate.
- Match request bodies field by field instead of asserting a whole JSON string.
- Build response bodies in a typed, nested way.
- Catch contract drift at compile time: renamed parameter, removed operation,
  missing field, or breaking schema change.
- Reuse the same model classes generated by `openapi-generator` instead of
  duplicating them.
- Keep escape hatches for cases where the schema shape is unusual or not fully
  flattened.

## How it differs from other approaches

| Approach | Contract drift is caught | What you still have to maintain |
|---|---|---|
| Dynamic mock server from the spec | At runtime, if at all | Mock configuration and server logic |
| `openapi-generator`'s `java-wiremock` | Usually not at compile time | Many string-based params and manual mappings |
| **wiremock-stubgen** | **At compile time** | Only the OpenAPI spec itself |

## Configuration options

Every option below is a `<configuration>` element of the plugin's `generate`
goal.

| Option | Default | Meaning |
|---|---|---|
| `inputSpec` | — (required) | OpenAPI document to read. |
| `stubPackage` | `io.github.valentinkarnaukhov.wiremockstubgen.generated` | Generated stub package. |
| `modelPackage` | *(none)* | Package of the consumer's model classes. |
| `grouping` | `TAG` | Group stubs by first tag or put them directly in `stubPackage`. |
| `explode` | `true` | Generate field-by-field body builders in addition to whole-object forms. |
| `maxDepth` | `5` | Nested property depth before falling back to a whole-object form. |
| `composition` | `MERGE` | How `oneOf` and `anyOf` schema alternatives are read. |
| `outputDirectory` | `${project.build.directory}/generated-test-sources/wiremock-stubgen` | Output directory. |
| `addTestCompileSourceRoot` | `true` | Register generated sources as test sources. |
| `addCompileSourceRoot` | `false` | Register them as main sources instead. |
| `options` | *(empty map)* | Pass through target-specific options. |

## Test scope vs shared client libraries

By default the generated stubs are test scaffolding. This is the simplest setup
for service tests and keeps WireMock out of published runtime artifacts.

If you publish a client library, flip the configuration so the generated stubs
travel with the library itself:

1. set `addCompileSourceRoot` to `true` and `addTestCompileSourceRoot` to `false`;
2. move `wiremock-stubgen-runtime-java` and `wiremock` to `compile` scope;
3. keep generation before `compile`.

Then consumers can write exactly the same generated stub calls without bringing
their own test-only setup.

## Modules

| Module | Purpose |
|---|---|
| `wiremock-stubgen-core` | Language-neutral OpenAPI representation and the `LanguageTarget` contract. |
| `wiremock-stubgen-codegen-java` | Java type mapping and source emission. |
| `wiremock-stubgen-runtime-java` | Runtime API consumed by generated Java stubs. |
| `wiremock-stubgen-maven-plugin` | Maven integration and generation step. |
| `wiremock-stubgen-fixtures` | OpenAPI fixtures used by the test suite. |
| `wiremock-stubgen-example` | End-to-end example project for consumers. |

`wiremock-stubgen-example` is the best place to start if you want to see the
whole workflow in practice: openapi-generator produces the model classes, the
plugin generates the stubs, and they are exercised against a live WireMock
server.

## Building & testing

Requires JDK 17 or later; CI also runs on JDK 21.

```bash
mvn verify
```

The modules with production logic enforce a minimum JaCoCo coverage threshold at
`verify`, so a change that meaningfully drops coverage fails the build instead
of slipping in quietly.

## Known limitations

- A response declared under a non-JSON media type gets the right `Content-Type`
  header, but the body is still matched and built as JSON.
- Request bodies declared as non-JSON are not covered end to end yet.
- `JsonNullable` fields (`openapi-generator`'s `useJsonNullable`) are not
  covered end to end yet.
- Nested list conditions in request-body matchers can still be tricky when a
  nested object scope has to be reasoned about independently.

## Contributing

Issues and pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for
the contribution workflow, [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for
community expectations, and [SECURITY.md](SECURITY.md) for security reporting.

## Roadmap

1. Close the gaps in the known limitations.
2. Extend golden-file coverage across more fixture operations.
3. Add a Gradle plugin using the same `LanguageTarget` mechanism already in
   place for target discovery.

## License

Apache License 2.0 — see [LICENSE](LICENSE).
