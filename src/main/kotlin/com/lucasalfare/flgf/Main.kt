package com.lucasalfare.flgf

import kotlin.math.abs

// ==================== MODELOS ====================

data class PlayerInput(
  val pressedFrets: Set<Int>,
  val justPressedFrets: Set<Int>,
  val activateSpecial: Boolean = false
)

data class Note(
  val time: Long,
  val lane: Int,
  val duration: Long,
  val isSpecial: Boolean = false
)

data class ActiveNote(
  val note: Note,
  var hit: Boolean = false,
  var missed: Boolean = false,
  var holding: Boolean = false,
  var sustainProgress: Double = 0.0
)

data class ScoreState(
  var score: Int = 0,
  var combo: Int = 0,
  var multiplier: Int = 1
)

data class SpecialState(
  var energy: Int = 0,
  var active: Boolean = false,

  var inSequence: Boolean = false,
  var sequenceBroken: Boolean = false
)

// ==================== ENGINE ====================

class GameEngine(
  private val notes: List<Note>,
  private val hitWindow: Long
) {

  private var time: Long = 0
  private var nextIndex = 0

  private val activeNotes = mutableListOf<ActiveNote>()

  val score = ScoreState()
  val special = SpecialState()

  fun tick(input: PlayerInput, currentTime: Long) {
    val dt = currentTime - time
    time = currentTime

    updateSpecial(input, dt)
    spawnNotes()
    resolveNotes(input)
    processSustain(input, dt)
    cleanup()
  }

  // ==================== SPAWN ====================

  private fun spawnNotes() {
    while (nextIndex < notes.size && notes[nextIndex].time <= time + hitWindow) {
      activeNotes.add(ActiveNote(notes[nextIndex]))
      nextIndex++
    }
  }

  // ==================== CORE ====================

  private fun resolveNotes(input: PlayerInput) {

    val pending = activeNotes.filter { !it.hit && !it.missed }
    if (pending.isEmpty()) return

    val nextTime = pending.minOf { it.note.time }
    val group = pending.filter { it.note.time == nextTime }

    val inWindow = abs(time - nextTime) <= hitWindow

    // ===== HIT =====
    if (inWindow && input.justPressedFrets.isNotEmpty()) {

      val expected = group.map { it.note.lane }.toSet()
      val pressed = input.justPressedFrets

      val exact = expected == pressed
      val partial = expected.intersect(pressed).isNotEmpty()

      when {
        exact -> {
          addHit()
          group.forEach {
            it.hit = true
            it.holding = it.note.duration > 0
            onNoteHit(it.note)
          }
        }

        partial -> {
          resetCombo()
          group.forEach {
            if (it.note.lane in pressed) {
              it.hit = true
              it.holding = it.note.duration > 0
              onNoteHit(it.note)
            } else {
              it.missed = true
              onNoteMiss(it.note)
            }
          }
        }

        else -> {
          resetCombo()
          group.forEach {
            it.missed = true
            onNoteMiss(it.note)
          }
        }
      }
    }

    // ===== MISS =====
    group.forEach {
      if (!it.hit && !it.missed && time > it.note.time + hitWindow) {
        it.missed = true
        resetCombo()
        onNoteMiss(it.note)
      }
    }
  }

  // ==================== SPECIAL ====================

  private fun onNoteHit(note: Note) {

    if (special.active) return

    if (note.isSpecial) {
      if (!special.inSequence) {
        special.inSequence = true
        special.sequenceBroken = false
      }
    } else {
      // fim da sequência
      if (special.inSequence && !special.sequenceBroken) {
        special.energy = (special.energy + 25).coerceAtMost(100)
      }
      special.inSequence = false
      special.sequenceBroken = false
    }
  }

  private fun onNoteMiss(note: Note) {
    if (note.isSpecial) {
      special.sequenceBroken = true
    }
  }

  private fun updateSpecial(input: PlayerInput, dt: Long) {

    if (input.activateSpecial && special.energy >= 50) {
      special.active = true
    }

    if (special.active) {
      special.energy -= (25 * (dt / 1000.0)).toInt()

      if (special.energy <= 0) {
        special.energy = 0
        special.active = false
      }

      // bloqueia coleta enquanto ativo
      special.inSequence = false
      special.sequenceBroken = false
    }
  }

  private fun specialMultiplier() =
    if (special.active) 2 else 1

  // ==================== SUSTAIN ====================

  private fun processSustain(input: PlayerInput, dt: Long) {
    val sustainRatePerSecond = 50.0

    activeNotes.forEach {
      if (!it.holding) return@forEach

      val holding = it.note.lane in input.pressedFrets

      if (!holding) {
        it.holding = false
        return@forEach
      }

      val remaining = it.note.duration - it.sustainProgress
      val delta = minOf(dt.toDouble(), remaining)

      val gained = (
          delta / 1000.0 *
              sustainRatePerSecond *
              score.multiplier *
              specialMultiplier()
          ).toInt()

      score.score += gained
      it.sustainProgress += delta

      if (it.sustainProgress >= it.note.duration) {
        it.holding = false
      }
    }
  }

  // ==================== SCORE ====================

  private fun addHit() {
    score.combo++

    score.multiplier = when {
      score.combo >= 30 -> 4
      score.combo >= 20 -> 3
      score.combo >= 10 -> 2
      else -> 1
    }

    score.score += 50 * score.multiplier * specialMultiplier()
  }

  private fun resetCombo() {
    score.combo = 0
    score.multiplier = 1
  }

  // ==================== CLEANUP ====================

  private fun cleanup() {
    activeNotes.removeIf {
      it.missed && time > it.note.time + 1000
    }
  }
}

/*
should hit a single note
should miss a note
should sustain long note and gain points progress
should break points gained of sustaining if released early
should hit chord correctly
should partially hit chord and fail combo due to the not hit
should partially hit chord with sustain and gain progress for the correct hit note and fail combo for not hit
should handle rapid fire notes on same lane
should fail combo on one miss in rapid sequence
should fail combo on one wrong hit in rapid sequence
should handle very fast notes near hit window limit
should increase multiplier with combo
should reset combo and multiplier on miss
should advance special on getting all special notes of a sequence
should let activate special when enough energy
should drain the activated special energy over time
should increase score faster when special is active
should gain more points from sustain with special active
should stop gaining sustain bonus after special ends
should continue sustain normally after special ends
should ignore special phrases while special is active
 */