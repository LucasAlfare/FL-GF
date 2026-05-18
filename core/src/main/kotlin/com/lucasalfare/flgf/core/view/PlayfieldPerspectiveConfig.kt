package com.lucasalfare.flgf.core.view

/**
 * Lightweight 3D-like rotation for the playfield rendered as 2D.
 *
 * The gameplay still lives in orthographic world space. This helper only transforms
 * render coordinates so you can think in top-down layout and rotate everything at the end.
 */
data class PlayfieldPerspectiveConfig(
  var enabled: Boolean = true,
  var rotationXDegrees: Float = 0f,
  var rotationYDegrees: Float = 0f,
  var rotationZDegrees: Float = 0f,
  var cameraDistanceMultiplier: Float = 2.5f
) {
  init {
    require(cameraDistanceMultiplier > 0f) { "cameraDistanceMultiplier must be > 0" }
  }
}
