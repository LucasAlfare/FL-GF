package com.lucasalfare.flgf.a_domain

/**
 * Represents the type of input interaction performed by the player.
 *
 * This enum abstracts the concept of pressing and releasing inputs,
 * which is essential for handling both simple notes and sustain notes.
 *
 * Types:
 * - PRESS: the player pressed a lane/button
 * - RELEASE: the player released a lane/button
 *
 * This abstraction allows the game to:
 * - Handle sustain notes correctly
 * - Support different input devices uniformly
 *
 * In the architecture:
 * - It belongs to the Domain layer
 * - It is used together with InputEvent
 */
enum class InputType {
  PRESS,
  RELEASE
}