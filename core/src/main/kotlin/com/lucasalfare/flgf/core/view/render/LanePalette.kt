package com.lucasalfare.flgf.core.view.render

import com.badlogic.gdx.graphics.Color

internal object LanePalette {
  private val colors = listOf(
    Color(0.2f, 0.8f, 0.2f, 1f),
    Color(0.8f, 0.2f, 0.2f, 1f),
    Color(0.9f, 0.8f, 0.1f, 1f),
    Color(0.2f, 0.2f, 0.9f, 1f),
    Color(0.95f, 0.5f, 0.15f, 1f),
  )

  fun colorForLane(lane: Int): Color = colors.getOrElse(lane) { Color(0.7f, 0.7f, 0.7f, 1f) }
}
