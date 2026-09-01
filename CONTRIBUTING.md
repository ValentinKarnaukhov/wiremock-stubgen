# Contributing

## 1. Set up your environment

1. Fork the repository and clone your fork.
2. Install JDK 17 or later (CI also runs on 21).
3. Build once to confirm everything works:

   ```bash
   mvn verify
   ```

   This builds every module, runs all tests, and enforces the JaCoCo coverage
   thresholds described in the [README](README.md#building--testing).

## 2. Find something to work on

- Check [open issues](https://github.com/ValentinKarnaukhov/wiremock-stubgen/issues)
  and the [Roadmap](README.md#roadmap).
- For anything larger than a small fix, open an issue first to agree on the
  approach before writing code.

## 3. Make your change

- Create a branch off `main`: `git checkout -b your-change-name`.
- Keep it to one concern. A behavior change and a refactor in the same pull
  request make both harder to review.
- See [Modules](README.md#modules) for which module owns what.
- If your change alters generated output, update the golden files in
  `wiremock-stubgen-codegen-java` (or add one — only 5 of 17 fixture
  operations have one today).
- Before coding against `openapi-generator`'s generated client or WireMock's
  matching behavior, run a throwaway probe against the real thing rather
  than assuming — several fixes in this project's history exist because an
  earlier assumption turned out to be wrong.

## 4. Test your change

```bash
mvn verify
```

must pass locally before you open a pull request; CI runs the same command
on JDK 17 and 21. For any new or fixed behavior, add a test and confirm it
fails under a specific, described mutation of the code it covers — a test
that only passes isn't proof it checks anything.

## 5. Submit the pull request

- Push your branch and open a pull request against `main`.
- Fill in the PR template: what changed and why, how it was verified, and
  the checklist.
- One approving review is required before merge.

## Reporting a bug

Open an issue using the bug report template. Include the OpenAPI fragment
that triggers it (or a pointer into `wiremock-stubgen-fixtures`' sample
spec), the command or plugin configuration you ran, and what you expected
instead of what happened. A generated-source diff is more useful than a
description of one.

## Security issues

Don't open a public issue for those — see [SECURITY.md](SECURITY.md).
