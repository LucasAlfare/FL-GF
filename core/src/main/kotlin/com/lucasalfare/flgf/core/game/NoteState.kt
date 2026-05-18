package com.lucasalfare.flgf.core.game

/**
 *
 * Runtime representation of a note currently active in the game.
 *
 * This wraps a static [Note] and adds mutable state used during gameplay.
 *
 * @property note The original immutable note data.
 *
 * @property hit Whether the note has been successfully hit.
 *
 * @property missed Whether the note has been missed.
 *
 * @property holding Whether the player is currently holding a sustain note.
 *
 * @property sustainProgress How much of the sustain duration has been completed.
 *
 * @property sustainBroken Whether the player released a sustain before it finished.
 */
data class NoteState(
  val note: Note,
  var hit: Boolean = false,
  var missed: Boolean = false,
  var holding: Boolean = false,
  var sustainProgress: Double = 0.0,
  var sustainBroken: Boolean = false,
  var specialDisabled: Boolean = false
)
