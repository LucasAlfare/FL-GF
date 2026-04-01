# 🎸 FL-GF

A clean-room rewrite of the classic **Guitar Flash** (Brazilian Guitar Hero clone), built with **pure Kotlin** and a modern architecture.

This project focuses on **correctness, maintainability, and testability**, addressing structural issues found in the original ActionScript implementation.

---

## 📌 Project Status

**Current state:** 🟡 In Progress (Core gameplay functional)

### ✅ Implemented

* Core gameplay engine (`GameEngine`)
* Deterministic time-based update loop (`tick`)
* Note spawning & lifecycle
* Hit/miss resolution (including chords)
* Sustain (hold note) system
* Score system (combo + multiplier)
* Special ability system (star power-like)
* XML chart parser (robust & fault-tolerant)
* Basic rendering prototype using LibGDX

### 🚧 In Progress / Missing

* Real input handling (keyboard/controller)
* Audio synchronization
* UI (menus, HUD, feedback)
* Visual polish (effects, animations)
* Asset pipeline improvements
* Full test suite implementation (scenarios already defined)
* Performance tuning

---

## 🧱 Project Structure

```
FL-GF
├── build.gradle.kts
├── settings.gradle.kts
├── core
│   ├── build.gradle.kts
│   ├── src
│   │   ├── Core.kt
│   │   ├── GuitarFlashGame.kt
│   ├── resources
│   │   └── (original game assets - decompiled)
│   └── testes
├── desktop
│   ├── build.gradle.kts
│   └── launcher (LibGDX desktop entrypoint)
```

### Modules

* **core**

    * Contains all game logic and rendering prototype
    * Fully independent gameplay engine
* **desktop**

    * Desktop launcher using LibGDX backend

---

## ⚙️ Tech Stack

* **Kotlin (JVM)**
* **LibGDX** (rendering + platform abstraction)
* **Gradle Kotlin DSL**
* XML parsing via standard Java APIs

---

## 🧠 Architecture Overview

### 1. Engine-Centric Design

The project is built around a **pure gameplay engine**:

```kotlin
fun tick(input: PlayerInput, currentTime: Long)
```

This makes the system:

* deterministic
* testable
* independent from rendering
* independent from input source

---

### 2. State Isolation

Instead of global mutable state, the engine uses explicit data models:

```kotlin
data class ScoreState(...)
data class SpecialState(...)
data class NoteState(...)
```

Everything is:

* scoped
* predictable
* composable

---

### 3. Time-Driven Logic

All gameplay is driven by **absolute time**, not frames.

This avoids:

* frame dependency bugs
* inconsistent timing across machines

---

### 4. Separation of Concerns

| Responsibility | Location               |
| -------------- | ---------------------- |
| Game rules     | `GameEngine`           |
| Rendering      | `GuitarFlashGame`      |
| Parsing        | `SongXmlParser`        |
| Data models    | Immutable data classes |

---

## 🔥 Original vs Rewrite

The original Guitar Flash codebase suffers from heavy coupling, global state, and implicit behavior.

This rewrite fixes those issues systematically.

---

### ❌ Problem 1: Global State Everywhere

**Original (ActionScript):**

```actionscript
public static var pontos:Number = 0;
public static var pontosG:Number = 0;
public static var especial:Boolean = false;
public static var fret1:Boolean = false;
public static var fret2:Boolean = false;
```



* Everything is global
* No ownership of data
* Impossible to test in isolation
* Side effects everywhere

---

**✅ Rewrite (Kotlin):**

```kotlin
data class ScoreState(
  var score: Int = 0,
  var combo: Int = 0,
  var multiplier: Int = 1
)

data class SpecialState(
  var energy: Int = 0,
  var active: Boolean = false
)
```

✔ State is localized
✔ Explicit ownership
✔ Test-friendly
✔ No hidden dependencies

---

### ❌ Problem 2: Game Logic Mixed with Rendering & Input

**Original:**

```actionscript
addEventListener(Event.ENTER_FRAME, tcVerif);
stage.addEventListener(KeyboardEvent.KEY_DOWN, tcEntra);
```

* Input, rendering, and logic all tied together
* Behavior depends on Flash runtime events
* Hard to reason about execution order

---

**✅ Rewrite:**

```kotlin
engine.tick(input, currentTime)
```

✔ Single entry point
✔ Deterministic execution order
✔ Decoupled from framework

---

### ❌ Problem 3: Implicit, Scattered Rules

Original logic is spread across:

* `jogo.as`
* `musicaXML.as`
* global variables
* frame-based conditions

Example (simplified):

```actionscript
if(root["paleta" + i].currentFrame == 2 || root["paleta" + i].currentFrame == 6)
{
    root["paleta" + i].gotoAndPlay(3);
}
```

* Meaning depends on animation frames (!)
* No clear rule definition
* Hard to maintain

---

**✅ Rewrite: Explicit Rules**

```kotlin
val expected = group.map { it.note.lane }.toSet()
val pressed = input.justPressedFrets

val exact = expected == pressed
val partial = expected.intersect(pressed).isNotEmpty()
```

✔ Rules are declarative
✔ No hidden meaning
✔ Easy to extend and debug

---

### ❌ Problem 4: No Clear Note Lifecycle

Original mixes:

* spawn
* movement
* hit detection
* scoring

All inside giant loops.

---

**✅ Rewrite: Structured Pipeline**

```kotlin
fun tick(...) {
  updateSpecial(...)
  spawnNotes()
  resolveNotes(...)
  processSustain(...)
  cleanup()
}
```

✔ Clear lifecycle
✔ Predictable flow
✔ Easy to modify

---

### ❌ Problem 5: Fragile XML Handling

**Original:**

```actionscript
xmlNotas = xml.Data.Note.attribute("track");
xmlTempo = xml.Data.Note.attribute("time");
```

* No validation
* Assumes perfect data
* Crashes or corrupts silently

---

**✅ Rewrite: Robust Parser**

```kotlin
val timeSec = node.getAttribute("time").toDoubleOrNull() ?: continue
val lane = node.getAttribute("track").toIntOrNull() ?: continue
```

✔ Skips invalid data safely
✔ Converts units explicitly
✔ Guarantees sorted output

---

## 🧪 Testing Strategy

Test scenarios are already defined (to be automated):

* Single note hit/miss
* Chords (correct and partial)
* Sustain behavior
* Combo & multiplier progression
* Rapid sequences
* Special activation & drain
* Edge timing cases

The engine design allows **pure unit testing without rendering**.

---

## 🎯 Goals

* Rebuild Guitar Flash with modern engineering practices
* Preserve gameplay feel while fixing structural issues
* Serve as a reference for:

    * game loop design
    * deterministic simulation
    * refactoring legacy code

---

## ⚠️ Disclaimer

This project is a **technical rewrite for study and improvement purposes**.

Assets included in `/resources` were extracted from the original game and are not owned by this repository.

---

## 👤 Author

**Francisco Lucas (FL)**