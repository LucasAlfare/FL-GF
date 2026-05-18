package com.lucasalfare.flgf.core.view.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.lucasalfare.flgf.core.view.PlayfieldLayout
import com.lucasalfare.flgf.core.view.PlayfieldPerspective

/**
 * Draws the track background and the lane separators.
 */
class TrackRenderer(
  private val layout: PlayfieldLayout,
  private val perspective: PlayfieldPerspective
) {
  fun draw(shapeRenderer: ShapeRenderer) {
    shapeRenderer.color = Color(0.12f, 0.12f, 0.15f, 1f)
    perspective.drawProjectedRect(
      shapeRenderer,
      layout.left,
      layout.bottom,
      layout.width,
      layout.height
    )

    shapeRenderer.color = Color(0.3f, 0.3f, 0.35f, 1f)
    for (i in 1 until layout.laneCount) {
      val x = layout.left + i * layout.laneWidth
      perspective.drawProjectedRect(shapeRenderer, x, layout.bottom, 0.002f, layout.height)
    }
  }
}
