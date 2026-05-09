package com.lucasalfare.flgf.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class GuitarFlashGame : ApplicationAdapter() {
  private lateinit var batch: com.badlogic.gdx.graphics.g2d.SpriteBatch
  private lateinit var shapeRenderer: ShapeRenderer
  private lateinit var laneRenderer: LaneRenderer
  private lateinit var hitSpotRenderer: HitSpotRenderer
  private lateinit var inputHandler: InputHandler

  override fun create() {
    batch = com.badlogic.gdx.graphics.g2d.SpriteBatch()
    shapeRenderer = ShapeRenderer()
    laneRenderer = LaneRenderer(shapeRenderer)
    hitSpotRenderer = HitSpotRenderer(shapeRenderer)
    inputHandler = InputHandler()
  }

  override fun render() {
    Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    // Update input and visual feedback
    val playerInput = inputHandler.updateInput()
    hitSpotRenderer.updatePressedFrets(playerInput.pressedFrets)

    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

    laneRenderer.render()
    hitSpotRenderer.render()

    shapeRenderer.end()
  }

  override fun dispose() {
    shapeRenderer.dispose()
    batch.dispose()
  }
}

/**
 * Responsible for rendering the 5 lanes of the guitar highway. Each lane is a vertical rectangle
 * where notes travel down.
 */
class LaneRenderer(private val shapeRenderer: ShapeRenderer) : Renderer {
  companion object {
    const val LANE_COUNT = 5
    const val LANE_WIDTH = 80f
    const val LANE_HEIGHT = 800f
    const val LANE_SPACING = 10f
    const val START_X = 200f
    const val START_Y = 50f
  }

  override fun render() {
    shapeRenderer.color = Color.GRAY

    for (i in 0 until LANE_COUNT) {
      val x = START_X + i * (LANE_WIDTH + LANE_SPACING)
      shapeRenderer.rect(x, START_Y, LANE_WIDTH, LANE_HEIGHT)
    }
  }
}

/**
 * Abstract interface for rendering components. Allows swapping between different rendering
 * strategies (shapes, textures, etc).
 */
interface Renderer {
  fun render()
}


/**
 * Responsible for rendering the hit spots at the bottom of each lane. These are the target areas
 * where players need to hit the notes.
 */
class HitSpotRenderer(private val shapeRenderer: ShapeRenderer) : Renderer {
  companion object {
    const val SPOT_SIZE = 60f
    const val SPOT_Y = LaneRenderer.START_Y - 20f
    const val PRESSED_SCALE = 0.8f
    const val ANIMATION_SPEED = 0.15f
  }

  /** Current scale for each lane (0-4). Used for press animation */
  private val scales = FloatArray(LaneRenderer.LANE_COUNT) { 1.0f }

  /**
   * Updates the visual state based on which frets are currently pressed.
   * 
   * @param pressedFrets Set of lane indices currently being pressed
   */
  fun updatePressedFrets(pressedFrets: Set<Int>) {
    for (i in 0 until LaneRenderer.LANE_COUNT) {
      if (i in pressedFrets) {
        // Shrink when pressed
        scales[i] = (scales[i] - ANIMATION_SPEED).coerceAtLeast(PRESSED_SCALE)
      } else {
        // Expand back to normal when released
        scales[i] = (scales[i] + ANIMATION_SPEED).coerceAtMost(1.0f)
      }
    }
  }

  override fun render() {
    for (i in 0 until LaneRenderer.LANE_COUNT) {
      val baseX = LaneRenderer.START_X +
              i * (LaneRenderer.LANE_WIDTH + LaneRenderer.LANE_SPACING) +
              (LaneRenderer.LANE_WIDTH - SPOT_SIZE) / 2
      
      val scaledSize = SPOT_SIZE * scales[i]
      val offset = (SPOT_SIZE - scaledSize) / 2
      val x = baseX + offset
      val y = SPOT_Y + offset
      
      // Change color based on scale (more orange when pressed)
      val t = (1.0f - scales[i]) / (1.0f - PRESSED_SCALE)
      shapeRenderer.color = Color.YELLOW.cpy().lerp(Color.ORANGE, t)
      
      shapeRenderer.rect(x, y, scaledSize, scaledSize)
    }
  }
}
