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

package com.lucasalfare.flgf

import kotlin.math.abs

data class PlayerInput(
  val pressedFrets: Set<Int>,
  val justPressedFrets: Set<Int>,
  val activateSpecial: Boolean = false
)

data class Note(
  val time: Double,
  val lane: Int,
  val duration: Double,
  val specialPhraseId: Int? = null
)

data class Song(
  val title: String,
  val artist: String,
  val duration: Double,
  val notes: List<Note>
)

enum class Judgement { HIT, MISS }

data class HitWindow(val window: Double)

enum class NoteState { PENDING, HIT, MISSED, HOLDING, DONE }

data class ActiveNote(
  val note: Note,
  val state: NoteState = NoteState.PENDING,
  val sustainProgress: Double = 0.0,
  val hitTime: Double? = null,
  val sustainMultiplierSnapshot: Int = 1
)

data class ScoreState(
  val score: Int = 0,
  val combo: Int = 0,
  val maxCombo: Int = 0,
  val multiplier: Int = 1
)

data class SpecialState(
  val energy: Int = 0,
  val active: Boolean = false,
  val currentPhraseId: Int? = null,
  val phraseHitCount: Int = 0,
  val phraseTotal: Int = 0
)

enum class GameStatus { PLAYING, PAUSED, FINISHED }

data class GameState(
  val song: Song,
  val currentTime: Double = 0.0,
  val activeNotes: List<ActiveNote> = emptyList(),
  val nextNoteIndex: Int = 0,
  val scoreState: ScoreState = ScoreState(),
  val specialState: SpecialState = SpecialState(),
  val status: GameStatus = GameStatus.PLAYING
)

class HitJudge {
  fun judge(note: Note, inputTime: Double, window: HitWindow): Judgement =
    if (abs(inputTime - note.time) <= window.window) Judgement.HIT else Judgement.MISS
}

class ScoreSystem {
  private val baseScore = 50

  fun apply(state: ScoreState, judgement: Judgement, specialMultiplier: Int): ScoreState = when (judgement) {
    Judgement.MISS -> state.copy(combo = 0, multiplier = 1)
    Judgement.HIT -> {
      val combo = state.combo + 1
      val mult = calculateMultiplier(combo)
      val gained = baseScore * mult * specialMultiplier
      state.copy(
        score = state.score + gained,
        combo = combo,
        maxCombo = maxOf(state.maxCombo, combo),
        multiplier = mult
      )
    }
  }

  private fun calculateMultiplier(combo: Int) = when {
    combo >= 30 -> 4
    combo >= 20 -> 3
    combo >= 10 -> 2
    else -> 1
  }
}

class SpecialSystem {
  private val energyPerPhrase = 25
  private val drainPerSecond = 25.0

  fun onNoteHit(state: SpecialState, note: Note, all: List<Note>): SpecialState {
    if (state.active) return state
    val id = note.specialPhraseId ?: return state

    return if (state.currentPhraseId != id) {
      val total = all.count { it.specialPhraseId == id }
      state.copy(currentPhraseId = id, phraseHitCount = 1, phraseTotal = total)
    } else {
      val count = state.phraseHitCount + 1
      if (count >= state.phraseTotal) {
        state.copy(
          energy = (state.energy + energyPerPhrase).coerceAtMost(100),
          currentPhraseId = null,
          phraseHitCount = 0,
          phraseTotal = 0
        )
      } else state.copy(phraseHitCount = count)
    }
  }

  fun onNoteMiss(state: SpecialState, note: Note): SpecialState {
    val id = note.specialPhraseId ?: return state
    return if (state.currentPhraseId == id) state.copy(currentPhraseId = null, phraseHitCount = 0, phraseTotal = 0) else state
  }

  fun tryActivate(state: SpecialState) = if (state.energy >= 50) state.copy(active = true) else state

  fun update(state: SpecialState, dt: Double): SpecialState {
    if (!state.active) return state
    val energy = (state.energy - drainPerSecond * dt).toInt().coerceAtLeast(0)
    return state.copy(
      energy = energy,
      active = energy > 0,
      currentPhraseId = null,
      phraseHitCount = 0,
      phraseTotal = 0
    )
  }

  fun multiplier(state: SpecialState) = if (state.active) 2 else 1
}

class NoteSpawner(private val window: Double) {
  fun spawn(notes: List<Note>, time: Double, index: Int, active: List<ActiveNote>): Pair<List<ActiveNote>, Int> {
    var i = index
    val result = active.toMutableList()
    while (i < notes.size && notes[i].time <= time + window) {
      result.add(ActiveNote(notes[i]))
      i++
    }
    return result to i
  }
}

class GameEngine(
  private val hitWindow: HitWindow,
  private val hitJudge: HitJudge,
  private val spawner: NoteSpawner,
  private val scoreSystem: ScoreSystem,
  private val specialSystem: SpecialSystem
) {
  private val sustainScorePerSecond = 50.0

  fun tick(state: GameState, input: PlayerInput, currentTime: Double): GameState {
    if (state.status != GameStatus.PLAYING) return state
    val dt = currentTime - state.currentTime

    var special = specialSystem.update(state.specialState, dt)
    if (input.activateSpecial) special = specialSystem.tryActivate(special)

    val (spawned, nextIndex) = spawner.spawn(state.song.notes, currentTime, state.nextNoteIndex, state.activeNotes)
    var notes = spawned.toMutableList()
    var score = state.scoreState
    val specialMult = specialSystem.multiplier(special)

    val pending = notes.filter { it.state == NoteState.PENDING }
    val groups = pending.groupBy { it.note.time }

    var anyHit = false
    val targetGroup = groups.minByOrNull { (_, group) -> group.minOf { abs(currentTime - it.note.time) } }

    targetGroup?.let { (_, group) ->
      val within = group.any { hitJudge.judge(it.note, currentTime, hitWindow) == Judgement.HIT }
      if (within && input.justPressedFrets.isNotEmpty()) {
        val expected = group.map { it.note.lane }.toSet()
        val pressed = input.justPressedFrets
        val correct = expected.intersect(pressed)
        if (correct.isNotEmpty()) {
          anyHit = true
          val isFullChord = correct.size == expected.size
          score = scoreSystem.apply(score, if (isFullChord) Judgement.HIT else Judgement.MISS, if (isFullChord) specialMult else 1)
          group.forEach { active ->
            val idx = notes.indexOf(active)
            if (active.note.lane in correct) {
              val snapshotMult = score.multiplier
              val newState = if (active.note.duration > 0) NoteState.HOLDING else NoteState.HIT
              notes[idx] = active.copy(state = newState, hitTime = currentTime, sustainMultiplierSnapshot = snapshotMult)
              special = specialSystem.onNoteHit(special, active.note, state.song.notes)
            } else {
              notes[idx] = active.copy(state = NoteState.MISSED)
              special = specialSystem.onNoteMiss(special, active.note)
            }
          }
        }
      }
    }

    notes = notes.map {
      if (it.state != NoteState.PENDING) return@map it
      if (hitJudge.judge(it.note, currentTime, hitWindow) == Judgement.MISS && currentTime > it.note.time) {
        score = scoreSystem.apply(score, Judgement.MISS, 1)
        special = specialSystem.onNoteMiss(special, it.note)
        it.copy(state = NoteState.MISSED)
      } else it
    }.toMutableList()

    val hasPending = pending.any { hitJudge.judge(it.note, currentTime, hitWindow) == Judgement.HIT }
    if (input.justPressedFrets.isNotEmpty() && !anyHit && hasPending) {
      score = scoreSystem.apply(score, Judgement.MISS, 1)
    }

    notes = notes.map {
      if (it.state != NoteState.HOLDING && it.state != NoteState.HIT) return@map it
      val note = it.note
      val hitTime = it.hitTime ?: return@map it
      if (note.duration <= 0) return@map it

      val elapsed = currentTime - hitTime
      val progress = elapsed.coerceAtMost(note.duration)
      val holding = note.lane in input.pressedFrets

      var newState = it.state
      var newProgress = it.sustainProgress

      if (holding) {
        newState = NoteState.HOLDING
        val delta = (progress - it.sustainProgress).coerceAtLeast(0.0)
        val gained = (delta * sustainScorePerSecond * it.sustainMultiplierSnapshot * specialMult).toInt()
        score = score.copy(score = score.score + gained)
        newProgress = progress
        if (progress >= note.duration) newState = NoteState.DONE
      } else {
        newState = NoteState.HIT
      }

      it.copy(state = newState, sustainProgress = newProgress)
    }.toMutableList()

    notes = notes.filter { !(it.state == NoteState.MISSED && currentTime > it.note.time + 1.0) && it.state != NoteState.DONE }.toMutableList()

    return state.copy(
      currentTime = currentTime,
      activeNotes = notes,
      nextNoteIndex = nextIndex,
      scoreState = score,
      specialState = special
    )
  }
}