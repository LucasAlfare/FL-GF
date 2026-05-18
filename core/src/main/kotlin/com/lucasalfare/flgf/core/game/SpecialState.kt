package com.lucasalfare.flgf.core.game

/**
 *
 * Tracks the special ability (e.g., "star power") system.
 *
 * @property energy Current stored energy (0â€“100).
 *
 * @property active Whether the special mode is currently active.
 *
 * @property inSequence Whether the player is currently inside a valid special-note sequence.
 *
 * @property sequenceBroken Whether the current sequence has been invalidated.
 */
data class SpecialState(
  var energy: Int = 0,
  var active: Boolean = false,
  var inSequence: Boolean = false,
  var sequenceBroken: Boolean = false,
  var drainAccumulator: Double = 0.0
)
