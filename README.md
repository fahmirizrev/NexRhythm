# NexRhythm

NexRhythm is a visual, syllable-first rhythm learning and practice application for musicians.

It is designed to help players hear, see, understand, and practice rhythm before applying it to drums, bass, guitar, vocals, keyboards, or other instruments.

## Product Direction

NexRhythm is:

- Rhythm-first
- Instrument-agnostic
- Visual
- Syllable-first
- Practice-oriented

Music theory is used as an explanation layer, not as the primary experience.

## Current MVP

The first MVP intentionally stays small.

Primary target:

> Build a reliable subdivision trainer that can play and visualize 1–8 equal subdivisions per beat.

Core MVP features:

- BPM control
- Start / Stop
- 1–8 subdivisions per beat
- Main beat accent
- Stable subdivision audio
- Visual subdivision spacing
- Syllable display
- Basic audio guide modes
- Adjustable beats per subdivision
- Simple `1 → 8 → 1` pyramid practice

### Default Syllables

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

## Product Evolution

```text
Core Foundation
Pulse + Beat + Subdivision
        ↓
MVP
1–8 Subdivision Trainer
        ↓
Next
Meter + Rhythm Pattern + Theory Layer
        ↓
Later
Polyrhythm + Advanced Rhythm Practice
```

Polyrhythm, live/performance mode, advanced theory, and other larger features are deferred until the core MVP is stable.

## Repository Structure

```text
docs/
    Permanent product and project documentation

patches/
    Manual patches

patches/output/
    Comprehensive discovery reports

prompt/
    TASK prompts for coding agents

temp/
    Temporary development notes

nexrhythm_flutter/
    Flutter application source

AGENTS.md
    Development and coding-agent rules

ROADMAP.md
    MVP implementation roadmap

README.md
    Product knowledge entry point
```

## Flutter Application

Flutter source:

```text
nexrhythm_flutter/
```

Current baseline:

- Flutter 3.44.2
- Dart 3.12.2
- Android-first MVP

Development commands are run from the repository root:

```text
C:\laragon\www\nexrhythm
```

Project-specific command conventions should follow `AGENTS.md`.

## Development Principles

- Keep the MVP small.
- Prefer the simplest maintainable solution.
- Do not expand scope without an explicit decision.
- Reliable rhythm timing is more important than visual complexity.
- Avoid unnecessary dependencies and abstractions.
- Verify implementation before claiming completion.
- Use current repository state as the source of truth for implementation.

## Documentation

See:

- `AGENTS.md` — development and agent rules
- `ROADMAP.md` — current MVP roadmap
- `docs/` — permanent product and technical documentation
