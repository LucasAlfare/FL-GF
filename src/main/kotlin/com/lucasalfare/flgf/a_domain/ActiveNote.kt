package com.lucasalfare.kgf.a_domain.entities

/**
 * Represents a Note during gameplay execution.
 *
 * While Note is a static definition (from the song),
 * ActiveNote adds runtime state to track player interaction.
 *
 * It exists because a note changes over time:
 * - It starts as pending
 * - It may be hit or missed
 * - It may enter a holding state if it's a sustain note
 *
 * Responsibilities:
 * - Track the current state of the note (via NoteState)
 * - Store when the note became active (optional timing reference)
 * - Track sustain progress for long notes
 *
 * It does NOT:
 * - Decide if a note should be hit or missed
 * - Handle scoring
 * - Interact with input or rendering
 *
 * In the architecture:
 * - It belongs to the Domain layer
 * - It is managed by the Game Engine (Use Cases)
 * - It is derived from Note and evolves during gameplay
 */
data class ActiveNote(
  val note: Note,
  val state: NoteState = NoteState.PENDING,
  val activatedAt: Double? = null,
  val sustainProgress: Double = 0.0
)