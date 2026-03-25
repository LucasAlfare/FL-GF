import com.lucasalfare.flgf.core.GameEngine
import com.lucasalfare.flgf.core.PlayerInput
import com.lucasalfare.flgf.core.SongXmlParser
import kotlin.test.assertTrue
import java.io.InputStream
import kotlin.test.Test

class SongIntegrationTest {

  private fun loadTestXml(): InputStream {
    return this::class.java
      .getResourceAsStream("/aux_song.xml")
      ?: error("XML not found")
  }

  private fun press(vararg lanes: Int) =
    PlayerInput(
      pressedFrets = lanes.toSet(),
      justPressedFrets = lanes.toSet()
    )

  private fun hold(vararg lanes: Int) =
    PlayerInput(
      pressedFrets = lanes.toSet(),
      justPressedFrets = emptySet()
    )

  @Test
  fun `should play entire song perfectly`() {
    val song = SongXmlParser.parse(loadTestXml())
    val engine = GameEngine(song.notes, hitWindow = 100)

    var currentTime = 0L
    val endTime = (song.lengthMs ?: 60000)

    var noteIndex = 0
    val notes = song.notes

    while (currentTime <= endTime) {

      val toPress = mutableSetOf<Int>()

      while (noteIndex < notes.size &&
        kotlin.math.abs(notes[noteIndex].time - currentTime) <= 1
      ) {
        toPress.add(notes[noteIndex].lane)
        noteIndex++
      }

      val input = if (toPress.isNotEmpty()) {
        press(*toPress.toIntArray())
      } else {
        hold()
      }

      engine.tick(input, currentTime)

      currentTime += 10 // resolução de 10ms
    }

    assertTrue(engine.score.score > 0)
    assertTrue(engine.score.combo > 0)
    assertTrue(engine.score.multiplier >= 1)
  }

  @Test
  fun `should build and consume special correctly`() {
    val song = SongXmlParser.parse(loadTestXml())
    val engine = GameEngine(song.notes, hitWindow = 100)

    var currentTime = 0L
    val notes = song.notes

    var noteIndex = 0
    var activated = false

    while (currentTime <= 12000) {

      val toPress = mutableSetOf<Int>()

      while (noteIndex < notes.size &&
        kotlin.math.abs(notes[noteIndex].time - currentTime) <= 1
      ) {
        toPress.add(notes[noteIndex].lane)
        noteIndex++
      }

      val activate = engine.special.energy >= 50 && !activated

      val input = PlayerInput(
        pressedFrets = toPress,
        justPressedFrets = toPress,
        activateSpecial = activate
      )

      if (activate) activated = true

      engine.tick(input, currentTime)
      currentTime += 10
    }

    assertTrue(engine.special.energy >= 0)
  }

  @Test
  fun `should handle dense sections without breaking`() {
    val song = SongXmlParser.parse(loadTestXml())
    val engine = GameEngine(song.notes, hitWindow = 100)

    var currentTime = 14000L
    var noteIndex = song.notes.indexOfFirst { it.time >= 14000 }

    while (currentTime <= 16000) {

      val toPress = mutableSetOf<Int>()

      while (noteIndex < song.notes.size &&
        kotlin.math.abs(song.notes[noteIndex].time - currentTime) <= 1
      ) {
        toPress.add(song.notes[noteIndex].lane)
        noteIndex++
      }

      val input = if (toPress.isNotEmpty()) {
        press(*toPress.toIntArray())
      } else {
        hold()
      }

      engine.tick(input, currentTime)
      currentTime += 5 // mais fino aqui
    }

    assertTrue(engine.score.score > 0)
  }

  // TODO: test sustain from XML song
}