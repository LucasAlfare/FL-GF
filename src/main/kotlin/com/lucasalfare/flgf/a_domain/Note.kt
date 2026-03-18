package com.lucasalfare.flgf.a_domain

/**
 * Represents a single musical note definition.
 *
 * Immutable and contains no gameplay logic.
 */
data class Note(
  val time: Double,
  val lane: Int,
  val duration: Double,
  val special: Boolean,
  val hopo: Boolean
)