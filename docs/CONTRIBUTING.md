# Contributing to NexRhythm

Thank you for your interest in contributing to NexRhythm.

NexRhythm is intentionally small and focused. Contributions should improve the rhythm-training experience without adding unnecessary architecture, dependencies, or unrelated scope.

## Current Product Scope

The active trainer modes are:

- Basic
- My Exercise
- Polyrhythm

Experimental or deferred features should not be promoted to user-facing functionality without prior discussion and validation.

## Before You Start

For bug fixes, features, or behavior changes:

1. Search existing issues before opening a new one.
2. Keep the proposed change focused on one problem.
3. For larger product or architecture changes, open an issue first and explain the motivation and expected behavior.
4. Avoid unrelated cleanup or refactoring in the same change.

## Development Environment

The Android application is located in:

`nexrhythm_kotlin/`

Android Studio is recommended for Android source-code work. Documentation can be edited with VS Code or any text editor.

## Build and Test

Run commands from the repository root.

```powershell
git diff --check
.\nexrhythm_kotlin\gradlew.bat -p nexrhythm_kotlin test
.\nexrhythm_kotlin\gradlew.bat -p nexrhythm_kotlin assembleDebug
```

For device installation:

```powershell
adb devices
.\nexrhythm_kotlin\gradlew.bat -p nexrhythm_kotlin installDebug
adb shell am force-stop com.nexrhythm.app
adb shell monkey -p com.nexrhythm.app -c android.intent.category.LAUNCHER 1
```

Runtime validation is expected when a change affects UI, audio, timing, interaction, or device behavior.

## Contribution Guidelines

Please:

- prefer the smallest change that solves the problem;
- preserve readability and maintainability;
- follow the existing Kotlin and Jetpack Compose style;
- avoid introducing a new dependency unless it is clearly justified;
- avoid replacing the current architecture or state approach without prior discussion;
- do not change unrelated behavior;
- keep user-facing terminology consistent with the application;
- add or update tests when behavior can be covered meaningfully;
- update documentation when setup, usage, features, or project direction changes.

## Pull Requests

A pull request should:

- explain what changed and why;
- stay focused on one scope;
- include the verification that was actually performed;
- mention known limitations or remaining concerns;
- include screenshots for meaningful UI changes;
- avoid claiming that behavior is fully verified when it has not been runtime-tested.

## Commit Messages

Use short, descriptive commit messages.

Examples:

```text
feat: add about dialog
fix: correct exercise playback state
docs: update contribution guidelines
refactor: simplify trainer mode handling
```

## Licensing

By contributing to NexRhythm, you agree that your contribution will be licensed under the project's **GNU General Public License v3.0 (GPL-3.0)**.

See the repository `LICENSE` file for the full license text.
