import com.lucasalfare.flgf.a_domain.HitJudge
import com.lucasalfare.flgf.a_domain.HitWindow
import com.lucasalfare.flgf.a_domain.InputFrame
import com.lucasalfare.flgf.a_domain.Note
import com.lucasalfare.flgf.a_domain.NoteSpawner
import com.lucasalfare.flgf.a_domain.ScoreSystem
import com.lucasalfare.flgf.a_domain.Song
import com.lucasalfare.flgf.a_domain.SpecialSystem
import com.lucasalfare.flgf.a_domain.state.GameState
import com.lucasalfare.flgf.a_domain.state.NoteState
import com.lucasalfare.flgf.b_usecase.GameEngine
import com.lucasalfare.flgf.c_infra.fake.FakeClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameEngineTest {

  private fun createEngine(clock: FakeClock): GameEngine {
    return GameEngine(
      clock = clock,
      hitWindow = HitWindow(0.05, 0.1),
      hitJudge = HitJudge(),
      spawner = NoteSpawner(5.0),
      scoreSystem = ScoreSystem(),
      specialSystem = SpecialSystem()
    )
  }

  private fun baseState(song: Song) = GameState(song = song)

  @Test
  fun `should hit a single note`() {
    val clock = FakeClock(1.0)
    val engine = createEngine(clock)

    val song = Song(
      "t", "a", 10.0, listOf(
        Note(1.0, 1, 0.0, special = false, hopo = false)
      )
    )

    var state = baseState(song)

    state = engine.tick(state, InputFrame(1.0, setOf(1), strum = true))

    assertEquals(1, state.scoreState.combo)
    assertTrue(state.scoreState.score > 0)
  }

  @Test
  fun `should miss a note`() {
    val clock = FakeClock(2.0)
    val engine = createEngine(clock)

    val song = Song(
      "t", "a", 10.0, listOf(
        Note(1.0, 1, 0.0, special = false, hopo = false)
      )
    )

    var state = baseState(song)

    state = engine.tick(state, InputFrame(2.0, emptySet(), strum = false))

    assertEquals(0, state.scoreState.combo)
  }

  @Test
  fun `should sustain long note and gain progress`() {
    val clock = FakeClock(1.0)
    val engine = createEngine(clock)

    val song = Song(
      "t", "a", 10.0, listOf(
        Note(1.0, 1, 2.0, special = false, hopo = false)
      )
    )

    var state = baseState(song)

    state = engine.tick(state, InputFrame(1.0, setOf(1), true))

    clock.time = 2.0
    state = engine.tick(state, InputFrame(2.0, setOf(1), false))

    val note = state.activeNotes.first()
    assertTrue(note.sustainProgress > 0)
  }

  @Test
  fun `should fail sustain if released early`() {
    val clock = FakeClock(1.0)
    val engine = createEngine(clock)

    val song = Song(
      "t", "a", 10.0, listOf(
        Note(1.0, 1, 2.0, special = false, hopo = false)
      )
    )

    var state = baseState(song)

    state = engine.tick(state, InputFrame(1.0, setOf(1), true))

    clock.time = 1.5
    state = engine.tick(state, InputFrame(1.5, emptySet(), false))

    val note = state.activeNotes.first()
    assertEquals(NoteState.HIT, note.state)
    assertTrue(note.sustainProgress < note.note.duration)
  }

  @Test
  fun `should hit chord correctly`() {
    val clock = FakeClock(1.0)
    val engine = createEngine(clock)

    val song = Song(
      "t", "a", 10.0, listOf(
        Note(1.0, 1, 0.0, special = false, hopo = false),
        Note(1.0, 2, 0.0, special = false, hopo = false)
      )
    )

    var state = baseState(song)

    state = engine.tick(state, InputFrame(1.0, setOf(1, 2), true))

    assertEquals(2, state.scoreState.combo)
  }

  @Test
  fun `should partially hit chord`() {
    val clock = FakeClock(1.0)
    val engine = createEngine(clock)

    val song = Song(
      "t", "a", 10.0, listOf(
        Note(1.0, 1, 0.0, special = false, hopo = false),
        Note(1.0, 2, 0.0, special = false, hopo = false)
      )
    )

    var state = baseState(song)

    state = engine.tick(state, InputFrame(1.0, setOf(1), true))

    assertTrue(state.scoreState.combo < 2)
  }

  @Test
  fun `should partially hit chord with sustain`() {
    val clock = FakeClock(1.0)
    val engine = createEngine(clock)

    val song = Song(
      "t", "a", 10.0, listOf(
        Note(1.0, 1, 2.0, special = false, hopo = false),
        Note(1.0, 2, 2.0, special = false, hopo = false)
      )
    )

    var state = baseState(song)

    state = engine.tick(state, InputFrame(1.0, setOf(1), true))

    clock.time = 2.0
    state = engine.tick(state, InputFrame(2.0, setOf(1), false))

    val notes = state.activeNotes

    val hit = notes.find { it.note.lane == 1 }
    val missed = notes.find { it.note.lane == 2 }

    assertNotNull(hit)
    assertNotNull(missed)
    assertEquals(NoteState.MISSED, missed.state)
  }

  @Test
  fun `should handle rapid fire notes on same lane`() {
    val clock = FakeClock(0.0)
    val engine = createEngine(clock)

    val notes = (0 until 20).map {
      Note(time = 1.0 + it * 0.05, lane = 3, duration = 0.0, special = false, hopo = false)
    }

    val song = Song("rapid", "test", 10.0, notes)
    var state = baseState(song)

    notes.forEach { note ->
      clock.time = note.time
      state = engine.tick(state, InputFrame(note.time, setOf(3), strum = true))
    }

    assertEquals(20, state.scoreState.combo)
    assertTrue(state.scoreState.score > 0)
  }

  @Test
  fun `should break combo on one miss in rapid sequence`() {
    val clock = FakeClock(0.0)
    val engine = createEngine(clock)

    val notes = (0 until 10).map {
      Note(time = 1.0 + it * 0.05, lane = 3, duration = 0.0, special = false, hopo = false)
    }

    val song = Song("rapid", "test", 10.0, notes)
    var state = baseState(song)

    notes.forEachIndexed { index, note ->
      if (index == 5) {
        // don't play
        clock.time = note.time
        state = engine.tick(
          state,
          InputFrame(note.time, emptySet(), strum = false)
        )

        // advances time, simulating "missing the note"
        clock.time = note.time + 0.2
        state = engine.tick(
          state,
          InputFrame(clock.time, emptySet(), strum = false)
        )

      } else {
        clock.time = note.time
        state = engine.tick(
          state,
          InputFrame(note.time, setOf(3), strum = true)
        )
      }
    }

    // in miss, combo is lost
    assertTrue(state.scoreState.combo < 10)

    // streak/combo should be broke in some point
    assertTrue(state.scoreState.maxCombo >= 5)
  }

  @Test
  fun `should handle very fast notes near hit window limit`() {
    val clock = FakeClock(0.0)
    val engine = createEngine(clock)

    // extremely fast notes (basically in the window limit)
    val notes = (0 until 10).map {
      Note(time = 1.0 + it * 0.09, lane = 2, duration = 0.0, special = false, hopo = false)
    }

    val song = Song("tight", "test", 10.0, notes)
    var state = baseState(song)

    notes.forEach { note ->
      clock.time = note.time
      state = engine.tick(state, InputFrame(note.time, setOf(2), strum = true))
    }

    assertEquals(10, state.scoreState.combo)
  }
}