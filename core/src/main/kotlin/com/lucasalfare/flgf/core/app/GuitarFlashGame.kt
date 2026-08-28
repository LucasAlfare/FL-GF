package com.lucasalfare.flgf.core.app

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.TimeUtils
import com.badlogic.gdx.utils.viewport.FitViewport
import com.lucasalfare.flgf.core.game.GameEngine
import com.lucasalfare.flgf.core.game.HitWindow
import com.lucasalfare.flgf.core.game.Note
import com.lucasalfare.flgf.core.input.InputHandler
import com.lucasalfare.flgf.core.visual.RhythmSceneRenderer

class GuitarFlashGame : ApplicationAdapter() {

  private val spawnAheadTimeMs = 2_600L

  private lateinit var camera: OrthographicCamera
  private lateinit var viewport: FitViewport
  private lateinit var renderer: RhythmSceneRenderer
  private lateinit var engine: GameEngine

  private var startTime: Long = 0L

  override fun create() {
    camera = OrthographicCamera()
    viewport = FitViewport(1000f, 2000f, camera)
    viewport.apply(true)

    renderer = RhythmSceneRenderer(spawnAheadTimeMs = spawnAheadTimeMs)
    renderer.resize(viewport.worldWidth.toInt(), viewport.worldHeight.toInt())

    engine = GameEngine(
      notes = buildDemoChart(),
      hitWindow = HitWindow(early = 100L, late = 200L),
      spawnAheadTime = spawnAheadTimeMs
    )

    engine.special.energy = 75
    InputHandler.reset()
    startTime = TimeUtils.millis()
  }

  override fun render() {
    Gdx.gl.glClearColor(0.06f, 0.06f, 0.08f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    camera.update()
    renderer.setProjectionMatrix(camera.combined)

    val currentTime = TimeUtils.timeSinceMillis(startTime)
    val nextInput = InputHandler.update()
    engine.tick(input = nextInput, currentTime = currentTime)

    Gdx.graphics.setTitle(
      "Meu Jogo | FPS: ${Gdx.graphics.framesPerSecond} | " +
        "Score: ${engine.score.score} | Combo: ${engine.score.combo} | Special: ${engine.special.energy}"
    )

    renderer.render(
      engine = engine,
      input = nextInput,
      currentTimeMs = currentTime
    )
  }

  override fun resize(width: Int, height: Int) {
    viewport.update(width, height, true)
    renderer.resize(viewport.worldWidth.toInt(), viewport.worldHeight.toInt())
  }

  override fun dispose() {
    renderer.dispose()
  }

  private fun buildDemoChart(): List<Note> {
    return listOf(
      Note(hitTime = 1_400L, lane = 0),
      Note(hitTime = 1_800L, lane = 1),
      Note(hitTime = 2_200L, lane = 2, duration = 900L),
      Note(hitTime = 2_600L, lane = 3, isSpecial = true),
      Note(hitTime = 3_000L, lane = 4, isSpecial = true),
      Note(hitTime = 3_400L, lane = 0, isSpecial = true),
      Note(hitTime = 3_900L, lane = 1),
      Note(hitTime = 4_300L, lane = 2, duration = 1_150L),
      Note(hitTime = 4_800L, lane = 3),
      Note(hitTime = 5_200L, lane = 4),
      Note(hitTime = 5_800L, lane = 0, isSpecial = true),
      Note(hitTime = 6_200L, lane = 1, isSpecial = true),
      Note(hitTime = 6_600L, lane = 2),
      Note(hitTime = 7_000L, lane = 3, duration = 1_300L),
      Note(hitTime = 7_600L, lane = 4),
      Note(hitTime = 8_000L, lane = 0),
      Note(hitTime = 8_400L, lane = 2),
      Note(hitTime = 8_400L, lane = 4),
      Note(hitTime = 8_900L, lane = 1, duration = 1_000L),
      Note(hitTime = 9_500L, lane = 3, isSpecial = true),
      Note(hitTime = 9_900L, lane = 4, isSpecial = true),
      Note(hitTime = 10_300L, lane = 0, isSpecial = true)
    )
  }
}
