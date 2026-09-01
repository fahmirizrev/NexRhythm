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

### Verified

- Relevant unit tests and Android debug builds passed during the latest implementation cycle.
- Basic, My Exercise, Polyrhythm, the navigation drawer, and About UI have been runtime-checked on an Android device.
- Pyramid Exercise residue was removed and verified through source search, tests, and build checks.

## Phase 2 — Open Source Health

### Target

- Maintain a project CHANGELOG.
- Maintain a public ROADMAP.
- Provide contribution guidelines.
- Provide a Code of Conduct.
- Provide a security policy and responsible disclosure guidance.
- Provide structured bug-report and feature-request issue forms.
- Provide a pull request template.
- Verify that GitHub detects and renders all community health files correctly after they are committed.

## Phase 3 — Release Infrastructure

### Target

- Add a minimal Android CI workflow for:
  - unit tests;
  - debug build.
- Define the public release versioning strategy.
- Prepare the first public beta release.
- Attach a verified APK to GitHub Releases.
- Write concise release notes derived from the CHANGELOG.
- Audit the repository and Git history before making the repository public.

### Release Readiness Audit

The audit should check for:

- secrets or credentials;
- signing keys or keystores;
- local machine paths;
- private development files;
- build outputs and debug artifacts;
- accidentally committed temporary files;
- sensitive information in Git history.

### Concern

The current Android `versionName` belongs to the development baseline and should not be treated as the first public release version until release versioning is explicitly finalized.

## Phase 4 — Public Release

### Target

- Make the repository public only after the release-readiness audit passes.
- Publish the first public beta release.
- Verify README links, screenshots, license detection, issue templates, security policy, and downloadable APK from the public repository view.
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
