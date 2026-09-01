# wiremock-stubgen

[![build](https://github.com/ValentinKarnaukhov/wiremock-stubgen/actions/workflows/build.yml/badge.svg)](https://github.com/ValentinKarnaukhov/wiremock-stubgen/actions/workflows/build.yml)
[![License: Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange)](#building--testing)

Generates **type-safe WireMock stub builders** from OpenAPI specifications, so a
contract change is a compile error in your tests instead of a `404` nobody can
explain.

```java
new GetBookStub(target)
        .pathBookId("978-0201616224")
        .code200()
            .title("The Pragmatic Programmer")
            .authorName("Andrew Hunt")
            .authorCountry("US")
        .mock();
```

## Table of contents

- [Features](#features)
- [Why](#why)
- [From spec to stub](#from-spec-to-stub)
- [How it differs from what already exists](#how-it-differs-from-what-already-exists)
- [Status](#status)
- [Getting started](#getting-started)
  - [1. Build and install](#1-build-and-install)
  - [2. Generate models with openapi-generator, stubs with this plugin](#2-generate-models-with-openapi-generator-stubs-with-this-plugin)
  - [3. Use a stub in a test](#3-use-a-stub-in-a-test)
  - [Configuration options](#configuration-options)
  - [Where stubs end up: test scope vs. shared client libraries](#where-stubs-end-up-test-scope-vs-shared-client-libraries)
- [Known limitations](#known-limitations)
- [Modules](#modules)
- [Building & testing](#building--testing)
- [Contributing](#contributing)
- [Roadmap](#roadmap)
- [License](#license)

## Features

- **Compile-time contract checking.** A renamed parameter, a removed operation,
  or a body field that no longer exists breaks the build where the stub is
  used, not silently at test time.
- **Field-by-field bodies**, both ways: a request body matcher and a response
  body builder generated per schema, so a test states what it cares about
  instead of a hand-written JSON string that happens to parse.
- **Mirrors the client, not the spec.** Query parameter list formats,
  `OffsetDateTime` formatting, media types — every wire-format decision is
  measured against what `openapi-generator`'s own generated client actually
  sends, not assumed from reading the OpenAPI document alone.
- **No models of its own.** Stubs compile against the model classes your
  existing `openapi-generator` build already produces; nothing is duplicated
  or kept in sync by hand.
- **Escape hatches, not dead ends.** A missing `modelPackage`, `explode: false`,
  or an unusual body shape degrades to a working, less-typed stub rather than
  a build failure — see [Configuration options](#configuration-options).
- **Extensible by design.** Language targets are discovered with
  `ServiceLoader`; adding a target language is a new module, not a change to
  the core or the build-tool plugins.
- **Proven, not just tested.** Every behavioural test in this project is
  written to fail under a specific mutation of the code it covers — a
  passing test alone is not treated as proof.

## Why

Stubbing an HTTP dependency with WireMock is stringly typed. A renamed query
parameter or a removed operation does not break the build — it produces a stub that
silently never matches, and a confusing `404` somewhere far from the cause.

`wiremock-stubgen` derives builders from the specification, so a contract change
becomes a **compile error** in the tests that depend on it, and reads a body field
by field instead of a hand-written JSON string.

This is real, generated output — see [`wiremock-stubgen-example`](wiremock-stubgen-example)
for the specification it came from and the live test that exercises it against a
running WireMock server.

## From spec to stub

Two operations from that same example, abridged to fit here. The first column
is what you write once; the second is how a test uses what gets generated from
it; the third is the actual `StubMapping` JSON that call registers with
WireMock — nothing past the first column is hand-maintained.

<table>
<tr>
<th>OpenAPI (abridged)</th>
<th>Generated stub, in use</th>
<th>The stub mapping it registers</th>
</tr>
<tr>
<td>

A path parameter and a nested response body:

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
        200:
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

`pathBookId` exists because `bookId` is a path parameter; `title` and
`authorName` exist because `Book` has a `title` and an `author.name` — rename
or remove any of them in the spec and the corresponding call stops compiling
here, rather than the stub quietly matching nothing.

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

The actual `StubMapping` JSON WireMock registers, taken straight from
`.buildStub()`. Trimmed of the `id`/`uuid` WireMock assigns and of the empty
`tags`/`status`/etc. `Book` also declares but this call never set — nothing in
the body but what was asked for.

</td>
</tr>
<tr>
<td>

A request body matched field by field, ignoring everything it does not
mention:

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
        201:
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

Answers only a request whose body carries this `bookId` and this
`borrower.email` — `days` is never mentioned, so any value for it, or none,
still matches. `equalToJson` on a hand-written string could not express
"these two fields, whatever else is there" without also asserting on `days`.

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

Two independent `bodyPatterns`, which WireMock ANDs: nothing here checks
`days` at all, which is exactly how a request that includes it, and a request
that omits it, both still match.

</td>
</tr>
</table>

## How it differs from what already exists

| | Approach | Contract drift is caught |
|---|---|---|
| Prism, Microcks, MockServer, Mockoon | dynamic mock server from the spec | at runtime, if at all |
| `openapi-generator`'s `java-wiremock` | code generation, every parameter a `String` | not at all |
| **wiremock-stubgen** | code generation, typed per operation | **at compile time** |

## Status

Functional and covered by 230 tests, including live ones against a real WireMock
server, but not yet published anywhere — building it means building from source,
into your own local Maven repository, until that changes. See
[Getting started](#getting-started) for what that means in practice, and
[Known limitations](#known-limitations) for what is deliberately not supported yet.

## Getting started

### 1. Build and install

There is no published artifact yet, so the first step is always this, from the
repository root:

```bash
mvn install
```

That puts `io.github.valentinkarnaukhov:wiremock-stubgen-maven-plugin` and
`io.github.valentinkarnaukhov:wiremock-stubgen-runtime-java` — both at
`1.0.0` — into your local `~/.m2`, which is as far as a consuming project
can currently reach them. Nothing here is on Maven Central, and nothing is on
any shared Artifactory; a CI runner or a teammate's machine will not resolve
these coordinates until one of those changes.

### 2. Generate models with openapi-generator, stubs with this plugin

`wiremock-stubgen` does not generate model classes — it expects a client already
generated by `openapi-generator` (or written by hand) in the same build, and
imports those classes rather than inventing its own. Point `modelPackage` at
wherever that is:

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.24.0</version>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
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
            <goals><goal>generate</goal></goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/openapi/my-api.yaml</inputSpec>
                <stubPackage>com.example.client.stubs</stubPackage>
                <modelPackage>com.example.client.model</modelPackage>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Add the runtime the generated stubs compile against — test scope, to match
where the plugin puts the generated sources by default (see
[Where stubs end up](#where-stubs-end-up-test-scope-vs-shared-client-libraries)):

```xml
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
```

`mvn generate-sources` (or any later phase) now writes one stub class per
operation into `target/generated-test-sources/wiremock-stubgen`, registered as a
test source root automatically — nothing to add to `build-helper-maven-plugin`.

### 3. Use a stub in a test

```java
WireMockServer server = new WireMockServer(options().dynamicPort());
server.start();
StubTarget target = StubTarget.of(server);

new GetBookStub(target)
        .pathBookId("978-0201616224")
        .code200()
            .title("The Pragmatic Programmer")
        .mock();
```

`StubTarget` is the one hand-written type every generated stub takes — see
[`wiremock-stubgen-runtime-java`](wiremock-stubgen-runtime-java) for `of(WireMock)`
(a remote, already-running WireMock reached over its admin API) and for
`BodySerializer`, the escape hatch for a body a consumer's own Jackson
configuration needs and WireMock's bundled one does not.

### Configuration options

Every option below is a `<configuration>` element of the plugin's `generate` goal.

| Option | Default | Meaning |
|---|---|---|
| `inputSpec` | — (required) | The OpenAPI document to read. |
| `stubPackage` | `io.github.valentinkarnaukhov.wiremockstubgen.generated` | Package the generated stubs are written into. |
| `modelPackage` | *(none)* | Where the consumer's own model classes live. Left unset, every stub still compiles, but bodies are taken and sent whole as `Object`, with no field-by-field builders or matchers at all. |
| `grouping` | `TAG` | `TAG` puts each operation's stub in a sub-package named after its first tag; `NONE` puts every stub directly in `stubPackage`. |
| `explode` | `true` | Describe bodies field by field, in addition to the whole-object form. `false` keeps only the form that takes a model the caller already has. |
| `maxDepth` | `5` | How many property hops a body scope flattens through before giving up and falling back to the whole-object form; guards against a schema that is deeply nested or self-referential. |
| `composition` | `MERGE` | How a `oneOf`/`anyOf` schema is read. `MERGE` flattens every alternative into one class, matching `openapi-generator`'s own default reading. Set to `OPAQUE` if the model generator was run with `useOneOfInterfaces=true`, which turns such a schema into an interface with no setters a builder could call. |
| `outputDirectory` | `${project.build.directory}/generated-test-sources/wiremock-stubgen` | Where generated `.java` files are written. |
| `addTestCompileSourceRoot` | `true` | Registers `outputDirectory` as a test source root. |
| `addCompileSourceRoot` | `false` | Registers it as a main source root instead — see below. Mutually exclusive with `addTestCompileSourceRoot`. |
| `options` | *(empty map)* | Passed through to the language target untouched, for a setting specific to one target language rather than the core. |

### Where stubs end up: test scope vs. shared client libraries

By default a stub is test scaffolding: it is written under
`generated-test-sources`, registered as a *test* source root, and depends on
WireMock, which has no business in a published artifact. This is right for a
service testing its own handlers, and it is why `wiremock-stubgen-runtime-java`
and `org.wiremock:wiremock` are declared `test`-scoped above.

A **client library** — one published so that *other* services can stub the API
it wraps in *their* tests — needs the opposite: the stubs, and what they compile
against, have to travel in the jar other projects depend on. Three changes:

1. Set `addCompileSourceRoot` to `true` and `addTestCompileSourceRoot` to
   `false` (the plugin refuses both at once, since they claim the same
   directory for two different lifecycle phases).
2. Move the `wiremock-stubgen-runtime-java` and `wiremock` dependencies from
   `test` to `compile` scope, so they resolve transitively for whoever depends
   on this library.
3. Move the plugin's own execution to run before `compile` if it is not
   already — `generate-sources` is the goal's default phase, so this is usually
   already correct.

Consumers of that library then write `new GetBookStub(target)` exactly as
above, having pulled in the stub class, `wiremock-stubgen-runtime-java`, and
WireMock all transitively from one dependency.

## Known limitations

Deliberate, and each recorded at the point it was decided, in code comments
next to what it affects:

- A response declared under a non-JSON media type gets the right
  `Content-Type` header, but its body is still matched and built as JSON —
  there is nowhere else for it to come from.
- The same is untested for a *request* body declared as non-JSON; every
  request body this project generates against is declared as JSON.
- `JsonNullable`-typed fields (`openapi-generator`'s `useJsonNullable`) are
  untested end to end — this project's own example specification does not use
  one, so there is nothing generated to test against.
- Chaining several conditions on one element of a list of objects — inside a
  request body matcher — now requires all of them to hold on the *same*
  element, but a nested list of objects reached from inside that scope still
  produces its own independent condition rather than folding into the
  enclosing one; measured against a live WireMock server to be unreliable if
  attempted.

## Modules

| Module | Purpose |
|---|---|
| `wiremock-stubgen-core` | Language-neutral: OpenAPI → a description of the API, plus the `LanguageTarget` contract |
| `wiremock-stubgen-codegen-java` | Java target: type mapping and source emission. Runs at generate time only |
| `wiremock-stubgen-runtime-java` | Hand-written runtime the generated Java code builds on. Ships to the consumer |
| `wiremock-stubgen-maven-plugin` | Maven delivery |
| `wiremock-stubgen-fixtures` | The specification every layer is tested against |
| `wiremock-stubgen-example` | A project that uses the plugin the way a consumer would |

The two `-java` modules share nothing but the word. `codegen-java` reads specifications
and writes source; it never reaches the consumer's classpath. `runtime-java` is what
generated code compiles against, so it is a published API with a compatibility
obligation, and it depends on nothing but WireMock (and, for a consumer-supplied
`BodySerializer`, Jackson — both `provided`, so the consumer's own version decides).

Language targets are discovered with `ServiceLoader`, so supporting another language
means adding a module — the core and the build-tool plugins stay untouched. Gradle
delivery is planned alongside the existing Maven plugin.

`wiremock-stubgen-example` is where the plugin actually runs: openapi-generator emits
model classes from a small specification, the plugin generates stubs against them, and
the generated stubs are exercised against a live WireMock. Start there for a working,
buildable reference; this README's snippets are lifted from it.

## Building & testing

Requires JDK 17 or later; CI (see the badge above) also runs on 21.

```bash
mvn verify
```

Every module with real logic (`core`, `codegen-java`, `runtime-java`,
`maven-plugin`) enforces a minimum JaCoCo coverage threshold at `verify`, so a
change that meaningfully drops coverage fails the build rather than merging
quietly. `fixtures` and `example` deliberately carry no such check: the former
has no logic of its own to protect, and the latter's instructions are almost
entirely `openapi-generator`'s model classes and this project's own generated
stubs, neither of which it owns.

## Contributing

Issues and pull requests are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md)
for the working method this project holds changes to, and
[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for how the community runs. Security
issues go through [SECURITY.md](SECURITY.md), not a public issue.

## Roadmap

Roughly in order of how much currently blocks real use:

1. **Publish somewhere reachable** — the `release` Maven profile is ready; a
   git remote, a Central account, a GPG key and a `v1.0.0` tag are not.
2. Close the gaps under [Known limitations](#known-limitations).
3. Extend golden-file coverage from 5 of 17 fixture operations towards all of
   them.
4. A Gradle plugin alongside the existing Maven one, using the same
   `LanguageTarget` service-provider mechanism already in place for adding
   target languages.

## License

Apache License 2.0 — see [LICENSE](LICENSE).

