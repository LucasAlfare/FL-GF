package com.lucasalfare.flgf.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.TimeUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import kotlin.math.max
import kotlin.random.Random

/**
 * World-space config for the prototype UI.
 * The playfield is centered inside the viewport and all renderers read from the same geometry.
 */
data class GameConfig(
  val noteSpeedPerMs: Float = 0.001975f,
  val playfieldWidth: Float = 0.72f,
  val playfieldHeight: Float = 2f,
  val playfieldCenterX: Float = 0.5f,
  val playfieldCenterY: Float = 1.0f,
  val hitLineInsetFromBottom: Float = 0.14f,
  val hitWindow: Long = 100L
) {
  val trackTravelHeight: Float
    get() = max(0f, playfieldHeight - hitLineInsetFromBottom)

  /**
   * Time it takes a note to travel from the top of the visible lane area to the hit line.
   */
  val spawnAheadTime: Long
    get() = (trackTravelHeight / noteSpeedPerMs).toLong()
}

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

private object LanePalette {
  private val colors = listOf(
    Color(0.2f, 0.8f, 0.2f, 1f),
    Color(0.8f, 0.2f, 0.2f, 1f),
    Color(0.9f, 0.8f, 0.1f, 1f),
    Color(0.2f, 0.2f, 0.9f, 1f),
    Color(0.95f, 0.5f, 0.15f, 1f),
  )

  fun colorForLane(lane: Int): Color = colors.getOrElse(lane) { Color(0.7f, 0.7f, 0.7f, 1f) }
}

/**
 * Draws the track background and the lane separators.
 */
class TrackRenderer(
  private val layout: PlayfieldLayout
) {
  fun draw(shapeRenderer: ShapeRenderer) {
    shapeRenderer.color = Color(0.12f, 0.12f, 0.15f, 1f)
    shapeRenderer.rect(
      layout.left,
      layout.bottom,
      layout.width,
      layout.height
    )

    shapeRenderer.color = Color(0.3f, 0.3f, 0.35f, 1f)
    for (i in 1 until layout.laneCount) {
      val x = layout.left + i * layout.laneWidth
      shapeRenderer.rect(x, layout.bottom, 0.002f, layout.height)
    }
  }
}

/**
 * Draws the fixed hit spots on the hit line.
 */
class HitSpotRenderer(
  private val layout: PlayfieldLayout
) {
  fun draw(shapeRenderer: ShapeRenderer) {
    for (lane in 0 until layout.laneCount) {
      shapeRenderer.color = LanePalette.colorForLane(lane)
      shapeRenderer.rect(
        layout.xForLane(lane),
        layout.hitLineY - layout.spotHeight / 2f,
        layout.laneWidth - layout.laneGap,
        layout.spotHeight
      )
    }
  }
}

/**
 * Draws the moving notes.
 */
class NoteRenderer(
  private val layout: PlayfieldLayout
) {
  private val colorInactive = Color(0.55f, 0.55f, 0.58f, 1f)
  private val colorBrokenBody = Color(0.55f, 0.55f, 0.58f, 0.25f)

  fun draw(shapeRenderer: ShapeRenderer, noteStates: List<NoteState>, songTime: Long) {
    noteStates.forEach { state ->
      val headY = yForTime(state.note.hitTime, songTime)
      val bodyTopY = yForTime(state.note.hitTime + state.note.duration, songTime)
      val visibleBottom = layout.hitLineY - layout.noteHeight
      val visibleTop = layout.top
      val noteTopY = max(headY + layout.noteHeight, bodyTopY)

      if (noteTopY < visibleBottom || headY > visibleTop) {
        return@forEach
      }

      // Draw the sustain body first so the note head sits on top.
      drawSustainBody(shapeRenderer, state, headY, bodyTopY)
      drawHead(shapeRenderer, state, headY)
    }
  }

  private fun yForTime(noteTime: Long, songTime: Long): Float {
    val distanceMs = noteTime - songTime
    return layout.hitLineY + distanceMs * layout.noteSpeedPerMs
  }

  private fun drawHead(shapeRenderer: ShapeRenderer, state: NoteState, headY: Float) {
    if (state.hit) return

    shapeRenderer.color = if (state.missed || state.sustainBroken) {
      colorInactive
    } else {
      LanePalette.colorForLane(state.note.lane)
    }

    shapeRenderer.rect(
      layout.xForLane(state.note.lane),
      headY,
      layout.laneWidth - layout.laneGap,
      layout.noteHeight
    )
  }

  private fun drawSustainBody(
    shapeRenderer: ShapeRenderer,
    state: NoteState,
    headY: Float,
    bodyTopY: Float
  ) {
    if (state.note.duration <= 0L) return
    if (state.hit && !state.sustainBroken && state.sustainProgress >= state.note.duration) return

    val rawBodyStartY = headY + layout.noteHeight
    val bodyStartY = if (state.hit && !state.sustainBroken && state.holding) {
      max(rawBodyStartY, layout.hitLineY)
    } else {
      rawBodyStartY
    }
    val bodyHeight = bodyTopY - bodyStartY
    if (bodyHeight <= 0f) return

    val laneX = layout.xForLane(state.note.lane)
    val laneWidth = layout.laneWidth - layout.laneGap
    val sustainWidth = laneWidth * layout.sustainBodyWidthRatio
    val sustainX = laneX + (laneWidth - sustainWidth) / 2f

    shapeRenderer.color = when {
      state.hit && !state.sustainBroken -> LanePalette.colorForLane(state.note.lane).cpy().apply { a = 0.55f }
      state.sustainBroken || state.missed -> colorBrokenBody
      else -> LanePalette.colorForLane(state.note.lane).cpy().apply { a = 0.5f }
    }

    shapeRenderer.rect(sustainX, bodyStartY, sustainWidth, bodyHeight)
  }
}

// This input helper can stay simple for now.
object InputHandler {
  private val laneKeys = intArrayOf(
    Input.Keys.E,
    Input.Keys.T,
    Input.Keys.U,
    Input.Keys.I,
    Input.Keys.O
  )
  private val previousPressed = BooleanArray(laneKeys.size)

  fun update(): PlayerInput {
    val pressed = BooleanArray(laneKeys.size)
    val justPressed = BooleanArray(laneKeys.size)
    val justReleased = BooleanArray(laneKeys.size)

    for (lane in laneKeys.indices) {
      pressed[lane] = Gdx.input.isKeyPressed(laneKeys[lane])
      justPressed[lane] = pressed[lane] && !previousPressed[lane]
      justReleased[lane] = !pressed[lane] && previousPressed[lane]
      previousPressed[lane] = pressed[lane]
    }

    return PlayerInput(
      pressedFrets = pressed.indices.filter { pressed[it] }.toSet(),
      justPressedFrets = justPressed.indices.filter { justPressed[it] }.toSet(),
      justReleasedFrets = justReleased.indices.filter { justReleased[it] }.toSet(),
      activateSpecial = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
    )
  }
}

class GuitarFlashGame : ApplicationAdapter() {

  private lateinit var camera: OrthographicCamera
  private lateinit var viewport: FitViewport
  private lateinit var shapeRenderer: ShapeRenderer
  private var startTime: Long = 0L

  private lateinit var engine: GameEngine

  private val config = GameConfig()
  private val layout = PlayfieldLayout(config)

  private lateinit var trackRenderer: TrackRenderer
  private lateinit var hitSpotRenderer: HitSpotRenderer
  private lateinit var noteRenderer: NoteRenderer

  override fun create() {
    camera = OrthographicCamera()
    viewport = FitViewport(1f, 2f, camera)
    viewport.apply(true)

    shapeRenderer = ShapeRenderer()

    trackRenderer = TrackRenderer(layout)
    hitSpotRenderer = HitSpotRenderer(layout)
    noteRenderer = NoteRenderer(layout)

    // Fake notes for now; we'll switch to resources/aux_song.xml later.
    val notes = mutableListOf(Note(hitTime = 1000L, lane = 0, duration = 2000))
    var lastTime = 3000
    repeat(100) {
      var nextTime = Random.nextInt(lastTime, lastTime + 300)
      if (nextTime - lastTime < 100) nextTime += 100
      notes += Note(hitTime = nextTime.toLong(), lane = Random.nextInt(5))
      lastTime = nextTime
    }

    engine = GameEngine(
      hitWindow = config.hitWindow,
      spawnAheadTime = config.spawnAheadTime,
      notes = notes
    )

    startTime = TimeUtils.millis()
  }

  override fun render() {
    Gdx.gl.glClearColor(0.06f, 0.06f, 0.08f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    val songTime = TimeUtils.timeSinceMillis(startTime)
    engine.tick(input = InputHandler.update(), currentTime = songTime)

    camera.update()
    shapeRenderer.projectionMatrix = camera.combined
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

    trackRenderer.draw(shapeRenderer)
    hitSpotRenderer.draw(shapeRenderer)
    noteRenderer.draw(shapeRenderer, engine.notesStates, songTime)

    shapeRenderer.end()
  }

  override fun resize(width: Int, height: Int) {
    viewport.update(width, height, true)
  }

  override fun dispose() {
    shapeRenderer.dispose()
  }
}
