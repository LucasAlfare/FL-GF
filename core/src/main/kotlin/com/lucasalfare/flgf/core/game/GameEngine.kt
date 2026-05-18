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
  private val notes: List<Note>,
  private val hitWindow: Long,
  private val spawnAheadTime: Long = 0L,
  private val scoringRules: ScoringRules = ScoringRules()
) {
  private val scoring = ScoringSystem(scoringRules)
  private var time: Long = 0
  private var nextIndex = 0
  private var specialDisabledUntilTime: Long = Long.MIN_VALUE

  val notesStates = mutableListOf<NoteState>()
  val score = ScoreState()
  val special = SpecialState()

  fun tick(input: PlayerInput, currentTime: Long) {
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

    scoring.resetCombo(score)
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

    if (hasWrongInput) scoring.resetCombo(score)
  }

  private fun findClosestPendingNoteForLane(lane: Int): NoteState? {
    return notesStates
      .asSequence()
      .filter { !it.hit && !it.missed && it.note.lane == lane }
      .filter { abs(time - it.note.hitTime) <= hitWindow }
      .minWithOrNull(compareBy({ abs(time - it.note.hitTime) }, { it.note.hitTime }))
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
        special.energy = scoring.gainSpecialEnergy(special.energy)
      }
      special.inSequence = false
      special.sequenceBroken = false
    }
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
    if (input.activateSpecial && scoring.canActivateSpecial(special.energy)) special.active = true
    if (special.active) {
      scoring.updateSpecialDrain(special, dt)
      special.inSequence = false
      special.sequenceBroken = false
    } else {
      special.drainAccumulator = 0.0
    }
  }

  private fun processSustain(input: PlayerInput, dt: Long) {
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
      scoring.registerSustain(score, delta, special.active)
      it.sustainProgress += delta
      if (it.sustainProgress >= it.note.duration) it.holding = false
    }
  }

  private fun addHit() {
    scoring.registerHit(score, special.active)
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
