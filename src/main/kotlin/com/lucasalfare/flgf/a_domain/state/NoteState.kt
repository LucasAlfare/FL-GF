package com.lucasalfare.flgf.a_domain.state

/**
 * Lifecycle state of a note during gameplay.
 */
enum class NoteState {
  PENDING,
  HIT,
  MISSED,
  HOLDING
}