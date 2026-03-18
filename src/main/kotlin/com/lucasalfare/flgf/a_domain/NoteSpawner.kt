package com.lucasalfare.flgf.a_domain

import com.lucasalfare.flgf.a_domain.state.GameState

/**
 * Responsible for spawning notes based on current time.
 */
class NoteSpawner(private val spawnWindow: Double) {

  fun spawn(state: GameState): Pair<List<ActiveNote>, Int> {
    val notes = state.song.notes
    var index = state.nextNoteIndex
    val active = state.activeNotes.toMutableList()

    while (index < notes.size && notes[index].time <= state.currentTime + spawnWindow) {
      active.add(ActiveNote(notes[index]))
      index++
    }

    return active to index
  }
}