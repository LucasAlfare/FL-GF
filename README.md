# FL-GF

A Kotlin-based experimental clone of the old **Guitar Flash** game, focused on rebuilding the core gameplay logic with a clean and testable architecture.

This project is not intended to be a full production-ready game. The main goal is to understand and reimplement the underlying mechanics of rhythm games like Guitar Flash / Guitar Hero in a structured and maintainable way.

---

## Current Status

The project is in an **early development stage**.

So far, the focus has been on:

* Core **gameplay rules and engine**
* **Domain modeling**
* **Use cases**
* **Automated tests**

The architecture follows a **Clean Architecture** approach, prioritizing separation of concerns and testability.

### Not implemented yet

* Audio system (entire **infrastructure layer** for audio is still missing)
* Rendering / graphics layer
* Input adapters (framework/platform-specific)
* Full gameplay integration

---

## Core Gameplay Requirements

The engine is being designed to support the following behaviors:

### Notes & Timing

* Hit a single note correctly
* Miss a note
* Handle very fast notes near the hit window limit
* Handle rapid sequences on the same lane

### Chords

* Hit chords correctly
* Partially hit chords
* Break combo when a chord is not fully hit
* Handle mixed sustain behavior within chords

### Sustain Notes

* Sustain long notes and gain progressive score
* Interrupt sustain early and lose potential points
* Continue sustain correctly after special ends
* Stop special bonus gain when special ends

### Combo & Score

* Increase combo on correct hits
* Break combo on miss or wrong input
* Increase multiplier based on combo
* Reset combo and multiplier on failure
* Fail combo on mistakes during fast sequences

### Special System

* Build special energy by completing note phrases
* Only gain energy when all notes in a phrase are hit
* Activate special when enough energy is available
* Drain special energy over time once activated
* Increase score gain while special is active
* Increase sustain score while special is active
* Ignore special phrases while special is active

---

## Graphics (Future Plans)

Graphical assets are not a priority right now.

The intention is to eventually reuse or recreate visuals inspired by the **original Guitar Flash**, keeping a similar look and feel.

---

## Goal

The main goal of this project is to build a **clean, deterministic, and testable rhythm game engine**, rather than a feature-complete clone.

---

## License

No license defined yet. This is currently an experimental project and may change in the future.