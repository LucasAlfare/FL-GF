package com.lucasalfare.flgf.core.view

/**
 * Single source of truth for playfield geometry.
 * Track background, hit spots and notes all use these same bounds.
 */
class PlayfieldLayout(
  private val config: GameConfig,
  val laneCount: Int = 5,
  val laneGap: Float = 0.004f,
  val spotHeight: Float = 0.15f,
  val noteHeight: Float = 0.1f,
  val sustainBodyWidthRatio: Float = 0.32f
) {
  val left: Float
    get() = config.playfieldCenterX - config.playfieldWidth / 2f

  val bottom: Float
    get() = config.playfieldCenterY - config.playfieldHeight / 2f

  val width: Float
    get() = config.playfieldWidth

  val height: Float
    get() = config.playfieldHeight

  val top: Float
    get() = bottom + height

  val hitLineY: Float
    get() = bottom + config.hitLineInsetFromBottom

  val laneWidth: Float
    get() = width / laneCount

  val noteSpeedPerMs: Float
    get() = config.noteSpeedPerMs

  fun xForLane(lane: Int): Float = left + lane * laneWidth
}
