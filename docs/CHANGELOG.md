# Changelog

All notable changes to NexRhythm are documented in this file.

## [Unreleased]

## [0.1.0-beta] - 2026-09-02

### Added

- Native Android rhythm trainer foundation using Kotlin and Jetpack Compose.
- Custom `AudioTrack`-based rhythm playback engine.
- Tempo control and time-signature support.
- Basic subdivision training from 1 to 8.
- My Exercise with continuous custom exercise loops.
- Polyrhythm training with independent Layer A and Layer B pulse controls.
- Branded launcher icon and splash screen.
- Navigation drawer and fit-to-screen practice layouts.
- About dialog with dynamic app version and **FRIZDEV** developer identity.
- Public-facing repository README, banner, screenshots, and application icon.
- GNU General Public License v3.0 (`GPL-3.0`).
- Open-source community health files, issue forms, and pull request template.
- Android CI workflow for unit tests and debug builds.
- Signed Android release workflow with APK signature verification, SHA-256 checksum generation, and automated GitHub Release creation.
- Release documentation covering versioning, signing, local verification, tagging, and GitHub Releases.

### Changed

- Established the native Android implementation as the active product baseline.
- Removed the previous Flutter application.
- Refined trainer defaults, live tempo updates, practice layout, and repository housekeeping.
- Simplified the active trainer experience to three modes:
  - Basic
  - My Exercise
  - Polyrhythm
- Cleaned exercise playback state after removing obsolete Pyramid Exercise behavior.
- Refined the repository README into a public-facing project overview.
- Set the first public beta version to `0.1.0-beta`.

### Removed

- Pyramid Exercise from the active product and related obsolete playback behavior.

### Deferred

- Rhythm Analyzer remains experimental and hidden from the user-facing application until musical accuracy is substantially improved and validated.
- Odd-meter grouping remains planned but is not currently user-facing.
