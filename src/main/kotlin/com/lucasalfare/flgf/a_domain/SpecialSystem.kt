package com.lucasalfare.flgf.a_domain

import com.lucasalfare.flgf.a_domain.state.SpecialState

/**
 * Handles special energy accumulation and activation.
 */
class SpecialSystem {
  fun onHit(state: SpecialState, note: Note, judgement: Judgement): SpecialState {
    val gain = if (note.special && judgement != Judgement.MISS) 0.05 else 0.0
    return state.copy(energy = (state.energy + gain).coerceAtMost(1.0))
  }

  fun activate(state: SpecialState): SpecialState {
    return if (state.energy >= 0.5) {
      state.copy(isActive = true, energy = state.energy - 0.5)
    } else state
  }
}