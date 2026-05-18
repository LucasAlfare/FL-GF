package com.lucasalfare.flgf.core.app

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.TimeUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.lucasalfare.flgf.core.game.GameEngine
import com.lucasalfare.flgf.core.game.Note
import com.lucasalfare.flgf.core.input.InputHandler
import com.lucasalfare.flgf.core.view.GameConfig
import com.lucasalfare.flgf.core.view.PlayfieldLayout
import com.lucasalfare.flgf.core.view.PlayfieldPerspective
import com.lucasalfare.flgf.core.view.PlayfieldPerspectiveConfig
import com.lucasalfare.flgf.core.view.render.HitSpotRenderer
import com.lucasalfare.flgf.core.view.render.NoteRenderer
import com.lucasalfare.flgf.core.view.render.TrackRenderer

class GuitarFlashGame : ApplicationAdapter() {

  private lateinit var camera: OrthographicCamera
  private lateinit var viewport: FitViewport
  private lateinit var shapeRenderer: ShapeRenderer
  private var startTime: Long = 0L

  private lateinit var engine: GameEngine

  private val config = GameConfig(
    perspective = PlayfieldPerspectiveConfig(
      enabled = false,
      rotationXDegrees = 70f,
      rotationYDegrees = 0f,
      rotationZDegrees = 0f
    )
  )
  private val layout = PlayfieldLayout(config)
  private val perspective =
    PlayfieldPerspective(layout, config.perspective)

  private lateinit var trackRenderer: TrackRenderer
  private lateinit var hitSpotRenderer: HitSpotRenderer
  private lateinit var noteRenderer: NoteRenderer

  override fun create() {
    camera = OrthographicCamera()
    viewport = FitViewport(1f, 2f, camera)
    viewport.apply(true)

    shapeRenderer = ShapeRenderer()

    trackRenderer = TrackRenderer(layout, perspective)
    hitSpotRenderer = HitSpotRenderer(layout, perspective)
    noteRenderer = NoteRenderer(layout, perspective)

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

private fun buildDebugChart(laneCount: Int): List<Note> {
  val DEBUG_START_DELAY_MS = 1000L
  val DEBUG_NORMAL_NOTE_SPACING_MS = 220L
  val DEBUG_SPECIAL_NOTE_SPACING_MS = 150L
  val DEBUG_SPECIAL_SEQUENCE_SIZE = 10
  val DEBUG_NORMAL_RUN_SIZE = 4
  val DEBUG_SPECIAL_TO_BANK_GAP_MS = 420L
  val DEBUG_BANK_TO_NEXT_SECTION_GAP_MS = 520L
  val DEBUG_LOOP_COUNT = 4

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
