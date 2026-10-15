# Release builds

A release build is minified with R8, and its mapping file is
uploaded to PostHog so that crash reports stay readable. The upload needs `posthog-cli` on the
`PATH` and three values in the environment or in a `.env` file at the repository root (copy
[`.env.example`](.env.example)):

- `POSTHOG_CLI_API_KEY`: a personal API key with the error tracking write and organization read
  scopes.
- `POSTHOG_CLI_PROJECT_ID`: the numeric project ID.
- `POSTHOG_CLI_HOST`: `https://eu.posthog.com`.

Without them, the release build still succeeds and reports that it skipped the upload. Details are in
[docs/error-reporting.md](docs/error-reporting.md).

## Signing for Play

Play re-signs the app, so what you upload is signed with the *upload key*. The keystore is never in
the repo; the build reads it from the environment, and without it the release build is unsigned:

- `AUTOSKOLA_KEYSTORE_FILE`: path to `upload_key.jks`.
- `AUTOSKOLA_KEYSTORE_PASSWORD`: its password.
- `AUTOSKOLA_KEY_ALIAS`: defaults to `upload`.
- `AUTOSKOLA_KEY_PASSWORD`: only if it differs from the keystore password.

The **Release** workflow (Actions tab, or push a `v*` tag) does this with repository secrets
`UPLOAD_KEYSTORE_BASE64`, `UPLOAD_KEYSTORE_PASSWORD` and optionally `UPLOAD_KEY_PASSWORD`, and
leaves the signed `.aab` (what Play takes), a signed `.apk` and the R8 `mapping.txt` on the run page.
Raise `versionCode` in `app/androidApp/build.gradle.kts` before each upload.
## iOS

There is no signed iOS release build yet: it needs a paid Apple Developer account. Until then,
[`codemagic.yaml`](../codemagic.yaml) builds an unsigned Debug app for the iOS Simulator. Run the
**iOS simulator preview** workflow from the Codemagic dashboard and open the result with **Quick
launch** on the finished build's page (Codemagic's App Preview).

A Codemagic workflow that signs the app and uploads it to TestFlight was written and then removed;
`git log -- codemagic.yaml` finds it.
