package com.lucasalfare.flgf.core.game

/**
 *
 * Represents the player input for a single frame/tick.
 *
 * @property pressedFrets Frets currently being held down.
 *
 * @property justPressedFrets Frets that were pressed exactly on this frame (edge-triggered).
 *
 * @property justReleasedFrets Frets that were released exactly on this frame (edge-triggered).
 *
 * @property activateSpecial Whether the player is attempting to activate the special mode.
 */
data class PlayerInput(
  val pressedFrets: Set<Int>,
  val justPressedFrets: Set<Int>,
  val justReleasedFrets: Set<Int> = emptySet(),
  val activateSpecial: Boolean = false
)
