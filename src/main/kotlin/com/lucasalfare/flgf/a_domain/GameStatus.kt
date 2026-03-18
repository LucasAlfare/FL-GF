package com.lucasalfare.flgf.a_domain

/**
 * Represents the high-level state of the game session.
 *
 * This defines whether the game is actively running,
 * paused, or finished.
 *
 * States:
 * - PLAYING: The game is actively updating and processing input
 * - PAUSED: The game is temporarily halted
 * - FINISHED: The song has ended or gameplay is complete
 *
 * This enum is used inside GameState to control flow
 * in higher-level application logic.
 *
 * It contains no behavior and belongs to the Domain layer.
 */
enum class GameStatus {
  PLAYING,
  PAUSED,
  FINISHED
}