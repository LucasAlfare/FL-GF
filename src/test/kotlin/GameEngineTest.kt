import com.lucasalfare.flgf.b_usecase.GameEngine
import com.lucasalfare.kgf.a_domain.entities.GameState
import com.lucasalfare.kgf.a_domain.entities.HitWindow
import com.lucasalfare.kgf.a_domain.entities.InputEvent
import com.lucasalfare.kgf.a_domain.entities.InputType
import com.lucasalfare.kgf.a_domain.entities.Note
import com.lucasalfare.kgf.a_domain.entities.Song
import kotlin.test.Test
import kotlin.test.assertEquals

class GameEngineTest {

  private val hitWindow = HitWindow(
    perfect = 0.05,
    good = 0.1
  )

  private val engine = GameEngine(
    hitWindow = hitWindow,
    spawnWindow = 2.0
  )

  @Test
  fun `should spawn note and register perfect hit`() {
    // Arrange
    val note = Note(
      time = 1.0,
      lane = 1,
      duration = 0.0,
      special = false,
      hopo = false
    )

    val song = Song(
      title = "Test",
      artist = "Test",
      duration = 10.0,
      notes = listOf(note)
    )

    var state = GameState(song = song)

    // Act 1: advance time to spawn note
    state = engine.update(
      state = state,
      currentTime = 0.0,
      inputs = emptyList()
    )

    // Act 2: hit the note at perfect timing
    state = engine.update(
      state = state,
      currentTime = 1.0,
      inputs = listOf(
        InputEvent(
          time = 1.0,
          lane = 1,
          type = InputType.PRESS
        )
      )
    )

    // Assert
    assertEquals(100, state.score)
    assertEquals(1, state.combo)
    assertEquals(1, state.maxCombo)
    assertEquals(0, state.activeNotes.size)
  }

  @Test
  fun `should mark note as missed when no input`() {
    // Arrange
    val note = Note(
      time = 1.0,
      lane = 1,
      duration = 0.0,
      special = false,
      hopo = false
    )

    val song = Song(
      title = "Test",
      artist = "Test",
      duration = 10.0,
      notes = listOf(note)
    )

    var state = GameState(song = song)

    // Act: advance time beyond hit window
    state = engine.update(
      state = state,
      currentTime = 2.0,
      inputs = emptyList()
    )

    // Assert
    assertEquals(0, state.score)
    assertEquals(0, state.combo)
    assertEquals(0, state.activeNotes.size)
  }

  @Test
  fun `should increase combo on consecutive hits`() {
    // Arrange
    val notes = listOf(
      Note(1.0, 1, 0.0, false, false),
      Note(2.0, 1, 0.0, false, false)
    )

    val song = Song(
      title = "Test",
      artist = "Test",
      duration = 10.0,
      notes = notes
    )

    var state = GameState(song = song)

    // First note
    state = engine.update(
      state, 1.0, listOf(
        InputEvent(1.0, 1, InputType.PRESS)
      )
    )

    // Second note
    state = engine.update(
      state, 2.0, listOf(
        InputEvent(2.0, 1, InputType.PRESS)
      )
    )

    // Assert
    assertEquals(2, state.combo)
    assertEquals(2, state.maxCombo)
    assertEquals(200, state.score)
  }

  @Test
  fun `should register GOOD hit when outside perfect window but inside good window`() {
    val note = Note(1.0, 1, 0.0, false, false)

    val song = Song("Test", "Test", 10.0, listOf(note))
    var state = GameState(song = song)

    state = engine.update(state, 1.08, listOf(
      InputEvent(1.08, 1, InputType.PRESS)
    ))

    assertEquals(70, state.score)
    assertEquals(1, state.combo)
  }

  @Test
  fun `should ignore input too early`() {
    val note = Note(1.0, 1, 0.0, false, false)

    val song = Song("Test", "Test", 10.0, listOf(note))
    var state = GameState(song = song)

    state = engine.update(state, 0.5, listOf(
      InputEvent(0.5, 1, InputType.PRESS)
    ))

    assertEquals(0, state.score)
    assertEquals(0, state.combo)
  }

  @Test
  fun `should penalize when pressing wrong lane`() {
    val note = Note(1.0, 1, 0.0, false, false)

    val song = Song("Test", "Test", 10.0, listOf(note))
    var state = GameState(song = song, combo = 3)

    state = engine.update(state, 1.0, listOf(
      InputEvent(1.0, 2, InputType.PRESS) // wrong lane
    ))

    assertEquals(0, state.combo) // combo reset
  }

  @Test
  fun `should hit multiple notes at the same time (chord)`() {
    val notes = listOf(
      Note(1.0, 1, 0.0, false, false),
      Note(1.0, 2, 0.0, false, false)
    )

    val song = Song("Test", "Test", 10.0, notes)
    var state = GameState(song = song)

    state = engine.update(state, 1.0, listOf(
      InputEvent(1.0, 1, InputType.PRESS),
      InputEvent(1.0, 2, InputType.PRESS)
    ))

    assertEquals(200, state.score)
    assertEquals(2, state.combo)
  }

  @Test
  fun `should hit closest note when multiple are nearby`() {
    val notes = listOf(
      Note(1.0, 1, 0.0, false, false),
      Note(1.2, 1, 0.0, false, false)
    )

    val song = Song("Test", "Test", 10.0, notes)
    var state = GameState(song = song)

    state = engine.update(state, 1.18, listOf(
      InputEvent(1.18, 1, InputType.PRESS)
    ))

    assertEquals(100, state.score)
    assertEquals(1, state.combo)
  }

  @Test
  fun `should reset combo after miss`() {
    val notes = listOf(
      Note(1.0, 1, 0.0, false, false),
      Note(2.0, 1, 0.0, false, false)
    )

    val song = Song("Test", "Test", 10.0, notes)
    var state = GameState(song = song)

    // hit first
    state = engine.update(state, 1.0, listOf(
      InputEvent(1.0, 1, InputType.PRESS)
    ))

    // miss second
    state = engine.update(state, 3.0, emptyList())

    assertEquals(0, state.combo)
  }

  @Test
  fun `should simulate full gameplay flow`() {
    val notes = listOf(
      Note(1.0, 1, 0.0, false, false),
      Note(2.0, 2, 0.0, false, false),
      Note(3.0, 3, 0.0, false, false)
    )

    val song = Song("Test", "Test", 5.0, notes)
    var state = GameState(song = song)

    // timeline simulation? o.o
    val timeline = listOf(
      1.0 to InputEvent(1.0, 1, InputType.PRESS),
      2.05 to InputEvent(2.05, 2, InputType.PRESS),
      4.0 to null // miss last note
    )

    for ((time, input) in timeline) {
      state = engine.update(
        state = state,
        currentTime = time,
        inputs = input?.let { listOf(it) } ?: emptyList()
      )
    }

    assertEquals(200, state.score)
    assertEquals(0, state.combo)
    assertEquals(2, state.maxCombo)
  }
}