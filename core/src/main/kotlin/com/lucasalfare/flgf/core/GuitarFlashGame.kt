package com.lucasalfare.flgf.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.TimeUtils
import kotlin.math.pow

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
    batch.draw(Assets.getTexture("track"), 0f, 0f, 800f, 600f)
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

      val texture = Assets.getTexture(noteTextureKey(note.lane))

      val width = texture.width * scale
      val height = texture.height * scale

      batch.draw(texture, x - width / 2, y - height / 2, width, height)
    }
  }

  // absolute coordinates on screen?
  private fun laneCoords(lane: Int) = when (lane) {
    0 -> 412f to 75f
    1 -> 408f to 235f
    2 -> 400f to 405f
    3 -> 393f to 570f
    4 -> 387f to 735f
    else -> error("Invalid lane")
  }

  private fun noteTextureKey(lane: Int) = when (lane) {
    0 -> "note_green"
    1 -> "note_red"
    2 -> "note_yellow"
    3 -> "note_blue"
    4 -> "note_orange"
    else -> error("Invalid lane: $lane")
  }

  override fun dispose() {
    batch.dispose()
    Assets.dispose()
  }
}

object Assets {
  private val textures = mutableMapOf<String, Texture>()

  fun load() {
    // TODO: in the future load special notes PNGs and buffed notes PNGs also
    loadTexture("track", "track.png")

    loadTexture("note_green", "default_green.png")
    loadTexture("note_red", "default_red.png")
    loadTexture("note_yellow", "default_yellow.png")
    loadTexture("note_blue", "default_blue.png")
    loadTexture("note_orange", "default_orange.png")
  }

  private fun loadTexture(key: String, path: String) {
    textures[key] = Texture(Gdx.files.internal(path))
  }

  fun getTexture(key: String): Texture =
    textures[key] ?: error("Texture not found: $key")

  fun dispose() {
    textures.values.forEach(Texture::dispose)
    textures.clear()
  }
}