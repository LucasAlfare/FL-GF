package com.lucasalfare.flgf.a_domain

import com.lucasalfare.flgf.a_domain.state.NoteState

/**
 * Runtime representation of a Note.
 */
data class ActiveNote(
  val note: Note,
  val state: NoteState = NoteState.PENDING,
  val activatedAt: Double? = null,
  val sustainProgress: Double = 0.0,
  val hitTime: Double? = null,
  val judgement: Judgement? = null
)