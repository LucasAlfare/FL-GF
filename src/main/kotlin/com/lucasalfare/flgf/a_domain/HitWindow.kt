package com.lucasalfare.kgf.a_domain.entities

/**
 * Defines the timing windows used to evaluate player input accuracy.
 *
 * This structure centralizes the rules that determine whether a note
 * is considered a perfect hit, a good hit, or a miss based on the
 * difference between input time and note time.
 *
 * In the legacy code, these thresholds are implicitly scattered across
 * conditionals and magic numbers. This class makes them explicit and configurable.
 *
 * Each value represents the maximum allowed time difference (in seconds)
 * between the expected note time and the player's input.
 *
 * Example:
 * - perfect: ±0.05s
 * - good: ±0.1s
 * - miss: anything beyond that
 *
 * Responsibilities:
 * - Define timing thresholds for judgement
 *
 * It does NOT:
 * - Perform the judgement itself
 * - Interact with input or notes
 *
 * In the architecture:
 * - It belongs to the Domain layer
 * - It is used by gameplay logic (Use Cases) to evaluate input accuracy
 */
data class HitWindow(
  val perfect: Double,
  val good: Double
)