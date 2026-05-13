package com.lucasalfare.flgf.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.TimeUtils
import com.badlogic.gdx.utils.viewport.FitViewport

class GuitarFlashGame : ApplicationAdapter() {

  private lateinit var camera: OrthographicCamera
  private lateinit var viewport: FitViewport

  private lateinit var shapeRenderer: ShapeRenderer

  private lateinit var layout: GameLayout

  private lateinit var laneRenderer: LaneRenderer
  private lateinit var hitSpotRenderer: HitSpotRenderer
  private lateinit var noteRenderer: NoteRenderer

  private lateinit var engine: GameEngine
  private lateinit var inputHandler: InputHandler

  private var startTime = 0L

  override fun create() {

    camera = OrthographicCamera()

    viewport = FitViewport(
      1920f,
      2400f,
      camera
    )

    viewport.apply()

    shapeRenderer = ShapeRenderer()

    createLayout()

    laneRenderer = LaneRenderer(
      shapeRenderer,
      layout
    )

    hitSpotRenderer = HitSpotRenderer(
      shapeRenderer,
      layout
    )

    noteRenderer = NoteRenderer(
      shapeRenderer,
      layout
    )

    inputHandler = InputHandler()

    engine = GameEngine(
      notes = generateFakeChart(),
      hitWindow = 120
    )

    startTime = TimeUtils.millis()
  }

  private fun createLayout() {

    layout = GameLayout(
      viewport.worldWidth,
      viewport.worldHeight
    )
  }

  override fun render() {

    Gdx.gl.glClearColor(
      0.06f,
      0.06f,
      0.08f,
      1f
    )

    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    camera.update()

    shapeRenderer.projectionMatrix =
      camera.combined

    val songTime =
      TimeUtils.timeSinceMillis(startTime)

    val input =
      inputHandler.update()

    engine.tick(
      input,
      songTime
    )

    shapeRenderer.begin(
      ShapeRenderer.ShapeType.Filled
    )

    laneRenderer.render()

    noteRenderer.render(
      engine.notesStates,
      songTime
    )

    hitSpotRenderer.render()

    shapeRenderer.end()
  }

  override fun resize(
    width: Int,
    height: Int
  ) {

    viewport.update(
      width,
      height,
      true
    )

    createLayout()

    laneRenderer = LaneRenderer(
      shapeRenderer,
      layout
    )

    hitSpotRenderer = HitSpotRenderer(
      shapeRenderer,
      layout
    )

    noteRenderer = NoteRenderer(
      shapeRenderer,
      layout
    )
  }

  override fun dispose() {

    shapeRenderer.dispose()
  }

  private fun generateFakeChart(): List<Note> {

    val notes = mutableListOf<Note>()

    var time = 1000L

    repeat(200) {

      notes += Note(
        hitTime = time,
        lane = MathUtils.random(0, 4),
        duration =
          if (MathUtils.randomBoolean(0.15f))
            1000L
          else
            0L,
        isSpecial =
          MathUtils.randomBoolean(0.1f)
      )

      time += 350L
    }

    return notes
  }
}

/* ========================================================= */
/* ======================= LAYOUT ========================== */
/* ========================================================= */

class GameLayout(

  screenWidth: Float,
  screenHeight: Float
) {

  val highwayWidth =
    screenWidth * 0.35f

  val highwayHeight =
    screenHeight * 0.9f

  val highwayX =
    (screenWidth - highwayWidth) / 2f

  val highwayY = 80f

  val laneCount = 5

  val laneSpacing =
    highwayWidth * 0.02f

  val laneWidth =
    (
        highwayWidth -
            laneSpacing * (laneCount - 1)
        ) / laneCount

  val hitLineY =
    highwayY + 140f

  val noteSize =
    laneWidth * 0.6f

  val scrollSpeed = 0.8f

  fun laneX(index: Int): Float {

    return highwayX +
        index * (laneWidth + laneSpacing)
  }

  fun laneCenterX(index: Int): Float {

    return laneX(index) + laneWidth / 2f
  }
}

/* ========================================================= */
/* ====================== RENDERERS ======================== */
/* ========================================================= */

interface Renderer {

  fun render()
}

class LaneRenderer(

  private val shapeRenderer: ShapeRenderer,
  private val layout: GameLayout
) : Renderer {

  override fun render() {

    for (i in 0 until layout.laneCount) {

      shapeRenderer.color =
        if (i % 2 == 0)
          Color(
            0.15f,
            0.15f,
            0.17f,
            1f
          )
        else
          Color(
            0.19f,
            0.19f,
            0.21f,
            1f
          )

      shapeRenderer.rect(
        layout.laneX(i),
        layout.highwayY,
        layout.laneWidth,
        layout.highwayHeight
      )
    }
  }
}

class HitSpotRenderer(

  private val shapeRenderer: ShapeRenderer,
  private val layout: GameLayout
) : Renderer {

  override fun render() {

    for (i in 0 until layout.laneCount) {

      shapeRenderer.color =
        laneColor(i)

      shapeRenderer.circle(
        layout.laneCenterX(i),
        layout.hitLineY,
        layout.noteSize * 0.55f
      )
    }
  }
}

class NoteRenderer(

  private val shapeRenderer: ShapeRenderer,
  private val layout: GameLayout
) {

  fun render(
    notes: List<NoteState>,
    songTime: Long
  ) {

    notes.forEach { state ->

      val note = state.note

      val distanceMs =
        note.hitTime - songTime

      val y =
        layout.hitLineY +
            distanceMs * layout.scrollSpeed

      if (
        y < layout.highwayY - 300f ||
        y > layout.highwayY + layout.highwayHeight + 300f
      ) {
        return@forEach
      }

      val x =
        layout.laneCenterX(note.lane) -
            layout.noteSize / 2f

      // Sustain body

      if (note.duration > 0) {

        val sustainHeight =
          note.duration * layout.scrollSpeed

        shapeRenderer.color =
          laneColor(note.lane).cpy().mul(
            0.6f
          )

        shapeRenderer.rect(
          x + layout.noteSize * 0.3f,
          y,
          layout.noteSize * 0.4f,
          sustainHeight
        )
      }

      // Note head

      shapeRenderer.color =
        when {

          state.missed ->
            Color.DARK_GRAY

          note.isSpecial ->
            Color.CYAN

          else ->
            laneColor(note.lane)
        }

      shapeRenderer.rect(
        x,
        y,
        layout.noteSize,
        layout.noteSize
      )
    }
  }
}

/* ========================================================= */
/* ==================== INPUT HANDLER ====================== */
/* ========================================================= */

class InputHandler {

  private val previousPressed =
    mutableSetOf<Int>()

  fun update(): PlayerInput {

    val pressed = mutableSetOf<Int>()

    if (Gdx.input.isKeyPressed(Input.Keys.E))
      pressed += 0

    if (Gdx.input.isKeyPressed(Input.Keys.T))
      pressed += 1

    if (Gdx.input.isKeyPressed(Input.Keys.U))
      pressed += 2

    if (Gdx.input.isKeyPressed(Input.Keys.I))
      pressed += 3

    if (Gdx.input.isKeyPressed(Input.Keys.O))
      pressed += 4

    val justPressed =
      pressed.filter {
        it !in previousPressed
      }.toSet()

    previousPressed.clear()

    previousPressed += pressed

    return PlayerInput(
      pressedFrets = pressed,
      justPressedFrets = justPressed,
      activateSpecial =
        Gdx.input.isKeyJustPressed(
          Input.Keys.SPACE
        )
    )
  }
}

/* ========================================================= */
/* ======================= HELPERS ========================= */
/* ========================================================= */

private fun laneColor(index: Int): Color {

  return when (index) {

    0 -> Color.GREEN

    1 -> Color.RED

    2 -> Color.YELLOW

    3 -> Color.BLUE

    else -> Color.ORANGE
  }
}