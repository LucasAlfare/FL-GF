package com.lucasalfare.flgf.a_domain.state

import com.lucasalfare.flgf.a_domain.ActiveNote
import com.lucasalfare.flgf.a_domain.GameStatus
import com.lucasalfare.flgf.a_domain.Song

/**
 * Complete snapshot of the game at a given moment.
 */
data class GameState(
  val song: Song,
  val currentTime: Double = 0.0,
  val activeNotes: List<ActiveNote> = emptyList(),
  val pressedLanes: Set<Int> = emptySet(),
  val nextNoteIndex: Int = 0,
  val scoreState: ScoreState = ScoreState(),
  val specialState: SpecialState = SpecialState(),
  val status: GameStatus = GameStatus.PLAYING
)