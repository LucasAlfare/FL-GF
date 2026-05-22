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
  private val specificSpecialConfig: SpecialMeterConfig = SpecialMeterConfig()
) {
  fun draw(shapeRenderer: ShapeRenderer, special: SpecialState) {
    val x = layout.left + layout.width + specificSpecialConfig.marginRight
    val y = layout.bottom + specificSpecialConfig.marginBottom
    val clampedEnergy = special.energy.coerceIn(0, specificSpecialConfig.maximumEnergy)
    val innerWidth = (specificSpecialConfig.width - specificSpecialConfig.innerPadding * 2f).coerceAtLeast(0f)
    val innerHeight = (specificSpecialConfig.height - specificSpecialConfig.innerPadding * 2f).coerceAtLeast(0f)
    val filledHeight = innerHeight * (clampedEnergy / specificSpecialConfig.maximumEnergy.toFloat())

    shapeRenderer.color = specificSpecialConfig.backgroundColor
    perspective.drawProjectedRect(shapeRenderer, x, y, specificSpecialConfig.width, specificSpecialConfig.height)

    shapeRenderer.color = if (special.active) specificSpecialConfig.activeFillColor else specificSpecialConfig.fillColor
    perspective.drawProjectedRect(
      shapeRenderer,
      x + specificSpecialConfig.innerPadding,
      y + specificSpecialConfig.innerPadding,
      innerWidth,
      filledHeight
    )
  }
}
