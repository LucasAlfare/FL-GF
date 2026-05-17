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
import kotlin.random.Random

// ==================== CONFIG ====================

/**
 * Tudo em unidades de mundo. O mundo vai de (0,0) até (1,1).
 *
 * @property noteSpeedPerMs  Velocidade das notas — fração do mundo por milissegundo.
 * @property trackHeight     Altura visível da pista em unidades de mundo.
 * @property hitLineY        Posição Y da linha de acerto em unidades de mundo.
 * @property hitWindow       Janela de acerto em milissegundos.
 */
data class GameConfig(
  val noteSpeedPerMs: Float = 0.001975f, // velocidade expert?
  val trackHeight: Float = 1.8f, // altura do tal mundo?
  val hitLineY: Float = 0.15f,
  val hitWindow: Long = 200L
) {
  /**
   * Quantos ms uma nota leva pra percorrer toda a pista.
   * Derivado de velocidade e espaço — a engine não precisa saber de nada disso.
   */
  val spawnAheadTime: Long
    get() = (trackHeight / noteSpeedPerMs).toLong()
}

// ==================== TRACK RENDERER ====================

/**
 * Desenha a pista: fundo e divisórias entre lanes.
 */
class TrackRenderer(
  private val config: GameConfig,
  private val laneLayout: LaneLayout
) {
  fun draw(shapeRenderer: ShapeRenderer) {
    shapeRenderer.color = Color(0.12f, 0.12f, 0.15f, 1f)
    shapeRenderer.rect(
      laneLayout.trackStartX,
      config.hitLineY,
      laneLayout.trackWidth,
      config.trackHeight
    )

    shapeRenderer.color = Color(0.3f, 0.3f, 0.35f, 1f)
    for (i in 1 until laneLayout.laneCount) {
      val x = laneLayout.trackStartX + i * laneLayout.laneWidth
      shapeRenderer.rect(x, config.hitLineY, 0.002f, config.trackHeight)
    }
  }
}

// ==================== HIT SPOT RENDERER ====================

/**
 * Desenha os spots fixos na hit line — os alvos onde a nota deve ser pressionada.
 * A altura do spot reflete honestamente a janela de acerto em ms.
 */
class HitSpotRenderer(
  private val config: GameConfig,
  private val laneLayout: LaneLayout
) {
  private val laneColors = listOf(
    Color(0.2f, 0.8f, 0.2f, 1f),
    Color(0.8f, 0.2f, 0.2f, 1f),
    Color(0.2f, 0.2f, 0.9f, 1f),
    Color(0.9f, 0.8f, 0.1f, 1f),
    Color(0.7f, 0.2f, 0.9f, 1f),
  )

  // Altura visual honesta: equivale exatamente à janela de acerto no mundo
  private val spotHeight: Float
    get() = 0.15f

  fun draw(shapeRenderer: ShapeRenderer) {
    for (lane in 0 until laneLayout.laneCount) {
      shapeRenderer.color = laneColors.getOrElse(lane) { Color.WHITE }
      shapeRenderer.rect(
        laneLayout.xForLane(lane),
        config.hitLineY - spotHeight / 2f,
        laneLayout.laneWidth - laneLayout.laneGap,
        spotHeight
      )
    }
  }
}

// ==================== NOTE RENDERER ====================

/**
 * Desenha as notas em movimento.
 * Recebe os dados da engine como parâmetros — não acessa a engine diretamente.
 */
class NoteRenderer(
  private val config: GameConfig,
  private val laneLayout: LaneLayout
) {
  private val noteHeight: Float
    get() = 0.1f

  private val colorPending = Color(0.2f, 1f, 0.4f, 1f)
  private val colorHit = Color(1f, 1f, 1f, 0.3f)
  private val colorMissed = Color(1f, 0.2f, 0.2f, 0.5f)

  fun draw(shapeRenderer: ShapeRenderer, noteStates: List<NoteState>, songTime: Long) {
    noteStates.forEach { state ->
      val distanceMs = state.note.hitTime - songTime
      val y = config.hitLineY + distanceMs * config.noteSpeedPerMs

      if (y < config.hitLineY - noteHeight || y > config.hitLineY + config.trackHeight) {
        return@forEach
      }

      shapeRenderer.color = when {
        state.missed -> colorMissed
        state.hit -> colorHit
        else -> colorPending
      }

      shapeRenderer.rect(
        laneLayout.xForLane(state.note.lane),
        y,
        laneLayout.laneWidth - laneLayout.laneGap,
        noteHeight
      )
    }
  }
}

// ==================== LANE LAYOUT ====================

/**
 * Centraliza toda a matemática de posicionamento das lanes.
 * Qualquer renderer que precise saber onde uma lane está usa isso.
 *
 * @param trackStartX  X onde a pista começa (unidades de mundo).
 * @param trackWidth   Largura total da pista (unidades de mundo).
 * @param laneCount    Número de lanes.
 * @param laneGap      Espaço entre lanes (unidades de mundo).
 */
data class LaneLayout(
  val trackStartX: Float = 0.1f,
  val trackWidth: Float = 0.8f,
  val laneCount: Int = 5,
  val laneGap: Float = 0.004f
) {
  val laneWidth: Float get() = trackWidth / laneCount

  fun xForLane(lane: Int): Float = trackStartX + lane * laneWidth
}

// ==================== GAME ====================

class GuitarFlashGame : ApplicationAdapter() {

  private lateinit var camera: OrthographicCamera
  private lateinit var viewport: FitViewport
  private lateinit var shapeRenderer: ShapeRenderer
  private var startTime: Long = 0L

  private lateinit var engine: GameEngine

  private val config = GameConfig()
  private val laneLayout = LaneLayout()

  private lateinit var trackRenderer: TrackRenderer
  private lateinit var hitSpotRenderer: HitSpotRenderer
  private lateinit var noteRenderer: NoteRenderer

  override fun create() {
    camera = OrthographicCamera()
    viewport = FitViewport(1f, 2f, camera)
    viewport.apply(true)

    shapeRenderer = ShapeRenderer()

    trackRenderer = TrackRenderer(config, laneLayout)
    hitSpotRenderer = HitSpotRenderer(config, laneLayout)
    noteRenderer = NoteRenderer(config, laneLayout)

    // notas fake pra testar, quero pegar do "resources/aux_song.xml" depois
    val notes = mutableListOf(
      Note(hitTime = 1000L, lane = 0), Note(hitTime = 1000L, lane = 4),
      Note(hitTime = 2000L, lane = 2), Note(hitTime = 2000L, lane = 3),
      Note(hitTime = 3000L, lane = 2), Note(hitTime = 3000L, lane = 3),
    )
    var lastTime = 10000
    repeat(100) {
      val nextTime = Random.nextInt(lastTime, lastTime + 1000)
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

// ==================== INPUT HANDLER ====================

object InputHandler {
  private val previousPressed = mutableSetOf<Int>()

  fun update(): PlayerInput {
    val pressed = mutableSetOf<Int>()

    if (Gdx.input.isKeyPressed(Input.Keys.E)) pressed += 0
    if (Gdx.input.isKeyPressed(Input.Keys.T)) pressed += 1
    if (Gdx.input.isKeyPressed(Input.Keys.U)) pressed += 2
    if (Gdx.input.isKeyPressed(Input.Keys.I)) pressed += 3
    if (Gdx.input.isKeyPressed(Input.Keys.O)) pressed += 4

    val justPressed = pressed.filter { it !in previousPressed }.toSet()
    previousPressed.clear()
    previousPressed += pressed

    return PlayerInput(
      pressedFrets = pressed,
      justPressedFrets = justPressed,
      activateSpecial = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
    )
  }
}