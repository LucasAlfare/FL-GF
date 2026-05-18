package com.lucasalfare.flgf.core.view.render

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.lucasalfare.flgf.core.game.SpecialState
import com.lucasalfare.flgf.core.view.PlayfieldLayout
import com.lucasalfare.flgf.core.view.PlayfieldPerspective
import com.lucasalfare.flgf.core.view.SpecialMeterConfig

/**
 * Draws the special meter as a vertical bar outside the playfield.
 *
 * TODO: perspectiva ainda não tá aplicada corretamente nesse aqui.
 */
class SpecialMeterRenderer(
  private val layout: PlayfieldLayout,
  private val perspective: PlayfieldPerspective,
  private val config: SpecialMeterConfig = SpecialMeterConfig()
) {
  fun draw(shapeRenderer: ShapeRenderer, special: SpecialState) {
    val x = layout.left + layout.width + config.marginRight
    val y = layout.bottom + config.marginBottom
    val clampedEnergy = special.energy.coerceIn(0, config.maximumEnergy)
    val innerWidth = (config.width - config.innerPadding * 2f).coerceAtLeast(0f)
    val innerHeight = (config.height - config.innerPadding * 2f).coerceAtLeast(0f)
    val filledHeight = innerHeight * (clampedEnergy / config.maximumEnergy.toFloat())

    shapeRenderer.color = config.backgroundColor
    perspective.drawProjectedRect(shapeRenderer, x, y, config.width, config.height)

    shapeRenderer.color = if (special.active) config.activeFillColor else config.fillColor
    perspective.drawProjectedRect(
      shapeRenderer,
      x + config.innerPadding,
      y + config.innerPadding,
      innerWidth,
      filledHeight
    )
  }
}
