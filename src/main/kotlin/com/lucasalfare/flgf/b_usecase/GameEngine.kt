package com.lucasalfare.flgf.b_usecase

import com.lucasalfare.flgf.a_domain.GameClock
import com.lucasalfare.flgf.a_domain.GameStatus
import com.lucasalfare.flgf.a_domain.HitJudge
import com.lucasalfare.flgf.a_domain.HitWindow
import com.lucasalfare.flgf.a_domain.InputFrame
import com.lucasalfare.flgf.a_domain.Judgement
import com.lucasalfare.flgf.a_domain.NoteSpawner
import com.lucasalfare.flgf.a_domain.ScoreSystem
import com.lucasalfare.flgf.a_domain.SpecialSystem
import com.lucasalfare.flgf.a_domain.state.GameState
import com.lucasalfare.flgf.a_domain.state.NoteState

/**
 * Core game loop logic.
 *
 * This is a pure use-case layer component.
 * It receives a GameState and returns a new GameState.
 *
 * It is responsible for:
 * - Advancing time
 * - Spawning notes
 * - Processing input (hit/miss)
 * - Updating score and combo
 * - Updating special system
 * - Updating note lifecycle
 *
 * It does NOT:
 * - Render anything
 * - Access framework APIs
 * - Load external data
 */
class GameEngine(
  private val clock: GameClock,
  private val hitWindow: HitWindow,
  private val hitJudge: HitJudge,
  private val spawner: NoteSpawner,
  private val scoreSystem: ScoreSystem,
  private val specialSystem: SpecialSystem
) {

  // TODO: rule mixed here is not good...
  private val sustainScorePerSecond = 50.0

  fun tick(state: GameState, input: InputFrame): GameState {

    if (state.status != GameStatus.PLAYING) return state

    val currentTime = clock.currentTime()

    val (spawnedNotes, nextIndex) = spawner.spawn(
      state.copy(currentTime = currentTime)
    )

    var updatedNotes = spawnedNotes
    var scoreState = state.scoreState
    var specialState = state.specialState

    // =============================
    // 1. HIT DETECTION
    // =============================
    updatedNotes = updatedNotes.map { active ->
      if (active.state != NoteState.PENDING) return@map active

      val note = active.note
      val isCorrectLanePressed = input.pressedFrets.contains(note.lane)

      val canHit = if (note.hopo) {
        isCorrectLanePressed
      } else {
        input.strum && isCorrectLanePressed
      }

      if (canHit) {
        val judgement = hitJudge.judge(note, currentTime, hitWindow)

        if (judgement != Judgement.MISS) {
          scoreState = scoreSystem.apply(scoreState, judgement)
          specialState = specialSystem.onHit(specialState, note, judgement)

          return@map active.copy(
            state = if (note.duration > 0) NoteState.HOLDING else NoteState.HIT,
            hitTime = currentTime,
            judgement = judgement
          )
        }
      }

      active
    }

    // =============================
    // 2. MISS DETECTION
    // =============================
    updatedNotes = updatedNotes.map { active ->
      if (active.state != NoteState.PENDING) return@map active

      val note = active.note

      if (currentTime > note.time + hitWindow.good) {
        scoreState = scoreSystem.apply(scoreState, Judgement.MISS)

        return@map active.copy(
          state = NoteState.MISSED,
          judgement = Judgement.MISS
        )
      }

      active
    }

    // =============================
    // 3. SUSTAIN LOGIC (CORRECT BEHAVIOR)
    // =============================
    updatedNotes = updatedNotes.map { active ->
      if (active.state != NoteState.HOLDING && active.state != NoteState.HIT) return@map active

      val note = active.note
      val hitTime = active.hitTime ?: return@map active

      if (note.duration <= 0) return@map active

      val elapsed = currentTime - hitTime
      val clampedProgress = elapsed.coerceAtMost(note.duration)

      val wasHolding = active.state == NoteState.HOLDING
      val isHoldingNow = input.pressedFrets.contains(note.lane)

      var newState = active.state
      var newProgress = active.sustainProgress

      if (isHoldingNow) {
        newState = NoteState.HOLDING

        val delta = (clampedProgress - active.sustainProgress).coerceAtLeast(0.0)

        if (delta > 0) {
          val gained = (delta * sustainScorePerSecond).toInt()
          scoreState = scoreState.copy(score = scoreState.score + gained)
        }

        newProgress = clampedProgress
      } else {
        if (wasHolding) {
          newState = NoteState.HIT
        }
      }

      active.copy(
        state = newState,
        sustainProgress = newProgress
      )
    }

    return state.copy(
      currentTime = currentTime,
      activeNotes = updatedNotes,
      nextNoteIndex = nextIndex,
      scoreState = scoreState,
      specialState = specialState,
      pressedLanes = input.pressedFrets
    )
  }
}