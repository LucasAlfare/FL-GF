package com.lucasalfare.flgf.core.view.render

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.lucasalfare.flgf.core.view.PlayfieldLayout
import com.lucasalfare.flgf.core.view.PlayfieldPerspective

/**
 * Draws the fixed hit spots on the hit line.
 */
class HitSpotRenderer(
  private val layout: PlayfieldLayout,
  private val perspective: PlayfieldPerspective
) {
  fun draw(shapeRenderer: ShapeRenderer) {
    for (lane in 0 until layout.laneCount) {
      shapeRenderer.color = LanePalette.colorForLane(lane)
      perspective.drawProjectedRect(
        shapeRenderer,
        layout.xForLane(lane),
        layout.hitLineY - layout.spotHeight / 2f,
        layout.laneWidth - layout.laneGap,
        layout.spotHeight
      )
    }
  }
}
