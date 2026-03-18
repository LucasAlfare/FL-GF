package com.lucasalfare.flgf.a_domain

import com.lucasalfare.flgf.a_domain.state.GameState

/**
 * Responsible for spawning notes based on current time.
 */
class NoteSpawner(
  private val spawnWindow: Double = 2.0
) {
  fun spawn(state: GameState): Pair<List<ActiveNote>, Int> {
    val notes = state.song.notes
    var index = state.nextNoteIndex
    val newNotes = mutableListOf<ActiveNote>()

    while (index < notes.size && notes[index].time <= state.currentTime + spawnWindow) {
      newNotes.add(ActiveNote(note = notes[index]))
      index++
    }

    return Pair(state.activeNotes + newNotes, index)
  }
}