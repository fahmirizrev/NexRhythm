# Changelog

All notable changes to NexRhythm are documented in this file.

NexRhythm is currently in pre-release development. Until the first tagged public release, changes are collected under **Unreleased**.

## [Unreleased]

### Added

- About dialog accessible from the bottom of the navigation drawer.
- Developer identity shown as **FRIZDEV** in the About dialog.
- App version displayed dynamically from the Android build configuration.
- Repository presentation assets, including the NexRhythm banner, screenshots, and application icon.
- GNU General Public License v3.0 (`GPL-3.0`) as the project license.

### Changed

- Simplified the active trainer experience to three modes:
  - Basic
  - My Exercise
  - Polyrhythm
- Cleaned exercise playback state after removing obsolete Pyramid Exercise behavior.
- Refined the repository README into a public-facing project overview.

### Removed

- Pyramid Exercise from the active product and related obsolete playback behavior.

### Deferred

- Rhythm Analyzer remains experimental and hidden from the user-facing application until musical accuracy is substantially improved and validated.
- Odd-meter grouping remains planned but is not currently user-facing.

## Initial Development — 2026-08-30 to 2026-09-01

### Added

- Native Android rhythm trainer foundation using Kotlin and Jetpack Compose.
- Custom `AudioTrack`-based rhythm playback engine.
- Tempo control and time-signature support.
- Subdivision training with visual rhythm feedback.
- Continuous custom exercise loops.
- Polyrhythm training with independent Layer A and Layer B pulse controls.
- Branded launcher icon and splash screen.
- Navigation drawer and fit-to-screen practice layouts.

### Changed

- Established the native Android implementation as the active product baseline.
- Removed the previous Flutter application.
- Refined trainer defaults, live tempo updates, practice layout, and repository housekeeping.
