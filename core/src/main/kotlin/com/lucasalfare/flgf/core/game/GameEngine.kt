package com.lucasalfare.flgf.core.game

import kotlin.math.abs

/**
 *
 * Core gameplay engine responsible for:
 *
 * - Spawning notes
 *
 * - Resolving hits/misses
 *
 * - Managing sustain logic
 *
 * - Handling scoring and combo
 *
 * - Managing special ability state
 *
 * This engine is time-driven and expects external calls to [tick].
 *
 * @param notes Full chart of notes, sorted by time.
 *
 * @param hitWindow Allowed timing error (in milliseconds) for hitting notes.
 */
class GameEngine(
  private val notes: List<com.lucasalfare.flgf.core.game.Note>,
  private val hitWindow: Long,
  private val spawnAheadTime: Long = 0L
) {
  private var time: Long = 0
  private var nextIndex = 0
  private var specialDisabledUntilTime: Long = Long.MIN_VALUE

  val notesStates = mutableListOf<com.lucasalfare.flgf.core.game.NoteState>()
  val score = _root_ide_package_.com.lucasalfare.flgf.core.game.ScoreState()
  val special = _root_ide_package_.com.lucasalfare.flgf.core.game.SpecialState()

  fun tick(input: com.lucasalfare.flgf.core.game.PlayerInput, currentTime: Long) {
    val dt = currentTime - time
    time = currentTime
    updateSpecial(input, dt)
    spawnNotes()
    resolveMissedNotes()
    resolveInputs(input)
    processSustain(input, dt)
    cleanup()
  }

  private fun spawnNotes() {
    while (nextIndex < notes.size && notes[nextIndex].hitTime <= time + spawnAheadTime) {
      val note = notes[nextIndex]
      notesStates.add(
        NoteState(
          note = note,
          specialDisabled = note.isSpecial && note.hitTime < specialDisabledUntilTime
        )
      )
      nextIndex++
    }
  }

  private fun resolveMissedNotes() {
    val missedNotes = notesStates
      .filter { !it.hit && !it.missed && time > it.note.hitTime + hitWindow }
      .sortedBy { it.note.hitTime }

    if (missedNotes.isEmpty()) return

    resetCombo()
    missedNotes.forEach {
      it.missed = true
      onNoteMiss(it)
    }
  }

  private fun resolveInputs(input: PlayerInput) {
    if (input.justPressedFrets.isEmpty()) return

    val hitNotes = mutableListOf<NoteState>()
    var hasWrongInput = false

    input.justPressedFrets.sorted().forEach { lane ->
      val noteState = findClosestPendingNoteForLane(lane)
      if (noteState == null) {
        hasWrongInput = true
        return@forEach
      }

      noteState.hit = true
      noteState.holding = noteState.note.duration > 0
      hitNotes += noteState
    }

    hitNotes.forEach {
      addHit()
      onNoteHit(it)
    }

    if (hasWrongInput) resetCombo()
  }

  private fun findClosestPendingNoteForLane(lane: Int): NoteState? {
    return notesStates
      .asSequence()
      .filter { !it.hit && !it.missed && it.note.lane == lane }
      .filter { abs(time - it.note.hitTime) <= hitWindow }
      .minWithOrNull(compareBy<NoteState>({ abs(time - it.note.hitTime) }, { it.note.hitTime }))
  }

  private fun onNoteHit(noteState: NoteState) {
    if (special.active) return
    if (noteState.note.isSpecial && !noteState.specialDisabled) {
      if (!special.inSequence) {
        special.inSequence = true
        special.sequenceBroken = false
      }
    } else if (noteState.note.isSpecial && noteState.specialDisabled) {
      return
    } else {
      if (special.inSequence && !special.sequenceBroken) {
        special.energy = (special.energy + 25).coerceAtMost(100)
      }
      special.inSequence = false
      special.sequenceBroken = false
    }

    println("Score data: $score")
  }

  private fun onNoteMiss(noteState: NoteState) {
    if (!noteState.note.isSpecial || noteState.specialDisabled) return

    special.sequenceBroken = true

    val currentIndex = notes.indexOfFirst { it == noteState.note }
    val sequenceEndTime = notes
      .drop(currentIndex + 1)
      .firstOrNull { !it.isSpecial }
      ?.hitTime ?: Long.MAX_VALUE

    specialDisabledUntilTime = sequenceEndTime

    notesStates.forEach {
      if (!it.hit && !it.missed &&
        it.note.isSpecial &&
        it.note.hitTime >= noteState.note.hitTime &&
        it.note.hitTime < sequenceEndTime
      ) {
        it.specialDisabled = true
      }
    }
  }

  private fun updateSpecial(input: PlayerInput, dt: Long) {
    if (input.activateSpecial && special.energy >= 50) special.active = true
    if (special.active) {
      special.drainAccumulator += 25.0 * dt / 1000.0
      val drainedEnergy = special.drainAccumulator.toInt()

      if (drainedEnergy > 0) {
        special.energy = (special.energy - drainedEnergy).coerceAtLeast(0)
        special.drainAccumulator -= drainedEnergy
      }

      if (special.energy <= 0) {
        special.energy = 0
        special.active = false
        special.drainAccumulator = 0.0
      }
      special.inSequence = false
      special.sequenceBroken = false
    } else {
      special.drainAccumulator = 0.0
    }
  }

  private fun specialMultiplier() = if (special.active) 2 else 1

  private fun processSustain(input: PlayerInput, dt: Long) {
    val sustainRatePerSecond = 50.0
    notesStates.forEach {
      if (!it.holding) return@forEach
      val remaining = it.note.duration - it.sustainProgress

      if (remaining <= 0.0) {
        it.holding = false
        return@forEach
      }

      val laneReleasedThisFrame = it.note.lane in input.justReleasedFrets
      val laneStillHeld = it.note.lane in input.pressedFrets

      if (laneReleasedThisFrame || !laneStillHeld) {
        it.holding = false
        if (it.sustainProgress < it.note.duration) {
          it.sustainBroken = true
        }
        return@forEach
      }

      val delta = minOf(dt.toDouble(), remaining)
      score.score += (delta / 1000.0 * sustainRatePerSecond * score.multiplier * specialMultiplier()).toInt()
      it.sustainProgress += delta
      if (it.sustainProgress >= it.note.duration) it.holding = false
    }
  }

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

  private fun cleanup() {
    // Sustain notes need to remain alive until the tail has fully left the screen.
    notesStates.removeIf { (it.missed || it.hit) && time > despawnTime(it.note) }
  }

  private fun despawnTime(note: Note): Long {
    val visibleLifetime = maxOf(1000L, note.duration)
    return note.hitTime + visibleLifetime + 1000L
  }
}
