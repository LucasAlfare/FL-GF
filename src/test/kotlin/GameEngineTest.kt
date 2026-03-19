import com.lucasalfare.flgf.GameEngine
import com.lucasalfare.flgf.GameState
import com.lucasalfare.flgf.HitJudge
import com.lucasalfare.flgf.HitWindow
import com.lucasalfare.flgf.Note
import com.lucasalfare.flgf.NoteSpawner
import com.lucasalfare.flgf.PlayerInput
import com.lucasalfare.flgf.ScoreSystem
import com.lucasalfare.flgf.Song
import com.lucasalfare.flgf.SpecialState
import com.lucasalfare.flgf.SpecialSystem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameEngineTest {

  private lateinit var engine: GameEngine

  @BeforeEach
  fun setup() {
    engine = GameEngine(
      hitWindow = HitWindow(0.1),
      hitJudge = HitJudge(),
      spawner = NoteSpawner(1.0),
      scoreSystem = ScoreSystem(),
      specialSystem = SpecialSystem()
    )
  }

  // --------------------------
  // HELPERS
  // --------------------------

  private fun song(vararg notes: Note) =
    Song("test", "test", 100.0, notes.toList())

  private fun baseState(song: Song) = GameState(song = song)

  private fun pressOnce(vararg lanes: Int) =
    PlayerInput(
      pressedFrets = lanes.toSet(),
      justPressedFrets = lanes.toSet()
    )

  private fun holdOnly(vararg lanes: Int) =
    PlayerInput(
      pressedFrets = lanes.toSet(),
      justPressedFrets = emptySet()
    )

  // --------------------------
  // TESTS
  // --------------------------

  @Test
  fun `should hit a single note`() {
    val s = song(Note(1.0, 0, 0.0, null))
    var state = baseState(s)

    state = engine.tick(state, pressOnce(0), 1.0)

    assertEquals(1, state.scoreState.combo)
    assertTrue(state.scoreState.score > 0)
  }

  @Test
  fun `should miss a note`() {
    val s = song(Note(1.0, 0, 0.0, null))
    var state = baseState(s)

    state = engine.tick(state, pressOnce(1), 1.0)

    assertEquals(0, state.scoreState.combo)
  }

  @Test
  fun `should sustain long note and gain points progress`() {
    val s = song(Note(1.0, 0, 2.0, null))
    var state = baseState(s)

    state = engine.tick(state, pressOnce(0), 1.0)
    val afterHit = state.scoreState.score

    state = engine.tick(state, holdOnly(0), 2.0)

    assertTrue(state.scoreState.score > afterHit)
  }

  @Test
  fun `should break points gained of sustaining if released early`() {
    val s = song(Note(1.0, 0, 2.0, null))
    var state = baseState(s)

    state = engine.tick(state, pressOnce(0), 1.0)
    state = engine.tick(state, holdOnly(0), 1.5)

    val midScore = state.scoreState.score

    state = engine.tick(state, holdOnly(), 2.0)

    assertEquals(midScore, state.scoreState.score)
  }

  @Test
  fun `should hit chord correctly`() {
    val s = song(
      Note(1.0, 0, 0.0, null),
      Note(1.0, 1, 0.0, null)
    )

    var state = baseState(s)

    state = engine.tick(state, pressOnce(0, 1), 1.0)

    assertEquals(1, state.scoreState.combo)
  }

  @Test
  fun `should partially hit chord and fail combo`() {
    val s = song(
      Note(1.0, 0, 0.0, null),
      Note(1.0, 1, 0.0, null)
    )

    var state = baseState(s)

    state = engine.tick(state, pressOnce(0), 1.0)

    assertEquals(0, state.scoreState.combo)
  }

  @Test
  fun `should partially hit chord with sustain and fail combo`() {
    val s = song(
      Note(1.0, 0, 2.0, null),
      Note(1.0, 1, 0.0, null)
    )

    var state = baseState(s)

    state = engine.tick(state, pressOnce(0), 1.0)

    assertEquals(0, state.scoreState.combo)
  }

  @Test
  fun `should handle rapid fire notes on same lane`() {
    val s = song(
      Note(1.0, 0, 0.0, null),
      Note(1.1, 0, 0.0, null),
      Note(1.2, 0, 0.0, null)
    )

    var state = baseState(s)

    state = engine.tick(state, pressOnce(0), 1.0)
    state = engine.tick(state, holdOnly(0), 1.05) // frame intermediário
    state = engine.tick(state, pressOnce(0), 1.1)
    state = engine.tick(state, holdOnly(0), 1.15)
    state = engine.tick(state, pressOnce(0), 1.2)

    assertEquals(3, state.scoreState.combo)
  }

  @Test
  fun `should fail combo on one miss in rapid sequence`() {
    val s = song(
      Note(1.0, 0, 0.0, null),
      Note(1.1, 0, 0.0, null),
      Note(1.2, 0, 0.0, null)
    )

    var state = baseState(s)

    state = engine.tick(state, pressOnce(0), 1.0)
    state = engine.tick(state, holdOnly(0), 1.05)

    state = engine.tick(state, pressOnce(1), 1.1) // miss
    state = engine.tick(state, holdOnly(0), 1.15)

    state = engine.tick(state, pressOnce(0), 1.2)

    assertEquals(1, state.scoreState.combo)
  }

  @Test
  fun `should fail combo on wrong hit in rapid sequence`() {
    val s = song(
      Note(1.0, 0, 0.0, null),
      Note(1.1, 1, 0.0, null)
    )

    var state = baseState(s)

    state = engine.tick(state, pressOnce(0), 1.0)
    state = engine.tick(state, pressOnce(0), 1.1)

    assertEquals(0, state.scoreState.combo)
  }

  @Test
  fun `should handle very fast notes near hit window limit`() {
    val s = song(Note(1.0, 0, 0.0, null))
    var state = baseState(s)

    state = engine.tick(state, pressOnce(0), 1.09)

    assertTrue(state.scoreState.score > 0)
  }

  @Test
  fun `should increase multiplier with combo`() {
    val notes = (1..10).map {
      Note(it.toDouble(), 0, 0.0, null)
    }

    var state = baseState(song(*notes.toTypedArray()))

    notes.forEach {
      state = engine.tick(state, pressOnce(0), it.time)
    }

    assertTrue(state.scoreState.multiplier >= 2)
  }

  @Test
  fun `should reset combo and multiplier on miss`() {
    val s = song(
      Note(1.0, 0, 0.0, null),
      Note(2.0, 0, 0.0, null)
    )

    var state = baseState(s)

    state = engine.tick(state, pressOnce(0), 1.0)
    state = engine.tick(state, pressOnce(1), 2.0)

    assertEquals(0, state.scoreState.combo)
    assertEquals(1, state.scoreState.multiplier)
  }

  @Test
  fun `should advance special on getting all special notes`() {
    val s = song(
      Note(1.0, 0, 0.0, 1),
      Note(2.0, 0, 0.0, 1)
    )

    var state = baseState(s)

    state = engine.tick(state, pressOnce(0), 1.0)
    state = engine.tick(state, pressOnce(0), 2.0)

    assertTrue(state.specialState.energy > 0)
  }

  @Test
  fun `should activate special when enough energy`() {
    val special = SpecialState(energy = 50)

    val activated = SpecialSystem().tryActivate(special)

    assertTrue(activated.active)
  }

  @Test
  fun `should drain special over time`() {
    val system = SpecialSystem()
    var state = SpecialState(energy = 100, active = true)

    state = system.update(state, 1.0)

    assertTrue(state.energy < 100)
  }

  @Test
  fun `should increase score faster when special is active`() {
    val s = song(Note(1.0, 0, 0.0, null))

    var state = baseState(s).copy(
      specialState = SpecialState(energy = 100, active = true)
    )

    state = engine.tick(state, pressOnce(0), 1.0)

    assertTrue(state.scoreState.score >= 100)
  }

  @Test
  fun `should ignore special phrases while special is active`() {
    val s = song(
      Note(1.0, 0, 0.0, 1),
      Note(1.01, 0, 0.0, 1) // mesma janela → sem drain relevante
    )

    var state = baseState(s).copy(
      specialState = SpecialState(energy = 100, active = true)
    )

    val before = state.specialState.energy

    state = engine.tick(state, pressOnce(0), 1.0)
    state = engine.tick(state, pressOnce(0), 1.01)

    assertTrue(state.specialState.energy < before) // só drenou
  }
}