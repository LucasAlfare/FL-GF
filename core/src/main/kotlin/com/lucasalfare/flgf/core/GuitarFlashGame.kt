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
  private val trackSkin = TrackSkin()
  private val noteSkin = NoteSkin()

  private lateinit var song: SongData

  private var startTime: Long = 0

  private val yTop = 740f
  private val yBottom = -35f

  private val travelTime = 3000f // 👈 controla o “feeling”

  override fun create() {
    batch = SpriteBatch()
    camera = OrthographicCamera()
    camera.setToOrtho(false, 800f, 600f)

    Assets.load()

    val xml = Gdx.files.internal("aux_song.xml").read()
    song = SongXmlParser.parse(xml)

    engine = GameEngine(song.notes, hitWindow = 120)

    startTime = TimeUtils.millis()
  }

  override fun render() {
    val currentTime = TimeUtils.millis() - startTime

    val input = PlayerInput(emptySet(), emptySet(), false)
    engine.tick(input, currentTime)

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
    val texture = trackSkin.getTrackTexture()
    batch.draw(texture, 0f, 0f, 800f, 600f)
  }

  private fun drawNotes(currentTime: Long) {

    song.notes.forEach { note ->

      val timeToHit = note.time - currentTime

      if (timeToHit < -200 || timeToHit > travelTime) return@forEach

      val progress = 1f - (timeToHit.toFloat() / travelTime)

      if (progress !in 0f..1.2f) return@forEach

      val eased = progress * progress

      val (xTop, xBottom) = xTopAndBottomCoordsByLane(note.lane)

      val x = xTop + (xBottom - xTop) * eased
      val y = yTop + (yBottom - yTop) * eased
      val scale = 0.2f + eased * 1.0f

      val texture = noteSkin.getNoteTexture(note.lane)

      val width = texture.width * scale
      val height = texture.height * scale

      batch.draw(texture, x - width / 2, y - height / 2, width, height)
    }
  }

  fun xTopAndBottomCoordsByLane(lane: Int) = when (lane) {
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
  private val textures = mutableMapOf<String, Texture>()
  fun load() {
    // the track
    loadTexture("track", "track.png")

    // notes
    loadTexture("note_green", "default_green.png")
    loadTexture("note_red", "default_red.png")
    loadTexture("note_yellow", "default_yellow.png")
    loadTexture("note_blue", "default_blue.png")
    loadTexture("note_orange", "default_orange.png")
  }

  private fun loadTexture(key: String, path: String) {
    textures[key] = Texture(Gdx.files.internal(path))
  }

  fun getTexture(key: String): Texture {
    return textures[key]
      ?: error("Texture not found: $key")
  }

  fun dispose() {
    textures.values.forEach { it.dispose() }
    textures.clear()
  }
}

class NoteSkin {
  fun getNoteTexture(lane: Int): Texture {
    return when (lane) {
      0 -> Assets.getTexture("note_green")
      1 -> Assets.getTexture("note_red")
      2 -> Assets.getTexture("note_yellow")
      3 -> Assets.getTexture("note_blue")
      4 -> Assets.getTexture("note_orange")
      else -> error("Invalid lane: $lane")
    }
  }
}

class TrackSkin {
  fun getTrackTexture(): Texture {
    return Assets.getTexture("track")
  }
}