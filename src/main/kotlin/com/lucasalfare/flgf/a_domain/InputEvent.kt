package com.lucasalfare.flgf.a_domain

/**
 * Discrete input event (device-agnostic).
 */
data class InputEvent(
  val time: Double,
  val lane: Int,
  val type: InputType
)