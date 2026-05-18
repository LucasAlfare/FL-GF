package com.lucasalfare.flgf.core

import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs
import org.w3c.dom.Element

// ==================== DATA MODELS ====================

/**
 *
 * Represents the player input for a single frame/tick.
 *
 * @property pressedFrets Frets currently being held down.
 *
 * @property justPressedFrets Frets that were pressed exactly on this frame (edge-triggered).
 *
 * @property justReleasedFrets Frets that were released exactly on this frame (edge-triggered).
 *
 * @property activateSpecial Whether the player is attempting to activate the special mode.
 */
data class PlayerInput(
  val pressedFrets: Set<Int>,
  val justPressedFrets: Set<Int>,
  val justReleasedFrets: Set<Int> = emptySet(),
  val activateSpecial: Boolean = false
)

/**
 *
 * Represents a note in the song chart.
 *
 * @property hitTime Timestamp (in milliseconds) when the note should be hit.
 *
 * @property lane Which lane/fret this note belongs to.
 *
 * @property duration Duration (in milliseconds) for sustain notes (0 = tap note).
 *
 * @property isSpecial Whether this note contributes to special energy sequences.
 */
data class Note(val hitTime: Long, val lane: Int, val duration: Long = 0L, val isSpecial: Boolean = false)

/**
 *
 * Runtime representation of a note currently active in the game.
 *
 * This wraps a static [Note] and adds mutable state used during gameplay.
 *
 * @property note The original immutable note data.
 *
 * @property hit Whether the note has been successfully hit.
 *
 * @property missed Whether the note has been missed.
 *
 * @property holding Whether the player is currently holding a sustain note.
 *
 * @property sustainProgress How much of the sustain duration has been completed.
 *
 * @property sustainBroken Whether the player released a sustain before it finished.
 */
data class NoteState(
  val note: Note,
  var hit: Boolean = false,
  var missed: Boolean = false,
  var holding: Boolean = false,
  var sustainProgress: Double = 0.0,
  var sustainBroken: Boolean = false,
  var specialDisabled: Boolean = false
)

/**
 *
 * Tracks scoring-related state.
 *
 * @property score Total accumulated score.
 *
 * @property combo Current combo streak.
 *
 * @property multiplier Score multiplier based on combo thresholds.
 */
data class ScoreState(var score: Int = 0, var combo: Int = 0, var multiplier: Int = 1)

/**
 *
 * Tracks the special ability (e.g., "star power") system.
 *
 * @property energy Current stored energy (0–100).
 *
 * @property active Whether the special mode is currently active.
 *
 * @property inSequence Whether the player is currently inside a valid special-note sequence.
 *
 * @property sequenceBroken Whether the current sequence has been invalidated.
 */
data class SpecialState(
  var energy: Int = 0,
  var active: Boolean = false,
  var inSequence: Boolean = false,
  var sequenceBroken: Boolean = false,
  var drainAccumulator: Double = 0.0
)

// ==================== GAME ENGINE ====================

/**
 *
 * Core gameplay engine responsible for:
 *
 * - Spawning notes
 *
 * - Resolving hits/misses
 *
 * - Managing sustain logic
 *
 * - Handling scoring and combo
 *
 * - Managing special ability state
 *
 * This engine is time-driven and expects external calls to [tick].
 *
 * @param notes Full chart of notes, sorted by time.
 *
 * @param hitWindow Allowed timing error (in milliseconds) for hitting notes.
 */
class GameEngine(
  private val notes: List<Note>,
  private val hitWindow: Long,
  private val spawnAheadTime: Long = 0L
) {
  private var time: Long = 0
  private var nextIndex = 0
  private var specialDisabledUntilTime: Long = Long.MIN_VALUE

  val notesStates = mutableListOf<NoteState>()
  val score = ScoreState()
  val special = SpecialState()

  fun tick(input: PlayerInput, currentTime: Long) {
    val dt = currentTime - time
    time = currentTime
    updateSpecial(input, dt)
    spawnNotes()
    resolveMissedNotes()
    resolveInputs(input)
    processSustain(input, dt)
    cleanup()
  }

  private fun spawnNotes() {
    while (nextIndex < notes.size && notes[nextIndex].hitTime <= time + spawnAheadTime) {
      val note = notes[nextIndex]
      notesStates.add(
        NoteState(
          note = note,
          specialDisabled = note.isSpecial && note.hitTime < specialDisabledUntilTime
        )
      )
      nextIndex++
    }
  }

  private fun resolveMissedNotes() {
    val missedNotes = notesStates
      .filter { !it.hit && !it.missed && time > it.note.hitTime + hitWindow }
      .sortedBy { it.note.hitTime }

    if (missedNotes.isEmpty()) return

    resetCombo()
    missedNotes.forEach {
      it.missed = true
      onNoteMiss(it)
    }
  }

  private fun resolveInputs(input: PlayerInput) {
    if (input.justPressedFrets.isEmpty()) return

    val hitNotes = mutableListOf<NoteState>()
    var hasWrongInput = false

    input.justPressedFrets.sorted().forEach { lane ->
      val noteState = findClosestPendingNoteForLane(lane)
      if (noteState == null) {
        hasWrongInput = true
        return@forEach
      }

      noteState.hit = true
      noteState.holding = noteState.note.duration > 0
      hitNotes += noteState
    }

    hitNotes.forEach {
      addHit()
      onNoteHit(it)
    }

    if (hasWrongInput) resetCombo()
  }

  private fun findClosestPendingNoteForLane(lane: Int): NoteState? {
    return notesStates
      .asSequence()
      .filter { !it.hit && !it.missed && it.note.lane == lane }
      .filter { abs(time - it.note.hitTime) <= hitWindow }
      .minWithOrNull(compareBy<NoteState>({ abs(time - it.note.hitTime) }, { it.note.hitTime }))
  }

  private fun onNoteHit(noteState: NoteState) {
    if (special.active) return
    if (noteState.note.isSpecial && !noteState.specialDisabled) {
      if (!special.inSequence) {
        special.inSequence = true
        special.sequenceBroken = false
      }
    } else if (noteState.note.isSpecial && noteState.specialDisabled) {
      return
    } else {
      if (special.inSequence && !special.sequenceBroken) {
        special.energy = (special.energy + 25).coerceAtMost(100)
      }
      special.inSequence = false
      special.sequenceBroken = false
    }

    println("Score data: $score")
  }

  private fun onNoteMiss(noteState: NoteState) {
    if (!noteState.note.isSpecial || noteState.specialDisabled) return

    special.sequenceBroken = true

    val currentIndex = notes.indexOfFirst { it == noteState.note }
    val sequenceEndTime = notes
      .drop(currentIndex + 1)
      .firstOrNull { !it.isSpecial }
      ?.hitTime ?: Long.MAX_VALUE

    specialDisabledUntilTime = sequenceEndTime

    notesStates.forEach {
      if (!it.hit && !it.missed &&
        it.note.isSpecial &&
        it.note.hitTime >= noteState.note.hitTime &&
        it.note.hitTime < sequenceEndTime
      ) {
        it.specialDisabled = true
      }
    }
  }

  private fun updateSpecial(input: PlayerInput, dt: Long) {
    if (input.activateSpecial && special.energy >= 50) special.active = true
    if (special.active) {
      special.drainAccumulator += 25.0 * dt / 1000.0
      val drainedEnergy = special.drainAccumulator.toInt()

      if (drainedEnergy > 0) {
        special.energy = (special.energy - drainedEnergy).coerceAtLeast(0)
        special.drainAccumulator -= drainedEnergy
      }

      if (special.energy <= 0) {
        special.energy = 0
        special.active = false
        special.drainAccumulator = 0.0
      }
      special.inSequence = false
      special.sequenceBroken = false
    } else {
      special.drainAccumulator = 0.0
    }
  }

  private fun specialMultiplier() = if (special.active) 2 else 1

  private fun processSustain(input: PlayerInput, dt: Long) {
    val sustainRatePerSecond = 50.0
    notesStates.forEach {
      if (!it.holding) return@forEach
      val remaining = it.note.duration - it.sustainProgress

      if (remaining <= 0.0) {
        it.holding = false
        return@forEach
      }

      val laneReleasedThisFrame = it.note.lane in input.justReleasedFrets
      val laneStillHeld = it.note.lane in input.pressedFrets

      if (laneReleasedThisFrame || !laneStillHeld) {
        it.holding = false
        if (it.sustainProgress < it.note.duration) {
          it.sustainBroken = true
        }
        return@forEach
      }

      val delta = minOf(dt.toDouble(), remaining)
      score.score += (delta / 1000.0 * sustainRatePerSecond * score.multiplier * specialMultiplier()).toInt()
      it.sustainProgress += delta
      if (it.sustainProgress >= it.note.duration) it.holding = false
    }
  }

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

  private fun resetCombo() {
    score.combo = 0
    score.multiplier = 1
  }

  private fun cleanup() {
    // Sustain notes need to remain alive until the tail has fully left the screen.
    notesStates.removeIf { (it.missed || it.hit) && time > despawnTime(it.note) }
  }

  private fun despawnTime(note: Note): Long {
    val visibleLifetime = maxOf(1000L, note.duration)
    return note.hitTime + visibleLifetime + 1000L
  }
}

/*

should hit a single note

should miss a note

should sustain long note and gain points progress

should break points gained of sustaining if released early

should hit simultaneous notes independently

should allow hitting simultaneous notes one by one

should keep sustain alive even if another simultaneous note is missed

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
 *
 * Represents all data required to play a song.
 *
 * @property notes List of parsed notes sorted by time.
 *
 * @property musicFileName Optional reference to the audio file associated with the chart.
 *
 * @property lengthMs Optional total song length in milliseconds.
 *
 * Notes:
 *
 * - Some chart formats may omit metadata, so nullable fields are expected.
 *
 * - The engine should not rely strictly on [lengthMs]; playback systems may define their own
 * timing.
 */
data class SongData(val notes: List<Note>, val musicFileName: String?, val lengthMs: Long?)

// ==================== XML PARSER ====================

/**
 *
 * Utility object responsible for parsing song chart data from XML.
 *
 * Expected XML structure (loosely defined):
 *
 * - Root element containing:
 *
 * - Multiple <Note> elements
 *
 * - Optional <Properties> section
 *
 * Design goals:
 *
 * - Be tolerant to malformed or incomplete data
 *
 * - Skip invalid notes instead of failing the entire parsing process
 *
 * - Convert all time units to milliseconds for engine compatibility
 */
object SongXmlParser {

  /**
   *
   * Parses an XML input stream into a [SongData] object.
   *
   * Processing steps:
   *
   * 1. Build DOM document
   *
   * 2. Normalize XML structure
   *
   * 3. Extract notes
   *
   * 4. Extract metadata properties
   *
   * 5. Sort notes by time (guarantees engine correctness)
   *
   * @param input Input stream containing XML chart data.
   *
   * @return Parsed [SongData] ready for use in the game engine.
   *
   * Important:
   *
   * - This method does not close the input stream.
   *
   * - Any XML parsing exception will propagate to the caller.
   */
  fun parse(input: InputStream): SongData {

    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)

    val root = doc.documentElement

    root.normalize()

    val notes = parseNotes(root)

    val (musicFileName, lengthMs) = parseProperties(root)

    return SongData(
      notes = notes.sortedBy { it.hitTime },
      musicFileName = musicFileName,
      lengthMs = lengthMs
    )
  }

  // ==================== NOTES PARSING ====================

  /**
   *
   * Extracts all <Note> elements from the XML root.
   *
   * Expected attributes per note:
   *
   * - time (seconds, required)
   *
   * - duration (seconds, optional, defaults to 0)
   *
   * - track (lane index, required)
   *
   * - special (optional flag, "1" = true)
   *
   * Error handling strategy:
   *
   * - Invalid or missing required fields cause the note to be skipped
   *
   * - Optional fields fallback to safe defaults
   *
   * Time conversion:
   *
   * - Input is in seconds (floating point)
   *
   * - Internally converted to milliseconds (Long)
   *
   * @param root Root XML element.
   *
   * @return List of parsed [Note] objects (unsorted).
   */
  private fun parseNotes(root: Element): List<Note> {

    val noteList = root.getElementsByTagName("Note")

    val result = mutableListOf<Note>()

    for (i in 0 until noteList.length) {

      val node = noteList.item(i) as? Element ?: continue

      /**
       *
       * Required: time (seconds)
       *
       * If invalid, skip the note entirely.
       */
      val timeSec = node.getAttribute("time").toDoubleOrNull() ?: continue

      /**
       *
       * Optional: duration (seconds)
       *
       * Defaults to 0 (tap note).
       */
      val durationSec = node.getAttribute("duration").toDoubleOrNull() ?: 0.0

      /**
       *
       * Required: track (lane index)
       *
       * If invalid, skip the note.
       */
      val lane = node.getAttribute("track").toIntOrNull() ?: continue

      /**
       *
       * Optional: special flag
       *
       * Convention: "1" means true, anything else is false.
       */
      val isSpecial = node.getAttribute("special") == "1"

      result.add(
        Note(
          hitTime = (timeSec * 1000).toLong(),
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
   *
   * Extracts optional metadata from the <Properties> section.
   *
   * Expected structure:
   *
   * <Properties>
   *
   * <MusicFileName>...</MusicFileName>
   *
   * <Length>...</Length> <!-- seconds -->
   *
   * </Properties>
   *
   * Behavior:
   *
   * - If <Properties> is missing, returns null values
   *
   * - Missing individual fields are also treated as null
   *
   * @param root Root XML element.
   *
   * @return Pair of (musicFileName, lengthMs)
   */
  private fun parseProperties(root: Element): Pair<String?, Long?> {

    val propsList = root.getElementsByTagName("Properties")

    if (propsList.length == 0) return null to null

    val props = propsList.item(0) as? Element ?: return null to null

    /**
     *
     * Optional music file reference.
     */
    val musicFileName = props.getElementsByTagName("MusicFileName").item(0)?.textContent

    /**
     *
     * Optional song length in seconds.
     *
     * Converted to milliseconds if valid.
     */
    val lengthSec = props.getElementsByTagName("Length").item(0)?.textContent?.toDoubleOrNull()

    val lengthMs = lengthSec?.let { (it * 1000).toLong() }

    return musicFileName to lengthMs
  }
}
