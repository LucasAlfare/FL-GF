package com.lucasalfare.flgf.a_domain

import com.lucasalfare.flgf.a_domain.state.SpecialState

/**
 * Handles special energy accumulation and activation.
 */
class SpecialSystem {

  private val maxEnergy = 100.0
  private val gainPerNote = 10.0
  private val drainPerSecond = 20.0

  fun onHit(state: SpecialState, note: Note): SpecialState {
    if (!note.special) return state

    return state.copy(
      energy = (state.energy + gainPerNote).coerceAtMost(maxEnergy)
    )
  }

  fun update(state: SpecialState, deltaTime: Double): SpecialState {
    if (!state.active) return state

    val energy = (state.energy - drainPerSecond * deltaTime)
      .coerceAtLeast(0.0)

    return state.copy(
      energy = energy,
      active = energy > 0
    )
  }

  fun tryActivate(state: SpecialState): SpecialState {
    if (state.energy < 50.0) return state
    return state.copy(active = true)
  }

  fun multiplier(state: SpecialState): Int {
    return if (state.active) 2 else 1
  }
}