package com.lucasalfare.kgf.a_domain.entities

/**
 * Represents a player's input action in the game timeline.
 *
 * This is a domain-level abstraction of input, independent from
 * any specific input device (keyboard, controller, etc.).
 *
 * Instead of dealing with raw key codes, the game operates with
 * logical actions such as pressing or releasing a lane at a given time.
 *
 * Responsibilities:
 * - Represent a discrete input event
 * - Provide timing information for gameplay evaluation
 *
 * Fields:
 * - time: when the input occurred (in seconds)
 * - lane: which lane/button was affected (1 to 5)
 * - type: whether the input was a press or release
 *
 * It does NOT:
 * - Know anything about physical devices
 * - Perform input handling
 * - Interact with rendering or audio
 *
 * In the architecture:
 * - It belongs to the Domain layer
 * - It is produced by outer layers (input adapters)
 * - It is consumed by gameplay logic
 */
data class InputEvent(
  val time: Double,
  val lane: Int,
  val type: InputType
)