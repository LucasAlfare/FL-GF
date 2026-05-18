package com.lucasalfare.flgf.core.view

import com.badlogic.gdx.graphics.Color

/**
 * Layout and color tuning for the special meter HUD.
 *
 * The meter stays outside the track, anchored to the bottom-right area of the
 * viewport in the current top-down presentation.
 */
data class SpecialMeterConfig(
  val width: Float = 0.075f,
  val height: Float = 0.48f,
  val marginRight: Float = 0.03f,
  val marginBottom: Float = 0.08f,
  val innerPadding: Float = 0.008f,
  val maximumEnergy: Int = 100,
  val backgroundColor: Color = Color(0.08f, 0.08f, 0.1f, 0.85f),
  val fillColor: Color = Color(0.95f, 0.84f, 0.2f, 1f),
  val activeFillColor: Color = Color(0.2f, 0.9f, 0.95f, 1f)
) {
  init {
    require(width > 0f) { "width must be > 0" }
    require(height > 0f) { "height must be > 0" }
    require(innerPadding >= 0f) { "innerPadding must be >= 0" }
    require(maximumEnergy > 0) { "maximumEnergy must be > 0" }
  }
}
