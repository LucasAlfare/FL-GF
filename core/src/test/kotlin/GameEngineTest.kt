import com.lucasalfare.flgf.core.GameEngine
import com.lucasalfare.flgf.core.Note
import com.lucasalfare.flgf.core.PlayerInput
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameEngineTest {

  private lateinit var engine: GameEngine

// --------------------------
// HELPERS
// --------------------------

  private fun engine(vararg notes: Note) =
    GameEngine(notes.toList(), hitWindow = 100) // 100ms

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
    engine = engine(Note(1000, 0, 0))

    engine.tick(pressOnce(0), 1000)

    assertEquals(1, engine.score.combo)
    assertTrue(engine.score.score > 0)
  }

  @Test
  fun `should miss a note`() {
    engine = engine(Note(1000, 0, 0))

    engine.tick(pressOnce(1), 1000)

    assertEquals(0, engine.score.combo)
  }

  @Test
  fun `should sustain long note and gain points progress`() {
    engine = engine(Note(1000, 0, 2000))

    engine.tick(pressOnce(0), 1000)
    val afterHit = engine.score.score

    engine.tick(holdOnly(0), 2000)

    assertTrue(engine.score.score > afterHit)
  }

  @Test
  fun `should break sustain if released early`() {
    engine = engine(Note(1000, 0, 2000))

    engine.tick(pressOnce(0), 1000)
    engine.tick(holdOnly(0), 1500)

    val mid = engine.score.score

    engine.tick(holdOnly(), 2000)

    assertEquals(mid, engine.score.score)
  }

  @Test
  fun `should hit chord correctly`() {
    engine = engine(
      Note(1000, 0, 0),
      Note(1000, 1, 0)
    )

    engine.tick(pressOnce(0, 1), 1000)

    assertEquals(1, engine.score.combo)
  }

  @Test
  fun `should partially hit chord and fail combo`() {
    engine = engine(
      Note(1000, 0, 0),
      Note(1000, 1, 0)
    )

    engine.tick(pressOnce(0), 1000)

    assertEquals(0, engine.score.combo)
  }

  @Test
  fun `should partially hit chord with sustain`() {
    engine = engine(
      Note(1000, 0, 2000),
      Note(1000, 1, 0)
    )

    engine.tick(pressOnce(0), 1000)

    assertEquals(0, engine.score.combo)
  }

  @Test
  fun `should handle rapid fire notes`() {
    engine = engine(
      Note(1000, 0, 0),
      Note(1100, 0, 0),
      Note(1200, 0, 0)
    )

    engine.tick(pressOnce(0), 1000)
    engine.tick(holdOnly(0), 1050)
    engine.tick(pressOnce(0), 1100)
    engine.tick(holdOnly(0), 1150)
    engine.tick(pressOnce(0), 1200)

    assertEquals(3, engine.score.combo)
  }

  @Test
  fun `should fail combo on miss in rapid`() {
    engine = engine(
      Note(1000, 0, 0),
      Note(1100, 0, 0),
      Note(1200, 0, 0)
    )

    engine.tick(pressOnce(0), 1000)
    engine.tick(holdOnly(0), 1050)

    engine.tick(pressOnce(1), 1100) // miss
    engine.tick(holdOnly(0), 1150)

    engine.tick(pressOnce(0), 1200)

    assertEquals(1, engine.score.combo)
  }

  @Test
  fun `should fail combo on wrong hit`() {
    engine = engine(
      Note(1000, 0, 0),
      Note(1100, 1, 0)
    )

    engine.tick(pressOnce(0), 1000)
    engine.tick(pressOnce(0), 1100)

    assertEquals(0, engine.score.combo)
  }

  @Test
  fun `should hit near window limit`() {
    engine = engine(Note(1000, 0, 0))

    engine.tick(pressOnce(0), 1090) // dentro de 100ms

    assertTrue(engine.score.score > 0)
  }

  @Test
  fun `should increase multiplier`() {
    val notes = (1..10).map {
      Note(it * 1000L, 0, 0)
    }

    engine = GameEngine(notes, 100)

    notes.forEach {
      engine.tick(pressOnce(0), it.time)
    }

    assertTrue(engine.score.multiplier >= 2)
  }

  @Test
  fun `should reset combo on miss`() {
    engine = engine(
      Note(1000, 0, 0),
      Note(2000, 0, 0)
    )

    engine.tick(pressOnce(0), 1000)
    engine.tick(pressOnce(1), 2000)

    assertEquals(0, engine.score.combo)
    assertEquals(1, engine.score.multiplier)
  }

  @Test
  fun `should increase score with special active`() {
    engine = engine(Note(1000, 0, 0))

    engine.special.energy = 100
    engine.special.active = true

    engine.tick(pressOnce(0), 1000)

    assertTrue(engine.score.score >= 100)
  }
}
