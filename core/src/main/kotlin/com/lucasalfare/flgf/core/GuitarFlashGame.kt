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

class GuitarFlashGame(
  private val difficulty: Difficulty = Difficulty.EXPERT
) : ApplicationAdapter() {

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

    viewport = FitViewport(1920f, 2400f, camera)
    viewport.apply()

    shapeRenderer = ShapeRenderer()

    createLayout()

    laneRenderer = LaneRenderer(shapeRenderer, layout)
    hitSpotRenderer = HitSpotRenderer(shapeRenderer, layout)
    noteRenderer = NoteRenderer(shapeRenderer, layout)

    inputHandler = InputHandler()

    engine = GameEngine(
      notes = generateFakeChart(),
      hitWindow = difficulty.hitWindow  // <- vem da dificuldade
    )

    startTime = TimeUtils.millis()
  }

  private fun createLayout() {
    layout = GameLayout(
      screenWidth = viewport.worldWidth,
      screenHeight = viewport.worldHeight,
      scrollSpeed = difficulty.scrollSpeed  // <- vem da dificuldade
    )
  }

  override fun render() {
    Gdx.gl.glClearColor(0.06f, 0.06f, 0.08f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    camera.update()
    shapeRenderer.projectionMatrix = camera.combined

    val songTime = TimeUtils.timeSinceMillis(startTime)
    val dt = Gdx.graphics.deltaTime

    val input = inputHandler.update()

    val hitsBefore = engine.notesStates
      .filter { it.hit }
      .map { it.note }
      .toSet()

    engine.tick(input, songTime)

    engine.notesStates
      .filter { it.hit && it.note !in hitsBefore }
      .forEach { hitSpotRenderer.flash(it.note.lane) }

    hitSpotRenderer.update(dt)

    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    laneRenderer.render()
    noteRenderer.render(engine.notesStates, songTime)
    hitSpotRenderer.render()
    shapeRenderer.end()
  }

  override fun resize(width: Int, height: Int) {
    viewport.update(width, height, true)

    createLayout()

    laneRenderer = LaneRenderer(shapeRenderer, layout)
    hitSpotRenderer = HitSpotRenderer(shapeRenderer, layout)
    noteRenderer = NoteRenderer(shapeRenderer, layout)
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
        duration = if (MathUtils.randomBoolean(0.15f)) 1000L else 0L,
        isSpecial = MathUtils.randomBoolean(0.1f)
      )
      time += 350L
    }

    return notes
  }
}

enum class Difficulty(
  val scrollSpeed: Float,
  val hitWindow: Long
) {
  EASY(scrollSpeed = 0.4f, hitWindow = 140),
  MEDIUM(scrollSpeed = 0.8f, hitWindow = 140),
  HARD(scrollSpeed = 1.3f, hitWindow = 100),
  EXPERT(scrollSpeed = 2f, hitWindow = 80)
}


class GameLayout(
  screenWidth: Float,
  screenHeight: Float,
  val scrollSpeed: Float  // <- vem de fora agora
) {
  val highwayWidth = screenWidth * 0.35f
  val highwayHeight = screenHeight * 0.9f
  val highwayX = (screenWidth - highwayWidth) / 2f
  val highwayY = 80f

  val laneCount = 5
  val laneSpacing = highwayWidth * 0.02f
  val laneWidth = (highwayWidth - laneSpacing * (laneCount - 1)) / laneCount

  val hitLineY = highwayY + 140f
  val noteSize = laneWidth * 0.6f

  fun laneX(index: Int): Float =
    highwayX + index * (laneWidth + laneSpacing)

  fun laneCenterX(index: Int): Float =
    laneX(index) + laneWidth / 2f
}

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

  private val flashTimers = FloatArray(layout.laneCount) { 0f }
  private val flashDuration = 0.15f

  fun flash(lane: Int) {
    if (lane in 0 until layout.laneCount) {
      flashTimers[lane] = flashDuration
    }
  }

  fun update(dt: Float) {
    for (i in flashTimers.indices) {
      if (flashTimers[i] > 0f) flashTimers[i] -= dt
    }
  }

  override fun render() {
    for (i in 0 until layout.laneCount) {
      val flashing = flashTimers[i] > 0f
      val progress = (flashTimers[i] / flashDuration).coerceIn(0f, 1f)

      val baseColor = laneColor(i)

      shapeRenderer.color = if (flashing) {
        // Interpola entre branco e a cor da lane
        Color(
          lerp(baseColor.r, 1f, progress),
          lerp(baseColor.g, 1f, progress),
          lerp(baseColor.b, 1f, progress),
          1f
        )
      } else {
        baseColor
      }

      val radius = layout.noteSize * if (flashing) {
        lerp(0.55f, 0.75f, progress) // Cresce levemente no flash
      } else {
        0.55f
      }

      shapeRenderer.circle(
        layout.laneCenterX(i),
        layout.hitLineY,
        radius
      )
    }
  }

  private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}

class NoteRenderer(
  private val shapeRenderer: ShapeRenderer,
  private val layout: GameLayout
) {

  fun render(notes: List<NoteState>, songTime: Long) {
    notes.forEach { state ->
      // Notas acertadas (sem sustain) somem imediatamente
      if (state.hit && !state.holding) return@forEach

      val note = state.note
      val distanceMs = note.hitTime - songTime
      val y = layout.hitLineY + distanceMs * layout.scrollSpeed

      if (y < layout.highwayY - 300f || y > layout.highwayY + layout.highwayHeight + 300f) {
        return@forEach
      }

      val x = layout.laneCenterX(note.lane) - layout.noteSize / 2f

      // Sustain body
      if (note.duration > 0) {
        val totalHeight = note.duration * layout.scrollSpeed
        // Encurta o corpo conforme o progresso do sustain
        val remainingRatio = 1.0 - (state.sustainProgress / note.duration)
        val sustainHeight = (totalHeight * remainingRatio).toFloat().coerceAtLeast(0f)

        shapeRenderer.color = laneColor(note.lane).cpy().mul(0.6f)
        shapeRenderer.rect(
          x + layout.noteSize * 0.3f,
          y,
          layout.noteSize * 0.4f,
          sustainHeight
        )
      }

      // Cabeça da nota — some se já foi acertada (só sustains continuam visíveis)
      if (!state.hit) {
        shapeRenderer.color = when {
          state.missed -> Color.DARK_GRAY
          note.isSpecial -> Color.CYAN
          else -> laneColor(note.lane)
        }
        shapeRenderer.rect(x, y, layout.noteSize, layout.noteSize)
      }
    }
  }
}

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

private fun laneColor(index: Int): Color {

  return when (index) {

    0 -> Color.GREEN

    1 -> Color.RED

    2 -> Color.YELLOW

    3 -> Color.BLUE

    else -> Color.ORANGE
  }
}