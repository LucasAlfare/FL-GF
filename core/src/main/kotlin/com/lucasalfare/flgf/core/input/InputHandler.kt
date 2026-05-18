package com.lucasalfare.flgf.core.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.lucasalfare.flgf.core.game.PlayerInput

// This input helper can stay simple for now. TODO: in the future implement this using bit masking, to avoid allocations
object InputHandler {
  private val laneKeys = intArrayOf(
    Input.Keys.E,
    Input.Keys.T,
    Input.Keys.U,
    Input.Keys.I,
    Input.Keys.O
  )
  private val previousPressed = BooleanArray(laneKeys.size)

  fun update(): PlayerInput {
    val pressed = BooleanArray(laneKeys.size)
    val justPressed = BooleanArray(laneKeys.size)
    val justReleased = BooleanArray(laneKeys.size)

    for (lane in laneKeys.indices) {
      pressed[lane] = Gdx.input.isKeyPressed(laneKeys[lane])
      justPressed[lane] = pressed[lane] && !previousPressed[lane]
      justReleased[lane] = !pressed[lane] && previousPressed[lane]
      previousPressed[lane] = pressed[lane]
    }

    return PlayerInput(
      pressedFrets = pressed.indices.filter { pressed[it] }.toSet(),
      justPressedFrets = justPressed.indices.filter { justPressed[it] }.toSet(),
      justReleasedFrets = justReleased.indices.filter { justReleased[it] }.toSet(),
      activateSpecial = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
    )
  }
}
