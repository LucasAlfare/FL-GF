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

/**
 * Tudo em unidades de mundo. O mundo vai de (0,0) até (1,1)? Tentei deixar o viewport em 1:2.
 *
 * @property noteSpeedPerMs  Velocidade das notas — fração do mundo por milissegundo.
 * @property trackHeight     Altura visível da pista em unidades de mundo.
 * @property hitLineY        Posição Y da linha de acerto em unidades de mundo.
 * @property hitWindow       Janela de acerto em milissegundos.
 */
data class GameConfig(
  val noteSpeedPerMs: Float = 0.001975f, // velocidade expert?
  val trackHeight: Float = 1.8f, // altura do tal "mundo"?
  val hitLineY: Float = 0.15f,
  val hitWindow: Long = 100L
) {
  /**
   * Quantos ms uma nota leva pra percorrer toda a pista.
   * Derivado de velocidade e espaço — a engine não precisa saber de nada disso.
   */
  val spawnAheadTime: Long
    get() = (trackHeight / noteSpeedPerMs).toLong()
}

private object LanePalette {
  private val colors = listOf(
    Color(0.2f, 0.8f, 0.2f, 1f), // green
    Color(0.8f, 0.2f, 0.2f, 1f), // red
    Color(0.9f, 0.8f, 0.1f, 1f), // yellow
    Color(0.2f, 0.2f, 0.9f, 1f), // blue
    Color(0.95f, 0.5f, 0.15f, 1f), // orange
  )

  fun colorForLane(lane: Int): Color = colors.getOrElse(lane) { Color(0.7f, 0.7f, 0.7f, 1f) }
}

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

/**
 * Desenha os spots fixos na hit line — os alvos onde a nota deve ser pressionada.
 */
class HitSpotRenderer(
  private val config: GameConfig,
  private val laneLayout: LaneLayout
) {
  private val spotHeight: Float
    get() = 0.15f

  fun draw(shapeRenderer: ShapeRenderer) {
    for (lane in 0 until laneLayout.laneCount) {
      shapeRenderer.color = LanePalette.colorForLane(lane)
      shapeRenderer.rect(
        laneLayout.xForLane(lane),
        config.hitLineY - spotHeight / 2f,
        laneLayout.laneWidth - laneLayout.laneGap,
        spotHeight
      )
    }
  }
}

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
  private val sustainBodyWidthRatio = 0.32f

  private val colorInactive = Color(0.55f, 0.55f, 0.58f, 1f)
  private val colorBrokenBody = Color(0.55f, 0.55f, 0.58f, 0.25f)

  fun draw(shapeRenderer: ShapeRenderer, noteStates: List<NoteState>, songTime: Long) {
    noteStates.forEach { state ->
      val headY = yForTime(state.note.hitTime, songTime)
      val bodyTopY = yForTime(state.note.hitTime + state.note.duration, songTime)
      val visibleBottom = config.hitLineY - noteHeight
      val visibleTop = config.hitLineY + config.trackHeight
      val noteTopY = maxOf(headY + noteHeight, bodyTopY)

      if (noteTopY < visibleBottom || headY > visibleTop) {
        return@forEach
      }

      // Draw the sustain body first so the note head sits on top of it.
      drawSustainBody(shapeRenderer, state, headY, bodyTopY)
      drawHead(shapeRenderer, state, headY)
    }
  }

  private fun yForTime(noteTime: Long, songTime: Long): Float {
    val distanceMs = noteTime - songTime
    return config.hitLineY + distanceMs * config.noteSpeedPerMs
  }

  private fun drawHead(shapeRenderer: ShapeRenderer, state: NoteState, headY: Float) {
    if (state.hit) return

    shapeRenderer.color = if (state.missed || state.sustainBroken) {
      colorInactive
    } else {
      LanePalette.colorForLane(state.note.lane)
    }

    shapeRenderer.rect(
      laneLayout.xForLane(state.note.lane),
      headY,
      laneLayout.laneWidth - laneLayout.laneGap,
      noteHeight
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

    val rawBodyStartY = headY + noteHeight
    val bodyStartY = if (state.hit && !state.sustainBroken && state.holding) {
      maxOf(rawBodyStartY, config.hitLineY)
    } else {
      rawBodyStartY
    }
    val bodyHeight = bodyTopY - bodyStartY
    if (bodyHeight <= 0f) return

    val laneX = laneLayout.xForLane(state.note.lane)
    val laneWidth = laneLayout.laneWidth - laneLayout.laneGap
    val sustainWidth = laneWidth * sustainBodyWidthRatio
    val sustainX = laneX + (laneWidth - sustainWidth) / 2f

    shapeRenderer.color = when {
      state.hit && !state.sustainBroken -> LanePalette.colorForLane(state.note.lane).cpy().apply { a = 0.55f }
      state.sustainBroken || state.missed -> colorBrokenBody
      else -> LanePalette.colorForLane(state.note.lane).cpy().apply { a = 0.5f }
    }

    shapeRenderer.rect(sustainX, bodyStartY, sustainWidth, bodyHeight)
  }
}

// isso aqui está "overegeneering"? se estiver, quero deixar trivial ou mais performático, como um array de booleans pra
// marcar os inputs que foram pressed etc. adapte a lógica se necessário.
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
      Note(hitTime = 1000L, lane = 0, duration = 2000),
      //Note(hitTime = 1000L, lane = 4),
//      Note(hitTime = 2000L, lane = 2), Note(hitTime = 2000L, lane = 3),
//      Note(hitTime = 3000L, lane = 2), Note(hitTime = 3000L, lane = 3),
    )
    var lastTime = 6000
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
