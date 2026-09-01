# Releasing NexRhythm

This document defines the release process for NexRhythm.

## Versioning

NexRhythm uses a semantic-style public version name:

`MAJOR.MINOR.PATCH[-PRERELEASE]`

The first public beta is:

`0.1.0-beta`

Git tags use the same version prefixed with `v`:

`v0.1.0-beta`

Android version rules:

- `versionName` matches the Git tag without the leading `v`.
- `versionCode` is an integer and must increase for every published Android build.
- A release tag must never be reused for different source code.

## Release Signing

Release APKs must be signed with the FRIZDEV NexRhythm release key.

The keystore must remain outside the repository and must be backed up securely.

Never commit:

- `.jks` files;
- `.keystore` files;
- keystore passwords;
- private signing credentials.

The Gradle release build reads these properties:

- `NEXRHYTHM_RELEASE_STORE_FILE`
- `NEXRHYTHM_RELEASE_STORE_PASSWORD`
- `NEXRHYTHM_RELEASE_KEY_ALIAS`
- `NEXRHYTHM_RELEASE_KEY_PASSWORD`

For environment variables, use Gradle's `ORG_GRADLE_PROJECT_` prefix.

Example property mapping:

```text
ORG_GRADLE_PROJECT_NEXRHYTHM_RELEASE_STORE_FILE
ORG_GRADLE_PROJECT_NEXRHYTHM_RELEASE_STORE_PASSWORD
ORG_GRADLE_PROJECT_NEXRHYTHM_RELEASE_KEY_ALIAS
ORG_GRADLE_PROJECT_NEXRHYTHM_RELEASE_KEY_PASSWORD
```

## GitHub Actions Secrets

The release workflow expects these repository secrets:

```text
NEXRHYTHM_KEYSTORE_BASE64
NEXRHYTHM_KEYSTORE_PASSWORD
NEXRHYTHM_KEY_ALIAS
NEXRHYTHM_KEY_PASSWORD
```

`NEXRHYTHM_KEYSTORE_BASE64` is the Base64-encoded release keystore.

The raw keystore must not be committed to the repository.

## Local Release Verification

Before creating a tag:

1. Confirm the working tree is clean.
2. Run unit tests.
3. Build the release APK with the release signing properties.
4. Verify the APK signature.
5. Confirm `versionName` matches the planned tag.
6. Confirm the CHANGELOG contains a section for that version.

The expected APK output is:

`nexrhythm_kotlin/app/build/outputs/apk/release/app-release.apk`

Release builds should be run with configuration cache disabled so signing secrets are not intentionally persisted in the project configuration cache.

## Tagging

For the first beta:

```powershell
git tag -a v0.1.0-beta -m "NexRhythm v0.1.0-beta"
git push origin v0.1.0-beta
```

Do not push the tag until the signed APK has been successfully built and verified locally.

## Automated GitHub Release

Pushing a `v*` tag triggers `.github/workflows/android-release.yml`.

The workflow:

1. verifies that the tag matches Android `versionName`;
2. restores the temporary release keystore from GitHub Actions secrets;
3. runs unit tests;
4. builds the signed release APK;
5. verifies the APK signature;
6. generates a SHA-256 checksum;
7. extracts release notes from the matching CHANGELOG section;
8. creates the GitHub Release;
9. marks prerelease versions such as `v0.1.0-beta` as prereleases;
10. removes the temporary keystore from the runner.

A release is not considered verified until the workflow is successful and the GitHub Release assets have been inspected.
