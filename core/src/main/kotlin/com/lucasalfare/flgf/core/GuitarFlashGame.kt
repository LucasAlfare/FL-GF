package com.lucasalfare.flgf.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.TimeUtils

// draft for graphics. focus here is not elegancy or performance (yet) totally bad and trashy yet
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
    drawNotes(currentTime)
    batch.end()
  }

  private fun drawTrack() {
    batch.draw(Assets.track, 0f, 0f, 800f, 600f)
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

  /**
   * Decide qual textura usar baseado em:
   * - Special note
   * - Special ativo
   */
  private fun resolveNoteTexture(note: Note): Texture {

    val color = laneToColor(note.lane)

    return when {
      engine.special.active -> Assets.buffedNote

      note.isSpecial -> Assets.starNotes[color]
        ?: error("Missing star note texture for $color")

      else -> Assets.defaultNotes[color]
        ?: error("Missing default note texture for $color")
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

  lateinit var track: Texture
    private set

  val defaultNotes = mutableMapOf<String, Texture>()
  val starNotes = mutableMapOf<String, Texture>()

  lateinit var buffedNote: Texture
    private set

  fun load() {

    track = load("track.png")

    // ===== DEFAULT NOTES =====
    loadDefault("green")
    loadDefault("red")
    loadDefault("yellow")
    loadDefault("blue")
    loadDefault("orange")

    // ===== STAR NOTES =====
    loadStar("green")
    loadStar("red")
    loadStar("yellow")
    loadStar("blue")
    loadStar("orange")

    // ===== BUFFED NOTE =====
    buffedNote = load("buffed_by_active_special_note.png")
  }

  private fun loadDefault(color: String) {
    defaultNotes[color] =
      load("default_notes/default_$color.png")
  }

  private fun loadStar(color: String) {
    starNotes[color] =
      load("star_notes/star_$color.png")
  }

  private fun load(path: String): Texture {
    return Texture(Gdx.files.internal(path))
  }

  fun dispose() {
    track.dispose()

    defaultNotes.values.forEach { it.dispose() }
    starNotes.values.forEach { it.dispose() }

    buffedNote.dispose()

    defaultNotes.clear()
    starNotes.clear()
  }
}