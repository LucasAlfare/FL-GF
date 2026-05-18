package com.lucasalfare.flgf.core.view

import kotlin.math.max

/**
 * World-space config for the prototype UI.
 * The playfield is centered inside the viewport and all renderers read from the same geometry.
 */
data class GameConfig(
  val noteSpeedPerMs: Float = 0.001975f,
  val playfieldWidth: Float = 0.72f,
  val playfieldHeight: Float = 2f, //4f considerando rotação no eixo em 70f; 2f se 0f de rotação
  val playfieldCenterX: Float = 0.5f,
  val playfieldCenterY: Float = 1.0f,
  val hitLineInsetFromBottom: Float = 0.14f,
  val hitWindow: Long = 100L,
  val perspective: PlayfieldPerspectiveConfig = PlayfieldPerspectiveConfig()
) {
  val trackTravelHeight: Float
    get() = max(0f, playfieldHeight - hitLineInsetFromBottom)

  /**
   * Time it takes a note to travel from the top of the visible lane area to the hit line.
   */
  val spawnAheadTime: Long
    get() = (trackTravelHeight / noteSpeedPerMs).toLong()
}
