package com.lucasalfare.flgf.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.MathUtils.atan2
import com.badlogic.gdx.utils.TimeUtils

class GuitarFlashGame : ApplicationAdapter() {

  private lateinit var batch: SpriteBatch
  private lateinit var camera: OrthographicCamera
  private lateinit var engine: GameEngine
  private lateinit var song: SongData

  private var startTime = 0L

  private val yTop = 740f
  private val yBottom = -35f
  private val travelTime = 3000f

  override fun create() {
    batch = SpriteBatch()

    camera = OrthographicCamera().apply {
      setToOrtho(false, 800f, 600f)
    }

    Assets.load()

    song = SongXmlParser.parse(Gdx.files.internal("aux_song.xml").read())
    engine = GameEngine(song.notes, hitWindow = 120)

    startTime = TimeUtils.millis()
  }

  override fun render() {
    val currentTime = TimeUtils.millis() - startTime

    engine.tick(PlayerInput(emptySet(), emptySet(), false), currentTime)

    Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

    camera.update()
    batch.projectionMatrix = camera.combined

    batch.begin()
    drawTrack()
    drawSustains(currentTime) // 👈 DESENHA PRIMEIRO
    drawNotes(currentTime)    // 👈 NOTA POR CIMA
    batch.end()
  }

  private fun drawTrack() {
    batch.draw(Assets.track, 0f, 0f, 800f, 600f)
  }

  private fun drawSustains(currentTime: Long) {
    val segments = 24
    for (note in song.notes) {
      if (note.duration <= 0) continue
      val startTimeNote = note.time
      val endTimeNote = note.time + note.duration

      val timeToStart = startTimeNote - currentTime
      val timeToEnd = endTimeNote - currentTime

      if (timeToEnd < -200 || timeToStart > travelTime) continue

      val color = laneToColor(note.lane)
      val texture = Assets.sustainNotes[color] ?: continue

      // find exact "sustain coordinates", they are slightly different from the notes
      val (xTop, xBottom) = laneCoords(note.lane)

      var prevX = 0f
      var prevY = 0f
      var hasPrev = false

      for (i in 0..segments) {

        val t = i / segments.toFloat()

        val timeAt = startTimeNote + note.duration * t
        val timeToPoint = timeAt - currentTime

        val progress = 1f - (timeToPoint / travelTime)
        if (progress !in 0f..1.2f) continue

        val eased = progress * progress

        val x = xTop + (xBottom - xTop) * eased
        val y = yTop + (yBottom - yTop) * eased

        if (hasPrev) {
          val dx = x - prevX
          val dy = y - prevY

          val length = kotlin.math.sqrt(dx * dx + dy * dy)
          if (length > 0f) {

            val angle = kotlin.math.atan2(dy, dx) * MathUtils.radiansToDegrees

            val width = texture.width.toFloat()

            batch.draw(
              texture,
              prevX,
              prevY,
              width / 2,
              0f,
              width,
              length,
              1f,
              1f,
              angle,
              0,
              0,
              texture.width,
              texture.height,
              false,
              false
            )
          }
        }

        prevX = x
        prevY = y
        hasPrev = true
      }
    }
  }

  private fun drawNotes(currentTime: Long) {
    for (note in song.notes) {

      val timeToHit = note.time - currentTime
      if (timeToHit < -200 || timeToHit > travelTime) continue

      val progress = 1f - (timeToHit.toFloat() / travelTime)
      if (progress !in 0f..1.2f) continue

      val eased = progress * progress

      val (xTop, xBottom) = laneCoords(note.lane)

      val x = xTop + (xBottom - xTop) * eased
      val y = yTop + (yBottom - yTop) * eased

      val scale = 0.2f + eased

      val texture = resolveNoteTexture(note)

      val width = texture.width * scale
      val height = texture.height * scale

      batch.draw(texture, x - width / 2, y - height / 2, width, height)
    }
  }

  private fun resolveNoteTexture(note: Note): Texture {
    val color = laneToColor(note.lane)

    return when {
      engine.special.active -> Assets.buffedNote
      note.isSpecial -> Assets.starNotes[color]!!
      else -> Assets.defaultNotes[color]!!
    }
  }

  private fun laneToColor(lane: Int) = when (lane) {
    0 -> "green"
    1 -> "red"
    2 -> "yellow"
    3 -> "blue"
    4 -> "orange"
    else -> error("Invalid lane")
  }

  private fun laneCoords(lane: Int) = when (lane) {
    0 -> 412f to 75f
    1 -> 408f to 235f
    2 -> 400f to 405f
    3 -> 393f to 570f
    4 -> 387f to 735f
    else -> error("Invalid lane")
  }

  override fun dispose() {
    batch.dispose()
    Assets.dispose()
  }
}

object Assets {
  lateinit var track: Texture private set

  val defaultNotes = mutableMapOf<String, Texture>()
  val starNotes = mutableMapOf<String, Texture>()
  val sustainNotes = mutableMapOf<String, Texture>()

  lateinit var buffedNote: Texture
    private set

  fun load() {

    track = load("track.png")

    // DEFAULT
    loadDefault("green")
    loadDefault("red")
    loadDefault("yellow")
    loadDefault("blue")
    loadDefault("orange")

    // STAR
    loadStar("green")
    loadStar("red")
    loadStar("yellow")
    loadStar("blue")
    loadStar("orange")

    // SUSTAIN
    loadSustain("green")
    loadSustain("red")
    loadSustain("yellow")
    loadSustain("blue")
    loadSustain("orange")

    // BUFFED
    buffedNote = load("buffed_by_active_especial_note.png")
  }

  private fun loadDefault(color: String) {
    defaultNotes[color] =
      load("default_notes/default_$color.png")
  }

  private fun loadStar(color: String) {
    starNotes[color] =
      load("star_notes/star_$color.png")
  }

  private fun loadSustain(color: String) {
    sustainNotes[color] =
      load("sustains/${color}_sustain.png")
  }

  private fun load(path: String): Texture {
    return Texture(Gdx.files.internal(path))
  }

  fun dispose() {
    track.dispose()
    buffedNote.dispose()

    defaultNotes.values.forEach { it.dispose() }
    starNotes.values.forEach { it.dispose() }
    sustainNotes.values.forEach { it.dispose() }

    defaultNotes.clear()
    starNotes.clear()
    sustainNotes.clear()
  }
}