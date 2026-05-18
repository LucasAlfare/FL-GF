package com.lucasalfare.flgf.core.game

/**
 *
 * Represents a note in the song chart.
 *
 * @property hitTime Timestamp (in milliseconds) when the note should be hit.
 *
 * @property lane Which lane/fret this note belongs to.
 *
 * @property duration Duration (in milliseconds) for sustain notes (0 = tap note).
 *
 * @property isSpecial Whether this note contributes to special energy sequences.
 */
data class Note(val hitTime: Long, val lane: Int, val duration: Long = 0L, val isSpecial: Boolean = false)
