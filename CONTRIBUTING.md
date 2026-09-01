# Contributing

## Working method

This is the bar a change to this project is held to, not a formality:

- **Measure, don't assume.** Anything about what `openapi-generator`'s
  generated client actually sends, or what WireMock actually matches, is
  worth a throwaway probe against the real thing before it is coded — several
  fixes in this project's history exist because an earlier assumption turned
  out to be wrong.
- **A passing test is not proof.** Show that a test fails under a specific,
  described mutation of the code it is meant to cover, not only that it
  passes.
- **Update the golden files, or add one.** `wiremock-stubgen-codegen-java`'s
  hand-written goldens are the contract for generated source shape; a change
  that alters output belongs there too. Only 5 of 17 fixture operations have
  one today — see the [Roadmap](README.md#roadmap).

## Before you open a pull request

```bash
mvn verify
```

runs the full build: every module's tests, and — for `core`, `codegen-java`,
`runtime-java`, and `maven-plugin` — a JaCoCo coverage threshold that fails
the build on a meaningful drop. Fix failures locally; CI runs the same
command on JDK 17 and 21.

Keep changes scoped to one concern. A behavior change and a refactor in the
same pull request make both harder to review.

## Reporting a bug

Include the OpenAPI fragment that triggers it (or a link to
`wiremock-stubgen-fixtures`' sample spec plus the operation/schema name), the
command or plugin configuration you ran, and what you expected instead of
what happened. A generated-source diff is more useful than a description of
one.

## Security issues

Don't open a public issue for those — see [SECURITY.md](SECURITY.md).
