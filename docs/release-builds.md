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
## iOS release

Codemagic builds the signed iOS app and uploads it to TestFlight, from
[`codemagic.yaml`](../codemagic.yaml) at the repository root. Start the **iOS release** workflow from
the Codemagic dashboard; nothing triggers it automatically. Each build takes Codemagic's build
counter as `CURRENT_PROJECT_VERSION`; raise `MARKETING_VERSION` in
`app/iosApp/Configuration/Config.xcconfig` by hand.

It needs two things set up in Codemagic first:

- An App Store Connect API key (App Manager role) added under Team settings > Integrations >
  Developer Portal, named `NajAutoskola`. Codemagic uses it for the certificate, the provisioning
  profile and the TestFlight upload.
- An environment variable group named `posthog` with `POSTHOG_CLI_API_KEY` (secret),
  `POSTHOG_CLI_PROJECT_ID` and `POSTHOG_CLI_HOST`, for the dSYM upload.
