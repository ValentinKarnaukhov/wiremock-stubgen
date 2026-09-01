# Releasing

This is the maintainer's runbook for cutting a release. Contributors don't
need this — see [CONTRIBUTING.md](CONTRIBUTING.md) instead.

## One-time setup (before the first release)

1. A [Sonatype Central](https://central.sonatype.com) account with the
   `io.github.valentinkarnaukhov` namespace verified (via a GitHub OAuth
   check on that account).
2. A GPG key pair. The `release` Maven profile signs every artifact with
   `maven-gpg-plugin` before publishing — Central rejects unsigned ones.
3. In the GitHub repository's **Settings → Secrets and variables → Actions**,
   add:
   - `CENTRAL_USERNAME` / `CENTRAL_PASSWORD` — a token generated on the
     Central Portal (not your account password).
   - `GPG_PRIVATE_KEY` — the armored private key (`gpg --export-secret-keys
     --armor KEYID`).
   - `GPG_PASSPHRASE` — the key's passphrase.

Do this once; every release after that is just steps below.

## Cutting a release

1. On `main`, confirm CI is green and `CHANGELOG.md` has a dated
   `## [X.Y.Z]` section (not `[Unreleased]`) describing what's in it.
2. Confirm every module's `pom.xml` is on `X.Y.Z` with no `-SNAPSHOT` suffix:

   ```bash
   grep -r '<version>' --include=pom.xml . | grep SNAPSHOT
   ```

   should print nothing. If it does, fix it (see
   [Bumping the version](#bumping-the-version) below) and commit first.
3. Run the full build one last time: `mvn clean verify`.
4. Tag the release commit and push the tag:

   ```bash
   git tag -a vX.Y.Z -m "Release X.Y.Z"
   git push origin vX.Y.Z
   ```

   Pushing the tag triggers `.github/workflows/release.yml`, which builds,
   signs, and publishes to Maven Central, then creates a GitHub Release
   using that version's `CHANGELOG.md` section as the release notes.
5. Watch the **release** workflow run to completion. A failed signing or
   publish step means nothing reached Central — delete the tag
   (`git push --delete origin vX.Y.Z`), fix the problem, and re-tag.
6. Once Central shows the artifact (can take a few minutes to sync), prepare
   `main` for the next round of development:

   ```bash
   mvn versions:set -DnewVersion=X.(Y+1).0-SNAPSHOT -DprocessAllModules
   mvn versions:commit
   ```

   Add a new empty `## [Unreleased]` section above the release you just
   shipped in `CHANGELOG.md` if the release workflow's commit didn't already
   leave one (it doesn't touch the changelog — only add one if it's missing).
   Commit as `Prepare next development iteration` and push to `main`.

## Bumping the version

Either edit each `pom.xml`'s `<version>` by hand (all seven currently share
one version, so a project-wide find-and-replace works), or use the Versions
Maven Plugin:

```bash
mvn versions:set -DnewVersion=X.Y.Z -DprocessAllModules
mvn versions:commit
```

`versions:commit` removes the `.versionsBackup` files the plugin leaves
behind; don't skip it.

## Choosing the version number

Follows [Semantic Versioning](https://semver.org/):

- **Patch** (`X.Y.Z+1`): a bug fix with no change to generated output shape
  or the runtime's public API.
- **Minor** (`X.Y+1.0`): a new capability that doesn't break existing
  generated stubs or runtime callers — a new `@Parameter`, a new target
  language, wider OpenAPI support.
- **Major** (`X+1.0.0`): anything that changes what already-generated code
  looks like, or breaks a `wiremock-stubgen-runtime-java` public method
  signature. Regenerating and recompiling is an acceptable cost for a major
  version; silently changing behavior under an existing version is not.
