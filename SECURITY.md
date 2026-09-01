# Security Policy

## Reporting a vulnerability

Please don't open a public GitHub issue for a security concern. Instead,
use [GitHub's private vulnerability reporting](https://github.com/ValentinKarnaukhov/wiremock-stubgen/security/advisories/new)
on this repository, or email **valentin.karnaukhovv@gmail.com** with details
and, if possible, a minimal OpenAPI fragment that reproduces the issue.

Expect an acknowledgment within a few days. This is a small project
maintained outside working hours, so response time isn't guaranteed, but
reports are taken seriously and a fix is prioritized over other work once
confirmed.

## Scope

This project generates source code and a runtime library that mediates
between generated code and WireMock. Relevant reports include anything that
could make a generated stub match a request it shouldn't, silently drop a
constraint the specification declared, or execute code the specification
didn't ask for. It does not include vulnerabilities in `openapi-generator`,
WireMock, or Jackson themselves — report those upstream.

## Supported versions

Only the latest published version is supported. There is no long-term
support branch at this stage.
