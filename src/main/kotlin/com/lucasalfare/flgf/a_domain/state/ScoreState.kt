package com.lucasalfare.flgf.a_domain.state

/**
 * Holds score-related state.
 */
data class ScoreState(
  val score: Int = 0,
  val combo: Int = 0,
  val maxCombo: Int = 0,
  val multiplier: Int = 1
)