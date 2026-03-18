package com.lucasalfare.flgf.a_domain

/**
 * Represents a single musical note in the game timeline.
 *
 * This is a pure domain model that defines *what* should happen,
 * not *how* or *when* it is processed during gameplay.
 *
 * Each note describes:
 * - The exact moment it should be played (time)
 * - Which lane/button the player must press (lane)
 * - Whether it has a duration (sustain notes)
 * - Whether it contributes to special energy
 * - Whether it follows HOPO (hammer-on / pull-off) rules
 *
 * This class is immutable and contains no gameplay logic.
 *
 * In the architecture:
 * - It belongs to the Domain layer
 * - It is loaded from external sources (e.g., XML) but is not responsible for parsing
 * - It is later transformed into ActiveNote during gameplay
 */
data class Note(
  val time: Double,
  val lane: Int,
  val duration: Double,
  val special: Boolean,
  val hopo: Boolean
)