package com.lucasalfare.flgf.a_domain

/**
 * Represents the complete state of a gameplay session at a given moment in time.
 *
 * This is the central structure of the Domain layer, acting as a snapshot
 * of everything that matters for gameplay logic.
 *
 * It contains:
 * - The current song being played
 * - The current time in the song
 * - Active notes currently in play
 * - Progression control (next note index)
 * - Score-related data (score, combo, multiplier)
 * - Special system state (energy and activation)
 * - Overall game status (playing, paused, finished)
 *
 * Important characteristics:
 * - It is immutable (recommended usage)
 * - It contains no business logic
 * - It is updated externally by Use Cases (Game Engine)
 *
 * In the architecture:
 * - It belongs to the Domain layer
 * - It is the main data structure manipulated by application logic
 * - It replaces the legacy global state (dGlobal) in a structured way
 *
 * Conceptually:
 * This is a "snapshot" of the game at a specific time,
 * making it predictable, testable, and easy to reason about.
 */
data class GameState(
  val song: Song,
  val currentTime: Double = 0.0,
  val activeNotes: List<ActiveNote> = emptyList(),
  val pressedLanes: Set<Int> = emptySet(),
  val nextNoteIndex: Int = 0,
  val score: Int = 0,
  val combo: Int = 0,
  val maxCombo: Int = 0,
  val multiplier: Int = 1,
  val specialEnergy: Int = 0,
  val specialActive: Boolean = false,
  val status: GameStatus = GameStatus.PLAYING
)