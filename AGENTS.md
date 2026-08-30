# AGENTS.md

## Project

**NexRhythm** is a rhythm learning and practice application for musicians.

Current MVP focus:

> Build a small, reliable subdivision trainer that can play and visualize 1–8 equal subdivisions per beat.

The product vision is broader, but agents must not expand MVP scope without explicit instruction.

---

## Repository Root

Work from:

```text
C:\laragon\www\nexrhythm
```

Expected structure:

```text
docs/                 Product/project documentation (*.md)
patches/              Manual patches (*.md)
patches/output/       Comprehensive discovery output (*.txt)
prompt/               Agent task prompts (TASK_*.txt)
temp/                 Temporary development notes
nexrhythm_flutter/    Flutter application source
.gitignore
AGENTS.md
ROADMAP.md
README.md
```

Do not invent files or directories that do not exist. Inspect the repository first.

---

## Product Principles

Prioritize:

- Simplicity
- Maintainability
- Readability
- Reliable timing
- Smallest solution that solves the current task

Avoid:

- Overengineering
- Premature optimization
- Unnecessary abstraction
- Unnecessary dependencies
- Scope creep

The MVP must work before it becomes sophisticated.

---

## Current Product Direction

NexRhythm is:

- Rhythm-first
- Instrument-agnostic
- Visual
- Syllable-first
- Practice-oriented

Default subdivision syllables:

```text
1 = TA
2 = TA-KA
3 = TA-KI-TA
4 = TA-KA-DI-MI
5 = TA-KA-TA-KI-TA
6 = TA-KA-DI-MI-TA-KA
7 = TA-KA-DI-MI-TA-KI-TA
8 = TA-KA-DI-MI-TA-KA-JU-NU
```

Music theory is an explanation layer, not the primary experience.

---

## MVP Scope

The first working MVP should focus on:

- BPM control
- Start / Stop
- 1–8 subdivisions per beat
- Main beat accent
- Evenly spaced subdivision audio
- Visual subdivision spacing
- Syllable display
- Basic guide audio modes
- Adjustable beats per subdivision
- Simple pyramid practice: `1 → 8 → 1`

Do not add unrelated features unless explicitly requested.

---

## Deferred / Post-MVP

Do not implement these unless a task explicitly asks for them:

- Polyrhythm
- Polymeter
- Live/performance mode
- Advanced music theory lessons
- Instrument-specific engines
- Accounts or cloud sync
- Social features
- MIDI integration
- Microphone/pad accuracy detection
- Full notation editor
- DAW/drum-machine features

---

## Source of Truth

Target behavior is determined by:

1. Latest user instruction
2. Latest confirmed decision
3. Latest project documentation
4. Current conversation context

Actual implementation state is determined by:

1. Current repository
2. Latest files
3. Latest terminal/test output
4. Latest screenshots
5. Agent reports

Never reconstruct source code from memory.

---

## Discovery Before Changes

If evidence is insufficient, perform **comprehensive discovery within the task scope** before editing.

Discovery must cover all directly related areas, such as:

- Entry point / primary source
- Callers and consumers
- Helpers/services/adapters
- State/models/controllers/providers
- Relevant data/control flow
- Tests
- Configuration
- Dependencies/assets
- Native/platform/build settings when relevant

Do not read the entire repository unless the task truly requires it.

Save one consolidated discovery report to:

```text
patches/output/<discovery_name>.txt
```

The report should contain:

- Task/problem
- Discovery scope
- Files inspected
- Important symbols/blocks
- Relevant relationships/data flow
- Existing test/verification coverage
- Relevant configuration/dependencies
- Findings
- Concerns/unknowns
- Whether evidence is sufficient to implement

Do not leave discovery as raw search output only.

---

## Implementation Rules

When implementing:

- Make the smallest necessary change.
- Preserve existing architecture unless change is explicitly required.
- Do not refactor unrelated code.
- Do not silently fix unrelated issues.
- Do not add dependencies without clear need.
- Keep UI, routing, persistence, and state-management changes within task scope.

If another issue is discovered, report it as a concern.

---

## Audio Timing Requirement

Timing correctness is a core product requirement.

The rhythm engine must not rely on ordinary UI timers for precise audio scheduling if that causes audible jitter or drift.

Important properties:

- Stable beat timing
- Even subdivision spacing
- No accumulated drift
- Audio timing independent from UI rendering performance where practical

Do not claim timing is reliable without verification.

---

## Verification

Run relevant verification after implementation:

- Formatter
- Static analysis
- Related tests
- Build check when appropriate
- Focused regression check

Distinguish clearly between:

- Implemented
- Verified
- Not Yet Verified
- Concern
- Known Limitation
- Out of Scope

Never claim production-ready or no-regression without evidence.

---

## Documentation

Use:

```text
docs/*.md
```

for permanent project/product documentation.

Use:

```text
temp/
```

for temporary development notes.

Documentation language:

- README: English
- ROADMAP: Bahasa Indonesia + English
- Project docs: Bahasa Indonesia
- Changelog, if added later: English, newest entry first

Update documentation only when the task materially changes documented behavior, setup, architecture, or project status.

---

## Agent Task Output

For tasks started from `prompt/TASK_*.txt`:

1. Read the repository first.
2. Respect the task scope.
3. Perform discovery when evidence is insufficient.
4. Implement only after evidence is sufficient.
5. Run relevant verification.
6. Report:
   - Implemented
   - Verified
   - Concerns
   - Known limitations
   - Out of scope
7. Do not expand scope without explicit approval.
