package com.lucasalfare.flgf.a_domain

/**
 * Aggregated input state for a frame/tick.
 *
 * IMPORTANT: supports chords and strum mechanics.
 */
data class InputFrame(
  val time: Double,
  val pressedFrets: Set<Int>,
  val events: List<InputEvent> = emptyList(),
  val activateSpecial: Boolean = false
)