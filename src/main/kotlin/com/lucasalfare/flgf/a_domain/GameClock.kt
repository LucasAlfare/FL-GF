package com.lucasalfare.flgf.a_domain

/**
 * Abstraction for the game's time source.
 *
 * Decouples domain logic from any concrete timing mechanism
 * (system clock, audio sync, frame loop, etc).
 */
interface GameClock {
  fun currentTime(): Double
}