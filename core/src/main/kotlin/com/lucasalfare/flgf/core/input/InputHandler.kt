package com.lucasalfare.flgf.core.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.lucasalfare.flgf.core.game.PlayerInput

object InputHandler {
  private val laneKeys = intArrayOf(
    Input.Keys.E, Input.Keys.T, Input.Keys.U, Input.Keys.I, Input.Keys.O
  )
  private var previousMask: Int = 0

  private val pressedSet = mutableSetOf<Int>()
  private val justPressedSet = mutableSetOf<Int>()
  private val justReleasedSet = mutableSetOf<Int>()

  fun update(): PlayerInput {
    var pressed = 0
    for (i in laneKeys.indices) {
      if (Gdx.input.isKeyPressed(laneKeys[i])) pressed = pressed or (1 shl i)
    }

    val justPressed = pressed and previousMask.inv()
    val justReleased = previousMask and pressed.inv()
    previousMask = pressed

    pressedSet.clear()
    justPressedSet.clear()
    justReleasedSet.clear()

    for (i in laneKeys.indices) {
      if ((pressed shr i) and 1 == 1) pressedSet.add(i)
      if ((justPressed shr i) and 1 == 1) justPressedSet.add(i)
      if ((justReleased shr i) and 1 == 1) justReleasedSet.add(i)
    }

    return PlayerInput(
      pressedFrets = pressedSet,
      justPressedFrets = justPressedSet,
      justReleasedFrets = justReleasedSet,
      activateSpecial = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
    )
  }
}
