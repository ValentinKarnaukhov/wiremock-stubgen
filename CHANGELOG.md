# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.0] - 2026-09-01

Initial public release.

### Added

- OpenAPI → type-safe WireMock stub builder generation for Java, delivered
  as a Maven plugin (`wiremock-stubgen-maven-plugin`).
- Field-by-field body accessors for requests and responses (`explode`),
  generated up to a configurable nesting depth.
- `TAG`/`NONE` stub grouping, `MERGE`/`OPAQUE` handling for `oneOf`/`anyOf`
  compositions, and configurable output package/directory/source-root scope.
- Hand-written runtime (`wiremock-stubgen-runtime-java`) shipped separately
  from generated code, with its own compatibility contract.

[Unreleased]: https://github.com/ValentinKarnaukhov/wiremock-stubgen/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/ValentinKarnaukhov/wiremock-stubgen/releases/tag/v1.0.0
