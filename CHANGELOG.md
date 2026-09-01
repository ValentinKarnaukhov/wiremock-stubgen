# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
intends to follow [Semantic Versioning](https://semver.org/) once it reaches
`1.0.0`.

## [Unreleased]

Initial public release in progress — see the [Roadmap](README.md#roadmap)
for what's still open.

### Added

- OpenAPI → type-safe WireMock stub builder generation for Java, delivered
  as a Maven plugin (`wiremock-stubgen-maven-plugin`).
- Field-by-field body accessors for requests and responses (`explode`),
  generated up to a configurable nesting depth.
- `TAG`/`NONE` stub grouping, `MERGE`/`OPAQUE` handling for `oneOf`/`anyOf`
  compositions, and configurable output package/directory/source-root scope.
- Hand-written runtime (`wiremock-stubgen-runtime-java`) shipped separately
  from generated code, with its own compatibility contract.
