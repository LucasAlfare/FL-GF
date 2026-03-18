package com.lucasalfare.flgf.a_domain.state

/**
 * Holds special (star power) state.
 */
data class SpecialState(
  val energy: Double = 0.0,
  val active: Boolean = false
)