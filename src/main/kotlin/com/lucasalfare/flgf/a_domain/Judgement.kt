package com.lucasalfare.kgf.a_domain.entities

/**
 * Represents the result of evaluating a player's input against a note.
 *
 * This defines the quality of the hit and is used by gameplay systems
 * such as scoring, combo tracking, and feedback.
 *
 * In the legacy code, this concept is implicit and tied to frame states
 * and scattered conditions. Here it is made explicit and reusable.
 *
 * Values:
 * - PERFECT: input was very close to the note timing
 * - GOOD: input was acceptable but not perfect
 * - MISS: input was too late, too early, or absent
 *
 * Responsibilities:
 * - Provide a standardized result for hit evaluation
 *
 * It does NOT:
 * - Perform the evaluation itself
 *
 * In the architecture:
 * - It belongs to the Domain layer
 * - It is used by gameplay logic and scoring systems
 */
enum class Judgement {
  PERFECT,
  GOOD,
  MISS
}