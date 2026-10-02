#!/bin/sh
#
# Uploads this build's dSYMs to PostHog, so a crash report from a release build arrives with
# readable frames. Called by the "Upload PostHog Debug Symbols" build phase in iosApp.xcodeproj.
#
# Every way this can fail to be set up exits 0 rather than breaking the build: a developer who only
# builds Debug, or has no credentials, should not have a red Xcode build over symbols they do not
# need. A failure once those are in place is real, and is left to fail.
#
# See docs/error-reporting.md.

if [ "${CONFIGURATION:-}" != "Release" ]; then
    exit 0
fi

export PATH="/opt/homebrew/bin:/usr/local/bin:${HOME}/.posthog:${PATH}"
POSTHOG_CLI_PATH="${POSTHOG_CLI_PATH:-$(command -v posthog-cli)}"
if [ -z "${POSTHOG_CLI_PATH}" ]; then
    echo "PostHog: posthog-cli is not installed, skipping symbol upload (npm install -g @posthog/cli)"
    exit 0
fi

# Either the key is in the environment, or the CLI holds a login from `posthog-cli login`.
if [ -z "${POSTHOG_CLI_API_KEY:-}" ] && [ ! -d "${HOME}/.posthog" ]; then
    echo "PostHog: no POSTHOG_CLI_API_KEY and no stored login, skipping symbol upload"
    exit 0
fi

# Read from the built Info.plist rather than from MARKETING_VERSION and friends: this project does
# not define those build settings, so the script would otherwise upload empty versions.
PLIST="${BUILT_PRODUCTS_DIR}/${INFOPLIST_PATH}"
RELEASE_NAME=$(/usr/libexec/PlistBuddy -c "Print :CFBundleIdentifier" "${PLIST}")
RELEASE_VERSION=$(/usr/libexec/PlistBuddy -c "Print :CFBundleShortVersionString" "${PLIST}")
BUILD=$(/usr/libexec/PlistBuddy -c "Print :CFBundleVersion" "${PLIST}")

echo "PostHog: uploading debug symbols for ${RELEASE_NAME} ${RELEASE_VERSION} (${BUILD})"

"${POSTHOG_CLI_PATH}" dsym upload \
    --directory "${DWARF_DSYM_FOLDER_PATH}" \
    --main-dsym "${DWARF_DSYM_FILE_NAME}" \
    --release-name "${RELEASE_NAME}" \
    --release-version "${RELEASE_VERSION}" \
    --build "${BUILD}"
