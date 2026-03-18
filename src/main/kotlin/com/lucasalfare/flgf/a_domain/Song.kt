package com.lucasalfare.flgf.a_domain

/**
 * Represents a full playable song.
 */
data class Song(
  val title: String,
  val artist: String,
  val duration: Double,
  val notes: List<Note>
)