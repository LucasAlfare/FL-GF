package com.lucasalfare.flgf

import org.w3c.dom.Element
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

// ==================== DATA MODELS ====================

/**
 * Represents the player input for a single frame/tick.
 *
 * @property pressedFrets Frets currently being held down.
 * @property justPressedFrets Frets that were pressed exactly on this frame (edge-triggered).
 * @property activateSpecial Whether the player is attempting to activate the special mode.
 */
data class PlayerInput(
  val pressedFrets: Set<Int>,
  val justPressedFrets: Set<Int>,
  val activateSpecial: Boolean = false
)

/**
 * Represents a note in the song chart.
 *
 * @property time Timestamp (in milliseconds) when the note should be hit.
 * @property lane Which lane/fret this note belongs to.
 * @property duration Duration (in milliseconds) for sustain notes (0 = tap note).
 * @property isSpecial Whether this note contributes to special energy sequences.
 */
data class Note(
  val time: Long,
  val lane: Int,
  val duration: Long,
  val isSpecial: Boolean = false
)

/**
 * Runtime representation of a note currently active in the game.
 *
 * This wraps a static [Note] and adds mutable state used during gameplay.
 *
 * @property note The original immutable note data.
 * @property hit Whether the note has been successfully hit.
 * @property missed Whether the note has been missed.
 * @property holding Whether the player is currently holding a sustain note.
 * @property sustainProgress How much of the sustain duration has been completed.
 */
data class NoteState(
  val note: Note,
  var hit: Boolean = false,
  var missed: Boolean = false,
  var holding: Boolean = false,
  var sustainProgress: Double = 0.0
)

/**
 * Tracks scoring-related state.
 *
 * @property score Total accumulated score.
 * @property combo Current combo streak.
 * @property multiplier Score multiplier based on combo thresholds.
 */
data class ScoreState(
  var score: Int = 0,
  var combo: Int = 0,
  var multiplier: Int = 1
)

/**
 * Tracks the special ability (e.g., "star power") system.
 *
 * @property energy Current stored energy (0–100).
 * @property active Whether the special mode is currently active.
 *
 * @property inSequence Whether the player is currently inside a valid special-note sequence.
 * @property sequenceBroken Whether the current sequence has been invalidated.
 */
data class SpecialState(
  var energy: Int = 0,
  var active: Boolean = false,

  var inSequence: Boolean = false,
  var sequenceBroken: Boolean = false
)

// ==================== GAME ENGINE ====================

/**
 * Core gameplay engine responsible for:
 * - Spawning notes
 * - Resolving hits/misses
 * - Managing sustain logic
 * - Handling scoring and combo
 * - Managing special ability state
 *
 * This engine is time-driven and expects external calls to [tick].
 *
 * @param notes Full chart of notes, sorted by time.
 * @param hitWindow Allowed timing error (in milliseconds) for hitting notes.
 */
class GameEngine(
  private val notes: List<Note>,
  private val hitWindow: Long
) {

  /** Current engine time (ms). */
  private var time: Long = 0

  /** Index of the next note to spawn from the chart. */
  private var nextIndex = 0

  /** Notes currently active and interactable. */
  private val notesStates = mutableListOf<NoteState>()

  /** Public score state. */
  val score = ScoreState()

  /** Public special ability state. */
  val special = SpecialState()

  /**
   * Main update loop.
   *
   * Called every frame with current input and time.
   *
   * Execution order is critical:
   * 1. Update special (time-dependent drain/activation)
   * 2. Spawn notes (based on time window)
   * 3. Resolve hits/misses
   * 4. Process sustain scoring
   * 5. Cleanup old notes
   *
   * @param input Player input snapshot for this frame.
   * @param currentTime Absolute time in milliseconds.
   */
  fun tick(input: PlayerInput, currentTime: Long) {
    val dt = currentTime - time
    time = currentTime

    updateSpecial(input, dt)
    spawnNotes()
    resolveNotes(input)
    processSustain(input, dt)
    cleanup()
  }

  // ==================== SPAWN ====================

  /**
   * Moves notes from the chart into the active list when they are
   * close enough to be hittable.
   *
   * Notes are spawned slightly early (time + hitWindow) so the player
   * can interact with them within the allowed timing window.
   */
  private fun spawnNotes() {
    while (nextIndex < notes.size && notes[nextIndex].time <= time + hitWindow) {
      notesStates.add(NoteState(notes[nextIndex]))
      nextIndex++
    }
  }

  // ==================== CORE HIT RESOLUTION ====================

  /**
   * Resolves player input against the next group of notes.
   *
   * Design decisions:
   * - Only the earliest pending note group is evaluated.
   * - Notes with identical timestamps are treated as a chord.
   * - Input is matched against the entire chord, not individual notes.
   */
  private fun resolveNotes(input: PlayerInput) {

    val pending = notesStates.filter { !it.hit && !it.missed }
    if (pending.isEmpty()) return

    // Find the next note time (earliest)
    val nextTime = pending.minOf { it.note.time }

    // All notes at that exact time (chord/group)
    val group = pending.filter { it.note.time == nextTime }

    val inWindow = kotlin.math.abs(time - nextTime) <= hitWindow

    // ===== HIT LOGIC =====
    if (inWindow && input.justPressedFrets.isNotEmpty()) {

      val expected = group.map { it.note.lane }.toSet()
      val pressed = input.justPressedFrets

      val exact = expected == pressed
      val partial = expected.intersect(pressed).isNotEmpty()

      when {
        /**
         * Perfect match:
         * Player pressed exactly the required frets for the chord.
         */
        exact -> {
          addHit()
          group.forEach {
            it.hit = true
            it.holding = it.note.duration > 0
            onNoteHit(it.note)
          }
        }

        /**
         * Partial match:
         * Some correct frets were pressed, but not all.
         * This breaks combo and splits result into hit/miss per note.
         */
        partial -> {
          resetCombo()
          group.forEach {
            if (it.note.lane in pressed) {
              it.hit = true
              it.holding = it.note.duration > 0
              onNoteHit(it.note)
            } else {
              it.missed = true
              onNoteMiss(it.note)
            }
          }
        }

        /**
         * Completely incorrect input:
         * No overlap between expected and pressed frets.
         */
        else -> {
          resetCombo()
          group.forEach {
            it.missed = true
            onNoteMiss(it.note)
          }
        }
      }
    }

    // ===== MISS BY TIMEOUT =====
    group.forEach {
      if (!it.hit && !it.missed && time > it.note.time + hitWindow) {
        it.missed = true
        resetCombo()
        onNoteMiss(it.note)
      }
    }
  }

  // ==================== SPECIAL SYSTEM ====================

  /**
   * Handles logic when a note is successfully hit.
   *
   * Special rules:
   * - Special notes start or continue a sequence.
   * - A sequence ends when a non-special note is hit.
   * - If the sequence was not broken, energy is awarded.
   */
  private fun onNoteHit(note: Note) {

    if (special.active) return

    if (note.isSpecial) {
      if (!special.inSequence) {
        special.inSequence = true
        special.sequenceBroken = false
      }
    } else {
      // End of sequence
      if (special.inSequence && !special.sequenceBroken) {
        special.energy = (special.energy + 25).coerceAtMost(100)
      }
      special.inSequence = false
      special.sequenceBroken = false
    }
  }

  /**
   * Handles logic when a note is missed.
   *
   * Missing a special note breaks the current sequence.
   */
  private fun onNoteMiss(note: Note) {
    if (note.isSpecial) {
      special.sequenceBroken = true
    }
  }

  /**
   * Updates special mode state.
   *
   * - Activates if player requests and enough energy is available.
   * - Drains energy over time while active.
   * - Disables energy gain while active.
   */
  private fun updateSpecial(input: PlayerInput, dt: Long) {

    if (input.activateSpecial && special.energy >= 50) {
      special.active = true
    }

    if (special.active) {
      special.energy -= (25 * (dt / 1000.0)).toInt()

      if (special.energy <= 0) {
        special.energy = 0
        special.active = false
      }

      // Disable sequence tracking while active
      special.inSequence = false
      special.sequenceBroken = false
    }
  }

  /**
   * Returns the multiplier applied by special mode.
   */
  private fun specialMultiplier() =
    if (special.active) 2 else 1

  // ==================== SUSTAIN SYSTEM ====================

  /**
   * Processes sustain (hold) notes.
   *
   * Sustain scoring:
   * - Continuous score gain over time while holding correctly.
   * - Scaled by combo multiplier and special multiplier.
   * - Stops if player releases the fret early.
   */
  private fun processSustain(input: PlayerInput, dt: Long) {
    val sustainRatePerSecond = 50.0

    notesStates.forEach {
      if (!it.holding) return@forEach

      val holding = it.note.lane in input.pressedFrets

      if (!holding) {
        it.holding = false
        return@forEach
      }

      val remaining = it.note.duration - it.sustainProgress
      val delta = minOf(dt.toDouble(), remaining)

      val gained = (
          delta / 1000.0 *
              sustainRatePerSecond *
              score.multiplier *
              specialMultiplier()
          ).toInt()

      score.score += gained
      it.sustainProgress += delta

      if (it.sustainProgress >= it.note.duration) {
        it.holding = false
      }
    }
  }

  // ==================== SCORE SYSTEM ====================

  /**
   * Called on a successful full hit.
   *
   * - Increases combo
   * - Updates multiplier thresholds
   * - Adds base score scaled by multipliers
   */
  private fun addHit() {
    score.combo++

    score.multiplier = when {
      score.combo >= 30 -> 4
      score.combo >= 20 -> 3
      score.combo >= 10 -> 2
      else -> 1
    }

    score.score += 50 * score.multiplier * specialMultiplier()
  }

  /**
   * Resets combo and multiplier.
   */
  private fun resetCombo() {
    score.combo = 0
    score.multiplier = 1
  }

  // ==================== CLEANUP ====================

  /**
   * Removes old notes from memory after they are no longer relevant.
   *
   * Notes are kept briefly after being missed to allow visual feedback,
   * then discarded after a fixed delay (1000 ms).
   */
  private fun cleanup() {
    notesStates.removeIf {
      (it.missed || it.hit) && time > it.note.time + 1000
    }
  }
}

/*
should hit a single note
should miss a note
should sustain long note and gain points progress
should break points gained of sustaining if released early
should hit chord correctly
should partially hit chord and fail combo due to the not hit
should partially hit chord with sustain and gain progress for the correct hit note and fail combo for not hit
should handle rapid fire notes on same lane
should fail combo on one miss in rapid sequence
should fail combo on one wrong hit in rapid sequence
should handle very fast notes near hit window limit
should increase multiplier with combo
should reset combo and multiplier on miss
should advance special on getting all special notes of a sequence
should let activate special when enough energy
should drain the activated special energy over time
should increase score faster when special is active
should gain more points from sustain with special active
should stop gaining sustain bonus after special ends
should continue sustain normally after special ends
should ignore special phrases while special is active
 */

// ==================== SONG DATA ====================

/**
 * Represents all data required to play a song.
 *
 * @property notes List of parsed notes sorted by time.
 * @property musicFileName Optional reference to the audio file associated with the chart.
 * @property lengthMs Optional total song length in milliseconds.
 *
 * Notes:
 * - Some chart formats may omit metadata, so nullable fields are expected.
 * - The engine should not rely strictly on [lengthMs]; playback systems may define their own timing.
 */
data class SongData(
  val notes: List<Note>,
  val musicFileName: String?,
  val lengthMs: Long?
)

// ==================== XML PARSER ====================

/**
 * Utility object responsible for parsing song chart data from XML.
 *
 * Expected XML structure (loosely defined):
 * - Root element containing:
 *   - Multiple <Note> elements
 *   - Optional <Properties> section
 *
 * Design goals:
 * - Be tolerant to malformed or incomplete data
 * - Skip invalid notes instead of failing the entire parsing process
 * - Convert all time units to milliseconds for engine compatibility
 */
object SongXmlParser {

  /**
   * Parses an XML input stream into a [SongData] object.
   *
   * Processing steps:
   * 1. Build DOM document
   * 2. Normalize XML structure
   * 3. Extract notes
   * 4. Extract metadata properties
   * 5. Sort notes by time (guarantees engine correctness)
   *
   * @param input Input stream containing XML chart data.
   * @return Parsed [SongData] ready for use in the game engine.
   *
   * Important:
   * - This method does not close the input stream.
   * - Any XML parsing exception will propagate to the caller.
   */
  fun parse(input: InputStream): SongData {
    val doc = DocumentBuilderFactory.newInstance()
      .newDocumentBuilder()
      .parse(input)

    val root = doc.documentElement
    root.normalize()

    val notes = parseNotes(root)
    val (musicFileName, lengthMs) = parseProperties(root)

    return SongData(
      notes = notes.sortedBy { it.time },
      musicFileName = musicFileName,
      lengthMs = lengthMs
    )
  }

  // ==================== NOTES PARSING ====================

  /**
   * Extracts all <Note> elements from the XML root.
   *
   * Expected attributes per note:
   * - time (seconds, required)
   * - duration (seconds, optional, defaults to 0)
   * - track (lane index, required)
   * - special (optional flag, "1" = true)
   *
   * Error handling strategy:
   * - Invalid or missing required fields cause the note to be skipped
   * - Optional fields fallback to safe defaults
   *
   * Time conversion:
   * - Input is in seconds (floating point)
   * - Internally converted to milliseconds (Long)
   *
   * @param root Root XML element.
   * @return List of parsed [Note] objects (unsorted).
   */
  private fun parseNotes(root: Element): List<Note> {
    val noteList = root.getElementsByTagName("Note")
    val result = mutableListOf<Note>()

    for (i in 0 until noteList.length) {
      val node = noteList.item(i) as? Element ?: continue

      /**
       * Required: time (seconds)
       * If invalid, skip the note entirely.
       */
      val timeSec = node.getAttribute("time").toDoubleOrNull() ?: continue

      /**
       * Optional: duration (seconds)
       * Defaults to 0 (tap note).
       */
      val durationSec = node.getAttribute("duration").toDoubleOrNull() ?: 0.0

      /**
       * Required: track (lane index)
       * If invalid, skip the note.
       */
      val lane = node.getAttribute("track").toIntOrNull() ?: continue

      /**
       * Optional: special flag
       * Convention: "1" means true, anything else is false.
       */
      val isSpecial = node.getAttribute("special") == "1"

      result.add(
        Note(
          time = (timeSec * 1000).toLong(),
          lane = lane,
          duration = (durationSec * 1000).toLong(),
          isSpecial = isSpecial
        )
      )
    }

    return result
  }

  // ==================== METADATA PARSING ====================

  /**
   * Extracts optional metadata from the <Properties> section.
   *
   * Expected structure:
   * <Properties>
   *   <MusicFileName>...</MusicFileName>
   *   <Length>...</Length> <!-- seconds -->
   * </Properties>
   *
   * Behavior:
   * - If <Properties> is missing, returns null values
   * - Missing individual fields are also treated as null
   *
   * @param root Root XML element.
   * @return Pair of (musicFileName, lengthMs)
   */
  private fun parseProperties(root: Element): Pair<String?, Long?> {
    val propsList = root.getElementsByTagName("Properties")
    if (propsList.length == 0) return null to null

    val props = propsList.item(0) as? Element ?: return null to null

    /**
     * Optional music file reference.
     */
    val musicFileName = props
      .getElementsByTagName("MusicFileName")
      .item(0)
      ?.textContent

    /**
     * Optional song length in seconds.
     * Converted to milliseconds if valid.
     */
    val lengthSec = props
      .getElementsByTagName("Length")
      .item(0)
      ?.textContent
      ?.toDoubleOrNull()

    val lengthMs = lengthSec?.let { (it * 1000).toLong() }

    return musicFileName to lengthMs
  }
}