package com.lucasalfare.kgf.a_domain.entities

/**
 * Represents a full playable song in the game.
 *
 * A Song is essentially a timeline of Notes, ordered by time,
 * along with basic metadata such as title and artist.
 *
 * This is the main entry point of gameplay content in the domain.
 * It replaces the original XML structure used in the legacy project.
 *
 * Responsibilities:
 * - Hold all notes that define the gameplay
 * - Guarantee that notes are sorted in chronological order
 * - Provide basic metadata about the track
 *
 * It does NOT:
 * - Handle audio playback
 * - Perform timing updates
 * - Contain gameplay logic
 *
 * In the architecture:
 * - It is part of the Domain layer
 * - It is consumed by the Game Engine (Use Cases)
 */
data class Song(
  val title: String,
  val artist: String,
  val duration: Double,
  val notes: List<Note>
)