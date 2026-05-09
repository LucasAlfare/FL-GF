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
  private lateinit var noteRenderer: NoteRenderer
  private lateinit var inputHandler: InputHandler

  override fun create() {
    batch = com.badlogic.gdx.graphics.g2d.SpriteBatch()
    shapeRenderer = ShapeRenderer()
    laneRenderer = LaneRenderer(shapeRenderer)
    hitSpotRenderer = HitSpotRenderer(shapeRenderer)
    noteRenderer = NoteRenderer(shapeRenderer)
    inputHandler = InputHandler()
  }

  override fun render() {
    Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    // Update input and visual feedback
    val playerInput = inputHandler.updateInput()
    hitSpotRenderer.updatePressedFrets(playerInput.pressedFrets)
    
    // Update note positions
    noteRenderer.updateNotes(System.currentTimeMillis())

    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

    laneRenderer.render()
    noteRenderer.render()
    hitSpotRenderer.render()

    shapeRenderer.end()
  }

  override fun dispose() {
    shapeRenderer.dispose()
    batch.dispose()
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
      val baseX =
              LaneRenderer.START_X +
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

/**
 * Responsible for rendering notes traveling down the lanes.
 * Notes start above the screen and travel down to the hit spots.
 */
class NoteRenderer(private val shapeRenderer: ShapeRenderer) : Renderer {
  companion object {
    const val NOTE_SIZE = 50f
    const val NOTE_SPEED = 200f // pixels per second
    const val SPAWN_Y = LaneRenderer.START_Y + LaneRenderer.LANE_HEIGHT + 100f
    const val DESPAWN_Y = LaneRenderer.START_Y - 100f
  }

  /** Test note for demonstration */
  private val testNote = Note(time = 0, lane = 2, duration = 0, isSpecial = false)
  private var noteStartTime: Long = 0

  /**
   * Updates note positions based on current song time.
   * 
   * @param currentTime Current song time in milliseconds
   */
  fun updateNotes(currentTime: Long) {
    // Initialize start time on first update
    if (noteStartTime == 0L) {
      noteStartTime = currentTime
    }
  }

  override fun render() {
    // Calculate current Y position based on elapsed time
    val currentTime = System.currentTimeMillis()
    val elapsedMs = if (noteStartTime > 0) currentTime - noteStartTime else 0
    val elapsedSeconds = elapsedMs / 1000f
    
    // Calculate Y position (moving down)
    val currentY = SPAWN_Y - (elapsedSeconds * NOTE_SPEED)
    
    // Only render if note is visible
    if (currentY >= DESPAWN_Y && currentY <= SPAWN_Y + NOTE_SIZE) {
      val lane = testNote.lane
      val x = LaneRenderer.START_X +
              lane * (LaneRenderer.LANE_WIDTH + LaneRenderer.LANE_SPACING) +
              (LaneRenderer.LANE_WIDTH - NOTE_SIZE) / 2
      
      // Render note as a colored rectangle
      shapeRenderer.color = if (testNote.isSpecial) Color.PURPLE else Color.GREEN
      shapeRenderer.rect(x, currentY, NOTE_SIZE, NOTE_SIZE)
    }
  }
}
