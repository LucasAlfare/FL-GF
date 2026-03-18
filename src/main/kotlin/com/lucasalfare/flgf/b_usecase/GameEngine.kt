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

  private val sustainScorePerSecond = 50.0

  fun tick(state: GameState, input: InputFrame): GameState {

    if (state.status != GameStatus.PLAYING) return state

    val currentTime = clock.currentTime()
    val deltaTime = currentTime - state.currentTime

    var specialState = specialSystem.update(state.specialState, deltaTime)

    if (input.activateSpecial) {
      specialState = specialSystem.tryActivate(specialState)
    }

    val (spawned, nextIndex) = spawner.spawn(state.copy(currentTime = currentTime))

    var notes = spawned
    var scoreState = state.scoreState

    // HIT
    notes = notes.map { active ->

      if (active.state != NoteState.PENDING) return@map active

      val note = active.note
      val isPressed = input.pressedFrets.contains(note.lane)

      if (isPressed) {
        val judgement = hitJudge.judge(note, currentTime, hitWindow)

        if (judgement != Judgement.MISS) {
          val base = scoreSystem.apply(scoreState, judgement)
          val mult = specialSystem.multiplier(specialState)

          scoreState = base.copy(score = base.score * mult)
          specialState = specialSystem.onHit(specialState, note)

          return@map active.copy(
            state = if (note.duration > 0) NoteState.HOLDING else NoteState.HIT,
            hitTime = currentTime
          )
        }
      }

      active
    }

    // MISS
    notes = notes.map { active ->

      if (active.state != NoteState.PENDING) return@map active

      val note = active.note

      if (currentTime > note.time + hitWindow.good) {
        scoreState = scoreSystem.apply(scoreState, Judgement.MISS)

        return@map active.copy(state = NoteState.MISSED)
      }

      active
    }

    // SUSTAIN
    notes = notes.map { active ->

      if (active.state != NoteState.HOLDING && active.state != NoteState.HIT) return@map active

      val note = active.note
      val hitTime = active.hitTime ?: return@map active

      if (note.duration <= 0) return@map active

      val elapsed = currentTime - hitTime
      val progress = elapsed.coerceAtMost(note.duration)

      val isHolding = input.pressedFrets.contains(note.lane)

      var newState: NoteState
      var newProgress = active.sustainProgress

      if (isHolding) {
        newState = NoteState.HOLDING

        val delta = (progress - active.sustainProgress).coerceAtLeast(0.0)
        val mult = specialSystem.multiplier(specialState)

        val gained = (delta * sustainScorePerSecond * mult).toInt()

        scoreState = scoreState.copy(score = scoreState.score + gained)

        newProgress = progress
      } else {
        newState = NoteState.HIT
      }

      active.copy(state = newState, sustainProgress = newProgress)
    }

    return state.copy(
      currentTime = currentTime,
      activeNotes = notes,
      nextNoteIndex = nextIndex,
      scoreState = scoreState,
      specialState = specialState,
      pressedLanes = input.pressedFrets
    )
  }
}