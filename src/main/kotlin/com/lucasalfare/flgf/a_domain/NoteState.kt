package com.lucasalfare.kgf.a_domain.entities

/**
 * Represents the current lifecycle state of a note during gameplay.
 *
 * This is used by ActiveNote to track how the player has interacted
 * with a given Note.
 *
 * States:
 * - PENDING: The note has not yet been hit or missed
 * - HIT: The note was successfully played
 * - MISSED: The note passed without being played correctly
 * - HOLDING: The note is a sustain and is currently being held
 *
 * This enum is part of the Domain layer and contains no behavior,
 * only state representation.
 */
enum class NoteState {
  PENDING,
  HIT,
  MISSED,
  HOLDING
}