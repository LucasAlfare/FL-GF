package com.lucasalfare.kgf.a_domain.entities

/**
 * Abstraction for the game's time source.
 *
 * The purpose of this interface is to decouple the Domain layer
 * from any concrete time implementation (such as system time,
 * audio playback position, or frame-based timing).
 *
 * In the legacy code, time is implicitly controlled by frame updates
 * and mixed with rendering and audio logic. This abstraction isolates
 * time as a pure dependency.
 *
 * Responsibilities:
 * - Provide the current time of the game in seconds
 *
 * It does NOT:
 * - Control time progression
 * - Depend on any framework or engine
 * - Handle updates or scheduling
 *
 * In the architecture:
 * - It belongs to the Domain layer as an abstraction
 * - Implementations will live in outer layers (e.g., infrastructure)
 *
 * This allows:
 * - Swapping time sources (audio-synced, system clock, test clock)
 * - Deterministic testing of gameplay logic
 */
interface GameClock {
  fun currentTime(): Double
}