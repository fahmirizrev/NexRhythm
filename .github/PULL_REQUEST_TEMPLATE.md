## Summary

Describe the change and why it is needed.

## Scope

What part of NexRhythm does this change affect?

- [ ] Basic
- [ ] My Exercise
- [ ] Polyrhythm
- [ ] Audio / timing
- [ ] Time signature / tempo
- [ ] Navigation / About
- [ ] Documentation
- [ ] Build / tooling
- [ ] Other

## Verification

Describe what you actually verified.

- [ ] `git diff --check`
- [ ] Unit tests
- [ ] Debug build
- [ ] Runtime check on Android device / emulator
- [ ] UI screenshots reviewed, if applicable
- [ ] Documentation reviewed, if applicable

Commands commonly used from the repository root:

```powershell
git diff --check
.\nexrhythm_kotlin\gradlew.bat -p nexrhythm_kotlin test
.\nexrhythm_kotlin\gradlew.bat -p nexrhythm_kotlin assembleDebug
```

## Screenshots

Add before/after screenshots for meaningful UI changes.

Not applicable for non-visual changes.

## Known Limitations / Concerns

List anything that is implemented but not verified, any known limitation, or any concern reviewers should know.

## Checklist

- [ ] The change is focused and does not include unrelated refactoring.
- [ ] No unnecessary dependency was added.
- [ ] User-facing terminology remains consistent with NexRhythm.
- [ ] Tests were added or updated when practical.
- [ ] Documentation was updated when setup, usage, or product behavior changed.
- [ ] I am not claiming behavior is fully verified unless the relevant verification was actually performed.
- [ ] I understand that contributions are licensed under GPL-3.0.
