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

private val specialActiveColor = Color(0.1f, 0.92f, 1f, 1f)
private val specialIdleColor = Color(1f, 0.84f, 0.2f, 1f)
private val brokenColor = Color(0.55f, 0.55f, 0.58f, 0.25f)

private const val DEBUG_START_DELAY_MS = 1000L
private const val DEBUG_NORMAL_NOTE_SPACING_MS = 220L
private const val DEBUG_SPECIAL_NOTE_SPACING_MS = 150L
private const val DEBUG_SPECIAL_SEQUENCE_SIZE = 10
private const val DEBUG_NORMAL_RUN_SIZE = 4
private const val DEBUG_SPECIAL_TO_BANK_GAP_MS = 420L
private const val DEBUG_BANK_TO_NEXT_SECTION_GAP_MS = 520L
private const val DEBUG_LOOP_COUNT = 4

private fun buildDebugChart(laneCount: Int): List<Note> {
  val notes = mutableListOf<Note>()
  var time = DEBUG_START_DELAY_MS

  fun addNormalRun(startTime: Long, laneOffset: Int, count: Int): Long {
    var currentTime = startTime
    repeat(count) { index ->
      notes += Note(
        hitTime = currentTime,
        lane = (laneOffset + index) % laneCount
      )
      currentTime += DEBUG_NORMAL_NOTE_SPACING_MS
    }
    return currentTime
  }

  fun addSpecialPhrase(startTime: Long, laneOffset: Int): Long {
    var currentTime = startTime
    repeat(DEBUG_SPECIAL_SEQUENCE_SIZE) { index ->
      notes += Note(
        hitTime = currentTime,
        lane = (laneOffset + index) % laneCount,
        isSpecial = true
      )
      currentTime += DEBUG_SPECIAL_NOTE_SPACING_MS
    }
    return currentTime
  }

  time = addNormalRun(time, 0, DEBUG_NORMAL_RUN_SIZE)

  repeat(DEBUG_LOOP_COUNT) { block ->
    time += DEBUG_NORMAL_NOTE_SPACING_MS
    time = addSpecialPhrase(time, block)
    time += DEBUG_SPECIAL_TO_BANK_GAP_MS

    notes += Note(
      hitTime = time,
      lane = (block + 2) % laneCount
    )
    time += DEBUG_BANK_TO_NEXT_SECTION_GAP_MS

    time = addNormalRun(time, block + 1, DEBUG_NORMAL_RUN_SIZE)
  }

  return notes.sortedBy { it.hitTime }
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

  fun draw(
    shapeRenderer: ShapeRenderer,
    noteStates: List<NoteState>,
    songTime: Long,
    specialActive: Boolean
  ) {
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
      drawSustainBody(shapeRenderer, state, headY, bodyTopY, specialActive)
      drawHead(shapeRenderer, state, headY, specialActive)
    }
  }

  private fun yForTime(noteTime: Long, songTime: Long): Float {
    val distanceMs = noteTime - songTime
    return layout.hitLineY + distanceMs * layout.noteSpeedPerMs
  }

  private fun drawHead(
    shapeRenderer: ShapeRenderer,
    state: NoteState,
    headY: Float,
    specialActive: Boolean
  ) {
    if (state.hit) return

    val laneX = layout.xForLane(state.note.lane)
    val laneWidth = layout.laneWidth - layout.laneGap

    shapeRenderer.color = when {
      state.missed || state.sustainBroken -> colorInactive
      specialActive -> specialActiveColor
      state.note.isSpecial -> specialIdleColor
      else -> LanePalette.colorForLane(state.note.lane)
    }

    if (state.note.isSpecial) {
      drawSpecialHead(shapeRenderer, laneX, headY, laneWidth)
      return
    }

    shapeRenderer.rect(
      laneX,
      headY,
      laneWidth,
      layout.noteHeight
    )
  }

  private fun drawSustainBody(
    shapeRenderer: ShapeRenderer,
    state: NoteState,
    headY: Float,
    bodyTopY: Float,
    specialActive: Boolean
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
      state.sustainBroken || state.missed -> brokenColor
      state.hit && !state.sustainBroken && specialActive -> specialActiveColor.cpy().apply { a = 0.58f }
      state.hit && !state.sustainBroken -> LanePalette.colorForLane(state.note.lane).cpy().apply { a = 0.55f }
      specialActive -> specialActiveColor.cpy().apply { a = 0.45f }
      state.note.isSpecial -> specialIdleColor.cpy().apply { a = 0.45f }
      else -> LanePalette.colorForLane(state.note.lane).cpy().apply { a = 0.5f }
    }

    shapeRenderer.rect(sustainX, bodyStartY, sustainWidth, bodyHeight)
  }

  private fun drawSpecialHead(
    shapeRenderer: ShapeRenderer,
    laneX: Float,
    headY: Float,
    laneWidth: Float
  ) {
    val topY = headY + layout.noteHeight
    val centerX = laneX + laneWidth / 2f

    shapeRenderer.triangle(
      laneX,
      topY,
      laneX + laneWidth,
      topY,
      centerX,
      headY
    )
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

    // Fake notes for now: alternating normal and special sections so you can farm energy from zero.
    val notes = buildDebugChart(layout.laneCount)

    engine = GameEngine(
      hitWindow = config.hitWindow,
      spawnAheadTime = config.spawnAheadTime,
      notes = notes
    )
    engine.special.energy = 0

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
    noteRenderer.draw(shapeRenderer, engine.notesStates, songTime, engine.special.active)

    shapeRenderer.end()
  }

  override fun resize(width: Int, height: Int) {
    viewport.update(width, height, true)
  }

  override fun dispose() {
    shapeRenderer.dispose()
  }
}
