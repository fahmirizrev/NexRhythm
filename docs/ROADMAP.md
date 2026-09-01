# NexRhythm Roadmap

This roadmap describes the current product direction for NexRhythm. It is not a release-date commitment. Priorities may change based on testing, product needs, and implementation evidence.

## Status Definitions

- **Implemented** — present in the current source or product.
- **Verified** — confirmed through relevant build, test, or runtime evidence.
- **Target** — planned for a future phase.
- **Concern** — requires attention before the related work can be considered complete.
- **Known Limitation** — a limitation that is already understood.
- **Deferred** — intentionally postponed and outside the active scope.

## Current Baseline

### Implemented

- Native Android application using Kotlin and Jetpack Compose.
- Custom `AudioTrack`-based rhythm playback.
- Basic subdivision training from 1 to 8.
- My Exercise with selected subdivisions and continuous ascending/descending exercise flow.
- Polyrhythm with two independent pulse layers.
- Time signatures with numerator `2–24` and denominator `2, 4, 8, 16`.
- Quarter-note BPM reference with denominator-aware beat duration.
- Navigation drawer with Basic, My Exercise, Polyrhythm, and About.
- About dialog with version information and FRIZDEV developer identity.
- GPL-3.0 licensing.
- Public-facing repository README, banner, screenshots, and icon assets.
- Open-source community health documentation and GitHub contribution templates.

### Verified

- Relevant unit tests and Android debug builds passed during the latest implementation cycle.
- Basic, My Exercise, Polyrhythm, the navigation drawer, and About UI have been runtime-checked on an Android device.
- Pyramid Exercise residue was removed and verified through source search, tests, and build checks.
- Phase 2 community health files are committed to `main`.

## Phase 3 — Release Infrastructure

### Implemented

- Android CI workflow for unit tests and debug builds.
- Semantic public version naming with Android `versionCode` kept as a monotonically increasing integer.
- Release signing configuration that reads credentials from Gradle properties rather than repository files.
- Tag-driven signed release workflow.
- APK signature verification in CI.
- SHA-256 checksum generation for release APKs.
- Automated GitHub Release creation from version tags.
- Release-process documentation.

### Target

- Generate and securely back up the FRIZDEV release signing key.
- Configure GitHub Actions release secrets.
- Verify the Android CI workflow succeeds on `main`.
- Build and verify the signed `0.1.0-beta` APK locally.
- Commit all Phase 3 release-infrastructure changes.
- Push tag `v0.1.0-beta`.
- Verify the tag-triggered release workflow succeeds.
- Verify the private GitHub Release contains:
  - the signed APK;
  - the SHA-256 checksum;
  - release notes derived from the CHANGELOG.

### Concern

The signing key is a long-term release identity. Loss of the private key can prevent compatible updates for users who installed APKs signed with that key. The key must never be committed to Git.

## Phase 4 — Public Release

### Target

- Audit the repository and Git history before changing repository visibility.
- Check for:
  - secrets or credentials;
  - signing keys or keystores;
  - local machine paths;
  - private development files;
  - build outputs and debug artifacts;
  - accidentally committed temporary files;
  - sensitive information in Git history.
- Make the repository public only after the release-readiness audit passes.
- Verify the README, screenshots, license detection, issue templates, security policy, release page, checksum, and downloadable APK from the public repository view.
- Gather real user feedback before expanding product scope.

## Deferred Product Work

### Odd-Meter Grouping

**Status: Deferred**

Odd-meter grouping will be treated as a separate musical concept from time signature.

Examples:

- `7/8 = 2+2+3`
- `7/8 = 3+2+2`
- `7/8 = 2+3+2`
- `11/8 = 3+3+3+2`

Potential future accent hierarchy:

- Primary Accent = beginning of measure.
- Secondary Accent = beginning of group.
- Regular Click = other beat units.

This is not part of the current active implementation scope.

### Rhythm Analyzer

**Status: Deferred / Known Limitation**

An imported-audio rhythm-analysis prototype exists, but its musical interpretation is not reliable enough for user-facing release.

Known limitations include:

- unstable tempo interpretation on complex full-mix audio;
- half-time / double-time ambiguity;
- low-confidence meter estimation;
- unreliable time-signature interpretation.

A future research direction may consider learned beat/downbeat detection. Any model or runtime dependency must be evaluated for accuracy, licensing, redistribution rights, Android runtime cost, and long-term maintainability before adoption.

## Product Principle

NexRhythm prioritizes the smallest change that produces meaningful rhythm-training value.

A feature should not become user-facing only because it is technically possible. Experimental features should remain hidden or deferred until their behavior, musical value, and runtime reliability are supported by evidence.
