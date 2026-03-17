package com.lucasalfare.flgf.b_usecase

import com.lucasalfare.kgf.a_domain.entities.ActiveNote
import com.lucasalfare.kgf.a_domain.entities.GameState
import com.lucasalfare.kgf.a_domain.entities.GameStatus
import com.lucasalfare.kgf.a_domain.entities.HitWindow
import com.lucasalfare.kgf.a_domain.entities.InputEvent
import com.lucasalfare.kgf.a_domain.entities.InputType
import com.lucasalfare.kgf.a_domain.entities.Judgement
import com.lucasalfare.kgf.a_domain.entities.NoteState
import kotlin.math.abs

/**
 * Pure game engine responsible for updating the GameState.
 *
 * This class contains the core gameplay loop logic, completely independent
 * from rendering, audio, or input frameworks.
 *
 * It transforms the current GameState into a new GameState based on:
 * - Current time
 * - Player input events
 *
 * It is deterministic and side-effect free.
 */
class GameEngine(
  private val hitWindow: HitWindow,
  private val spawnWindow: Double = 2.0
) {

  fun update(
    state: GameState,
    currentTime: Double,
    inputs: List<InputEvent>
  ): GameState {

    var newState = state.copy(currentTime = currentTime)

    newState = spawnNotes(newState)
    newState = processMissedNotes(newState)
    newState = processInputs(newState, inputs)
    newState = cleanupNotes(newState)
    newState = updateGameStatus(newState)

    return newState
  }

  private fun spawnNotes(state: GameState): GameState {
    val notes = state.song.notes
    var index = state.nextNoteIndex
    val active = state.activeNotes.toMutableList()

    while (index < notes.size) {
      val note = notes[index]

      if (note.time <= state.currentTime + spawnWindow) {
        active.add(ActiveNote(note = note))
        index++
      } else break
    }

    return state.copy(
      activeNotes = active,
      nextNoteIndex = index
    )
  }

  /**
   * Passive miss (note passed the hit window)
   */
  private fun processMissedNotes(state: GameState): GameState {
    var combo = state.combo

    val updated = state.activeNotes.map {
      if (it.state == NoteState.PENDING &&
        state.currentTime > it.note.time + hitWindow.good
      ) {
        combo = 0
        it.copy(state = NoteState.MISSED)
      } else it
    }

    return state.copy(
      activeNotes = updated,
      combo = combo
    )
  }

  /**
   * Input processing aligned with legacy behavior:
   * - respects timeline order
   * - only considers notes within hit window
   * - first valid note wins
   * - wrong input penalizes
   */
  private fun processInputs(
    state: GameState,
    inputs: List<InputEvent>
  ): GameState {

    var score = state.score
    var combo = state.combo
    var maxCombo = state.maxCombo

    val updatedNotes = state.activeNotes.toMutableList()

    for (input in inputs) {

      if (input.type != InputType.PRESS) continue

      // this filter takes the first valid note in the streak
      val index = updatedNotes.indexOfFirst { note ->
        note.state == NoteState.PENDING &&
            note.note.lane == input.lane &&
            abs(note.note.time - input.time) <= hitWindow.good
      }

      // no valid note
      if (index == -1) {
        combo = 0
        continue
      }

      val activeNote = updatedNotes[index]
      val delta = abs(activeNote.note.time - input.time)

      val judgement = judge(delta)

      // just extra check
      if (judgement == null) {
        combo = 0
        continue
      }

      // hit
      updatedNotes[index] = activeNote.copy(state = NoteState.HIT)

      when (judgement) {
        Judgement.PERFECT -> score += 100
        Judgement.GOOD -> score += 70
        Judgement.MISS -> {}
      }

      combo++
      if (combo > maxCombo) maxCombo = combo
    }

    return state.copy(
      activeNotes = updatedNotes,
      score = score,
      combo = combo,
      maxCombo = maxCombo
    )
  }

  private fun cleanupNotes(state: GameState): GameState {
    val remaining = state.activeNotes.filter {
      it.state == NoteState.PENDING
    }

    return state.copy(activeNotes = remaining)
  }

  private fun updateGameStatus(state: GameState): GameState {
    return if (state.currentTime > state.song.duration) {
      state.copy(status = GameStatus.FINISHED)
    } else state
  }

  private fun judge(delta: Double): Judgement? {
    return when {
      delta <= hitWindow.perfect -> Judgement.PERFECT
      delta <= hitWindow.good -> Judgement.GOOD
      else -> null
    }
  }
}