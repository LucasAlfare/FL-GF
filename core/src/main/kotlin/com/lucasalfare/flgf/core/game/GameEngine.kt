package com.lucasalfare.flgf.core.game

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
  var specialDisabled: Boolean = false,
  var specialPhraseEnd: Boolean = false,
  var index: Int = 0
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
 * Single combo threshold that upgrades the score multiplier.
 */
data class ComboMultiplierTier(val comboThreshold: Int, val multiplier: Int)

/**
 * Tunable rules for the special meter and special-active score behavior.
 */
data class SpecialScoringRules(
  val energyPerCompletedSpecialPhrase: Int = 25,
  val activationEnergyThreshold: Int = 50,
  val maximumEnergy: Int = 100,
  val drainPerSecond: Double = 25.0,
  val activeScoreMultiplier: Int = 2
)

/**
 * All score-related tuning knobs in one place.
 *
 * This keeps hit value, combo tiers, sustain value, and special meter behavior
 * centralized so gameplay balancing can be adjusted without touching the engine flow.
 */
data class ScoringRules(
  val pointsPerHit: Int = 50,
  val sustainPointsPerSecond: Double = 50.0,
  val comboMultiplierTiers: List<ComboMultiplierTier> = listOf(
    ComboMultiplierTier(comboThreshold = 10, multiplier = 2),
    ComboMultiplierTier(comboThreshold = 20, multiplier = 3),
    ComboMultiplierTier(comboThreshold = 30, multiplier = 4)
  ),
  val special: SpecialScoringRules = SpecialScoringRules()
) {
  fun multiplierFor(combo: Int): Int {
    return comboMultiplierTiers
      .sortedBy { it.comboThreshold }
      .lastOrNull { combo >= it.comboThreshold }
      ?.multiplier ?: 1
  }

  fun hitPoints(combo: Int, specialActive: Boolean): Int {
    return pointsPerHit * multiplierFor(combo) * specialScoreMultiplier(specialActive)
  }

  fun sustainPoints(deltaMs: Double, comboMultiplier: Int, specialActive: Boolean): Int {
    return (deltaMs / 1000.0 * sustainPointsPerSecond * comboMultiplier * specialScoreMultiplier(specialActive)).toInt()
  }

  fun canActivateSpecial(energy: Int): Boolean {
    return energy >= special.activationEnergyThreshold
  }

  fun drainSpecialEnergy(currentEnergy: Int, deltaMs: Long): Int {
    val drained = (special.drainPerSecond * deltaMs / 1000.0).toInt()
    return (currentEnergy - drained).coerceAtLeast(0)
  }

  fun gainSpecialEnergy(currentEnergy: Int): Int {
    return (currentEnergy + special.energyPerCompletedSpecialPhrase).coerceAtMost(special.maximumEnergy)
  }

  private fun specialScoreMultiplier(active: Boolean): Int {
    return if (active) special.activeScoreMultiplier else 1
  }
}

/**
 * Thin gameplay helper that applies [ScoringRules] to mutable score state.
 */
class ScoringSystem(
  private val rules: ScoringRules = ScoringRules()
) {
  fun multiplierFor(combo: Int): Int = rules.multiplierFor(combo)

  fun registerHit(score: ScoreState, specialActive: Boolean): Int {
    score.combo++
    score.multiplier = rules.multiplierFor(score.combo)
    val points = rules.hitPoints(score.combo, specialActive)
    score.score += points
    return points
  }

  fun registerSustain(score: ScoreState, deltaMs: Double, specialActive: Boolean): Int {
    val points = rules.sustainPoints(deltaMs, score.multiplier, specialActive)
    score.score += points
    return points
  }

  fun resetCombo(score: ScoreState) {
    score.combo = 0
    score.multiplier = 1
  }

  fun canActivateSpecial(energy: Int): Boolean = rules.canActivateSpecial(energy)

  fun gainSpecialEnergy(currentEnergy: Int): Int = rules.gainSpecialEnergy(currentEnergy)

  fun drainSpecialEnergy(currentEnergy: Int, deltaMs: Long): Int = rules.drainSpecialEnergy(currentEnergy, deltaMs)

  fun updateSpecialDrain(special: SpecialState, deltaMs: Long) {
    if (deltaMs <= 0L) return

    special.drainAccumulator += rules.special.drainPerSecond * deltaMs / 1000.0
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
  }
}

/**
 *
 * Tracks the special ability (e.g., "star power") system.
 *
 * @property energy Current stored energy (0â€“100).
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

/**
 * Represents the player's input state for a single frame/tick using bitmasks
 * for zero-allocation, cache-friendly fret state storage.
 *
 * Each fret lane corresponds to a bit position: lane 0 → bit 0, lane 1 → bit 1,
 * and so on. This allows up to 31 lanes on a standard 32-bit Int.
 *
 * Bitmask semantics:
 * - A bit set to 1 means that lane is active for the given state.
 * - A bit set to 0 means that lane is inactive.
 *
 * Example for a 5-lane guitar: lanes 0–4 map to bits 0–4 (values 1, 2, 4, 8, 16).
 *
 * This is a value class backed by a single Long that packs all four fields into
 * one 64-bit integer, making the entire input state a single primitive — no heap
 * allocation, no GC pressure, safe to create and discard every frame.
 *
 * Bit layout of the backing [packed] Long (low → high):
 * ```
 * bits  0–14 : pressedFrets    (15 bits, supports up to 15 lanes)
 * bits 15–29 : justPressedFrets
 * bits 30–44 : justReleasedFrets
 * bit  63    : activateSpecial
 * ```
 *
 * Use the provided extension functions and constants to read individual fields
 * rather than manipulating [packed] directly.
 */
@JvmInline
value class PlayerInput(val packed: Long) {

  /**
   * Bitmask of frets currently held down.
   * Bit i is set if lane i is pressed this frame.
   */
  val pressedFrets: Int
    get() = (packed and PRESSED_MASK).toInt()

  /**
   * Bitmask of frets that transitioned from released to pressed this frame (edge-triggered).
   * Bit i is set if lane i was just pressed.
   */
  val justPressedFrets: Int
    get() = ((packed ushr JUST_PRESSED_SHIFT) and FIELD_MASK).toInt()

  /**
   * Bitmask of frets that transitioned from pressed to released this frame (edge-triggered).
   * Bit i is set if lane i was just released.
   */
  val justReleasedFrets: Int
    get() = ((packed ushr JUST_RELEASED_SHIFT) and FIELD_MASK).toInt()

  /**
   * True if the player is attempting to activate special mode this frame.
   */
  val activateSpecial: Boolean
    get() = (packed and SPECIAL_MASK) != 0L

  /**
   * Returns true if the given [lane] is currently held down.
   */
  fun isPressed(lane: Int): Boolean = (pressedFrets ushr lane) and 1 == 1

  /**
   * Returns true if the given [lane] was just pressed this frame.
   */
  fun isJustPressed(lane: Int): Boolean = (justPressedFrets ushr lane) and 1 == 1

  /**
   * Returns true if the given [lane] was just released this frame.
   */
  fun isJustReleased(lane: Int): Boolean = (justReleasedFrets ushr lane) and 1 == 1

  companion object {
    // ── Bit layout constants ─────────────────────────────────────────────
    private const val FIELD_BITS = 15
    private const val FIELD_MASK = (1L shl FIELD_BITS) - 1L  // 0x7FFF

    const val PRESSED_MASK: Long = FIELD_MASK                 // bits  0–14
    const val JUST_PRESSED_SHIFT: Int = FIELD_BITS                 // bit  15
    const val JUST_RELEASED_SHIFT: Int = FIELD_BITS * 2             // bit  30
    const val SPECIAL_MASK: Long = 1L shl 63                  // bit  63

    /** The empty/idle input: no frets pressed, special not activated. */
    val NONE = PlayerInput(0L)

    /**
     * Constructs a [PlayerInput] from pre-computed bitmasks.
     *
     * Prefer this factory over the primary constructor when building inputs
     * from raw bitmask values (e.g. inside [InputHandler]).
     *
     * @param pressed         Bitmask of currently held frets.
     * @param justPressed     Bitmask of frets pressed this frame.
     * @param justReleased    Bitmask of frets released this frame.
     * @param activateSpecial True if special activation was requested.
     */
    fun of(
      pressed: Int,
      justPressed: Int,
      justReleased: Int,
      activateSpecial: Boolean
    ): PlayerInput {
      val special = if (activateSpecial) SPECIAL_MASK else 0L
      val packed = (pressed.toLong() and FIELD_MASK) or
          ((justPressed.toLong() and FIELD_MASK) shl JUST_PRESSED_SHIFT) or
          ((justReleased.toLong() and FIELD_MASK) shl JUST_RELEASED_SHIFT) or
          special
      return PlayerInput(packed)
    }
  }
}

/**
 * Iterates over every lane index whose bit is set in this bitmask, invoking
 * [block] for each. Equivalent to iterating a Set<Int> of active lanes but
 * with zero allocation.
 *
 * @param maxLanes Upper bound on the number of lanes to check (default 15).
 */
inline fun Int.forEachSetBit(maxLanes: Int = 15, block: (lane: Int) -> Unit) {
  var mask = this
  while (mask != 0) {
    val lane = Integer.numberOfTrailingZeros(mask)
    if (lane >= maxLanes) break
    block(lane)
    mask = mask and (mask - 1) // clear lowest set bit
  }
}


/**
 * Asymmetric timing tolerance for registering a valid note hit.
 *
 * Separating [early] and [late] tolerances allows tuning feel independently:
 * many rhythm games are more forgiving when the player anticipates a note
 * (hits early) than when they react late. Both values are in milliseconds,
 * matching the unit used by [Note.hitTime] and [GameEngine.tick].
 *
 * A hit is valid when `delta` falls in `[-early, +late]`,
 * where `delta = currentTime - note.hitTime`.
 * Negative delta → player hit early. Positive delta → player hit late.
 *
 * @param early Milliseconds of tolerance *before* the note's hit time.
 * @param late  Milliseconds of tolerance *after*  the note's hit time.
 */
data class HitWindow(val early: Long, val late: Long) {

  /**
   * Returns true if [delta] (`currentTime - note.hitTime`) falls inside this window.
   */
  fun contains(delta: Long): Boolean = delta >= -early && delta <= late

  companion object {
    /** Convenience factory for a symmetric ±[ms] window. */
    fun symmetric(ms: Long) = HitWindow(early = ms, late = ms)
  }
}

/**
 * Immutable snapshot of the complete game state at the moment a callback fires.
 *
 * Every callback receives a [GameState] as its first argument, giving full
 * context — current time, score, special state, and all live notes — without
 * exposing any mutable internals of [GameEngine].
 *
 * Because this is a true copy (not a reference to live state), it is safe to
 * store across frames (e.g. for replay systems or UI animations). The trade-off
 * is a small allocation per fired callback; for the typical event rate of a
 * rhythm game this is negligible.
 *
 * @param time        Song position (ms) at the tick this snapshot was taken.
 * @param score       Deep copy of [ScoreState] at the moment of the event.
 * @param special     Deep copy of [SpecialState] at the moment of the event.
 * @param notesStates Shallow-immutable copy of the live note list. Each
 *                    [NoteState] inside is the same object the engine holds;
 *                    read its fields but do not mutate them from a callback.
 */
data class GameState(
  val time: Long,
  val score: ScoreState,
  val special: SpecialState,
  val notesStates: List<NoteState>
)

// ─────────────────────────────────────────────────────────────────────────────
// Callback type aliases
//
// Each alias gives a meaningful name to what would otherwise be an anonymous
// function type. The first parameter of every callback is always [GameSnapshot],
// providing full game context at the moment the event fired.
// ─────────────────────────────────────────────────────────────────────────────

/** Invoked when the player successfully hits a note. */
typealias NoteHitCallback = (state: GameState, note: NoteState) -> Unit

/** Invoked when a note's hit window expires without a hit. */
typealias NoteMissCallback = (state: GameState, note: NoteState) -> Unit

/** Invoked when the player's combo streak is reset to zero. */
typealias ComboBreakCallback = (state: GameState) -> Unit

/** Invoked the frame special mode transitions from inactive to active. */
typealias SpecialActivatedCallback = (state: GameState) -> Unit

/** Invoked the frame special mode transitions from active to inactive (energy depleted). */
typealias SpecialEndedCallback = (state: GameState) -> Unit

/** Invoked whenever the player gains special energy. Receives the updated energy value. */
typealias SpecialEnergyGainedCallback = (state: GameState, energy: Int) -> Unit

/** Invoked when a sustain note is held to full completion. */
typealias SustainCompletedCallback = (state: GameState, note: NoteState) -> Unit

/** Invoked when the player releases a sustain note before it finishes **/
typealias SustainBrokenCallback = (state: GameState, note: NoteState) -> Unit

/**
 * Central game-state machine for a rhythm game.
 *
 * [GameEngine] is responsible for one thing only: advancing the authoritative
 * state of every note and the special mechanic, one tick at a time, in response
 * to player input and elapsed time. It deliberately contains no rendering logic,
 * no audio logic, and no scoring formulas — those live in [ScoringSystem].
 *
 * ### State-based contract
 * ```
 * while (songPlaying) {
 *     engine.tick(input, currentTimeMs)
 *     render(engine.notesStates, engine.score, engine.special)
 * }
 * ```
 * The engine never pushes events; consumers pull state after each [tick].
 * Callbacks are an optional side-channel for fire-and-forget reactions
 * (sound effects, particle bursts, UI flashes) that need full game context.
 *
 * ### Registering callbacks
 * ```kotlin
 * engine.onNoteHit = { snapshot, note ->
 *     println("Hit lane ${note.note.lane} — combo is now ${snapshot.score.combo}")
 * }
 * engine.onComboBroke = { snapshot ->
 *     println("Combo broke at ${snapshot.time}ms, score was ${snapshot.score.score}")
 * }
 * ```
 * All callbacks default to null (no-op). Setting a property to null removes
 * the listener. Each event fires at most once per occurrence per tick.
 *
 * Callbacks receive a [GameState] — an immutable deep copy of the full game
 * state at the exact moment the event occurred. This makes it safe to store
 * snapshots across frames without risk of observing stale or mutated data.
 *
 * Callbacks are invoked **synchronously** inside [tick], on the same thread
 * that calls it. Implementations must not call [tick] re-entrantly.
 *
 * ### Input consumption
 * [GameEngine] consumes [PlayerInput] bitmasks directly via [Int.forEachSetBit],
 * avoiding any [Set] or [List] allocation in the hot path. The only allocations
 * per tick are the [GameState] copies created when a callback fires, which
 * are proportional to game events (hits, misses), not to frame rate.
 *
 * ### Tick pipeline (order is load-bearing)
 * 1. [updateSpecial]      — drain / activate special before any note logic.
 * 2. [spawnNotes]         — promote upcoming notes into the live state list.
 * 3. [resolveMissedNotes] — close the window on notes the player didn't hit.
 * 4. [resolveInputs]      — match player presses to live notes.
 * 5. [processSustain]     — advance held sustain notes.
 * 6. [cleanup]            — evict fully resolved notes from all collections.
 *
 * @param notes          Complete, **time-sorted** list of notes in the chart.
 * @param hitWindow      Asymmetric timing tolerance for a valid hit.
 * @param spawnAheadTime How many milliseconds ahead of [Note.hitTime] a note
 *                       enters [notesStates]. Must be large enough for the
 *                       renderer to show notes approaching the hit line.
 *                       Defaults to 0 (notes appear exactly at hit time).
 * @param scoringRules   Strategy object that owns all scoring formulas. The
 *                       engine delegates combo and special bookkeeping to it
 *                       without storing any point values itself.
 */
class GameEngine(
  private val notes: List<Note>,
  private val hitWindow: HitWindow,
  private val spawnAheadTime: Long = 0L,
  private val scoringRules: ScoringRules = ScoringRules()
) {
  // ── Scoring delegate ─────────────────────────────────────────────────────
  // All numeric scoring math lives in ScoringSystem. GameEngine only calls
  // into it; it never reads or stores raw point values itself.
  private val scoring = ScoringSystem(scoringRules)

  // ── Time tracking ────────────────────────────────────────────────────────
  /** Timestamp (ms) received in the most recent [tick] call. */
  private var time: Long = 0

  /** Elapsed milliseconds between the current and previous [tick] calls. */
  private var dt: Long = 0

  // ── Note-spawn cursor ────────────────────────────────────────────────────
  /** Index into [notes] of the next note that has not yet been spawned. */
  private var nextIndex = 0

  // ── Special-phrase bookkeeping ───────────────────────────────────────────
  /**
   * Indices (into [notes]) that are the *last* note of each special phrase.
   * Pre-computed once at construction to avoid repeated scans during gameplay.
   */
  private val specialPhraseEndIndices: Set<Int> = buildSpecialPhraseEndIndices(notes)

  /**
   * Notes whose [Note.hitTime] falls before this timestamp are spawned with
   * [NoteState.specialDisabled] = true, because their phrase was broken.
   * Initialised to [Long.MIN_VALUE] so no note starts disabled.
   */
  private var specialDisabledUntilTime: Long = Long.MIN_VALUE

  // ── Per-lane note queues ─────────────────────────────────────────────────
  /**
   * Lane → [ArrayDeque] of currently live [NoteState] objects for that lane,
   * in chronological order.
   *
   * [ArrayDeque] is used so that [cleanup] can remove spent notes from the
   * front in O(1) via [ArrayDeque.removeFirst], rather than an O(k) linear
   * scan. This is correct because notes within a lane always expire oldest-first.
   */
  private val notesByLane = mutableMapOf<Int, ArrayDeque<NoteState>>()

  // ── Despawn-time cache ───────────────────────────────────────────────────
  /**
   * Maps each live [Note] to its pre-computed despawn timestamp.
   * Avoids recalculating [despawnTime] on every frame for every live note.
   */
  private val despawnTimeCache = mutableMapOf<Note, Long>()

  // ── Scratch buffer for resolveInputs ────────────────────────────────────
  /**
   * Pre-allocated list reused every frame in [resolveInputs] to collect
   * successfully matched notes before processing them. Cleared at the start
   * of each call; never escapes the method, so no allocation occurs in the
   * hot path.
   */
  private val hitNotesBuffer = ArrayList<NoteState>(8)

  // ── Public state ─────────────────────────────────────────────────────────
  /**
   * Live snapshot of every note currently visible to the engine.
   *
   * Exposed as an immutable [List] so external code (renderer, tests) can
   * read state but cannot accidentally mutate it. Only [GameEngine] writes
   * to the backing list.
   */
  private val _notesStates = mutableListOf<NoteState>()
  val notesStates: List<NoteState> get() = _notesStates

  /** Accumulated score and combo counter. Mutated exclusively via [scoring]. */
  val score = ScoreState()

  /** Special-mode energy and activation flags. */
  val special = SpecialState()

  // ════════════════════════════════════════════════════════════════════════
  // Callbacks
  //
  // All callbacks are nullable — null means no listener, which costs nothing
  // beyond a single null-check. Set to null to unregister at any time.
  //
  // Every callback receives a [GameSnapshot] as its first argument: an
  // immutable deep copy of the full game state at the exact moment the event
  // occurred. Event-specific arguments follow as additional parameters.
  //
  // Callbacks are invoked synchronously inside [tick]. Do not call [tick]
  // re-entrantly from within a callback.
  // ════════════════════════════════════════════════════════════════════════

  /** Called once for each note successfully hit this tick. */
  var onNoteHit: NoteHitCallback? = null

  /** Called once for each note newly missed this tick. */
  var onNoteMiss: NoteMissCallback? = null

  /**
   * Called when the player's combo streak is broken.
   * May fire more than once per tick if both a miss and a wrong input occur
   * in the same frame (each independently resets the combo).
   */
  var onComboBroke: ComboBreakCallback? = null

  /** Called the frame special mode is activated. Fires at most once per activation. */
  var onSpecialActivated: SpecialActivatedCallback? = null

  /** Called the frame special mode ends due to energy depletion. */
  var onSpecialEnded: SpecialEndedCallback? = null

  /**
   * Called whenever the player gains special energy (phrase completion).
   * The [energy] parameter reflects the updated value after the gain.
   */
  var onSpecialEnergyGained: SpecialEnergyGainedCallback? = null

  /** Called when a sustain note is held to full completion. */
  var onSustainCompleted: SustainCompletedCallback? = null

  /** Called when the player releases a sustain note before it finishes. */
  var onSustainBroken: SustainBrokenCallback? = null

  // ════════════════════════════════════════════════════════════════════════
  // Public API
  // ════════════════════════════════════════════════════════════════════════

  /**
   * Advances the engine by one simulation step.
   *
   * Must be called exactly once per rendered frame. [currentTime] must be
   * monotonically non-decreasing across calls (e.g. milliseconds since the
   * song started). Passing a timestamp smaller than the previous one produces
   * undefined behaviour.
   *
   * After this returns, [notesStates], [score], and [special] reflect the
   * fully updated state for the current frame and are safe to read. Any
   * registered callbacks will have already been invoked before this returns.
   *
   * TODO: compensa fazer esse método retornar o GameState atual?
   *
   * @param input       Zero-allocation bitmask snapshot of player input for this frame.
   * @param currentTime Monotonically increasing song position in milliseconds.
   */
  fun tick(input: PlayerInput, currentTime: Long) {
    dt = currentTime - time
    time = currentTime

    updateSpecial(input)
    spawnNotes()
    resolveMissedNotes()
    resolveInputs(input)
    processSustain(input)
    cleanup()
  }

  // ════════════════════════════════════════════════════════════════════════
  // Pipeline steps
  // ════════════════════════════════════════════════════════════════════════

  /**
   * Handles special-mode activation and per-frame energy drain.
   *
   * If the player requests activation ([PlayerInput.activateSpecial]) and the
   * [ScoringSystem] considers the current energy sufficient, special mode is
   * enabled and [onSpecialActivated] fires. While active,
   * [ScoringSystem.updateSpecialDrain] depletes energy each tick; when energy
   * runs out [onSpecialEnded] fires. Phrase-tracking flags are suppressed
   * while special is active. When inactive the drain accumulator is reset so
   * partial-drain progress does not carry over between activations.
   */
  private fun updateSpecial(input: PlayerInput) {
    val wasActive = special.active

    if (input.activateSpecial && scoring.canActivateSpecial(special.energy)) {
      special.active = true
    }

    if (special.active) {
      if (!wasActive) onSpecialActivated?.invoke(snapshot())

      val wasEnergyPositive = special.energy > 0
      scoring.updateSpecialDrain(special, dt)
      if (wasEnergyPositive && special.energy <= 0) onSpecialEnded?.invoke(snapshot())

      special.inSequence = false
      special.sequenceBroken = false
    } else {
      special.drainAccumulator = 0.0
    }
  }

  /**
   * Promotes notes from [notes] into [_notesStates] when their spawn time arrives.
   *
   * A note is spawned when `note.hitTime <= time + spawnAheadTime`. The lead
   * time given by [spawnAheadTime] lets the renderer show notes travelling
   * toward the hit line before they actually need to be hit.
   *
   * Each spawned note is also inserted at the back of its lane's [ArrayDeque]
   * (preserving chronological order) and its despawn timestamp is cached.
   */
  private fun spawnNotes() {
    while (nextIndex < notes.size && notes[nextIndex].hitTime <= time + spawnAheadTime) {
      val note = notes[nextIndex]
      val state = NoteState(
        note = note,
        index = nextIndex,
        specialDisabled = note.isSpecial && note.hitTime < specialDisabledUntilTime,
        specialPhraseEnd = nextIndex in specialPhraseEndIndices
      )
      _notesStates.add(state)
      notesByLane.getOrPut(note.lane) { ArrayDeque() }.addLast(state)
      despawnTimeCache[note] = despawnTime(note)
      nextIndex++
    }
  }

  /**
   * Closes the hit window on notes the player failed to hit in time.
   *
   * A note is missed when `time - note.hitTime > hitWindow.late`. Only notes
   * newly missed *this frame* are collected; [onNoteMiss] and [handleNoteMiss]
   * are called exclusively on that subset to prevent repeated side-effects on
   * notes already processed in prior ticks.
   *
   * Returns early if there are no live notes, avoiding unnecessary iteration.
   */
  private fun resolveMissedNotes() {
    if (_notesStates.isEmpty()) return

    var hadMiss = false
    for (state in _notesStates) {
      if (!state.hit && !state.missed && (time - state.note.hitTime) > hitWindow.late) {
        state.missed = true
        hadMiss = true
      }
    }

    if (!hadMiss) return

    resetCombo()
    for (state in _notesStates) {
      if (state.missed && !state.hit) {
        // Guard: only process notes that were just marked missed this frame.
        // Because cleanup has not run yet, previously missed notes are still
        // in the list; re-check the hitTime boundary to skip them.
        if ((time - state.note.hitTime) <= hitWindow.late + dt) {
          onNoteMiss?.invoke(snapshot(), state)
          handleNoteMiss(state)
        }
      }
    }
  }

  /**
   * Matches player fret presses to the closest eligible note in each lane.
   *
   * Iterates [PlayerInput.justPressedFrets] via [Int.forEachSetBit] — a
   * zero-allocation bit-scan loop that visits only the set bits. A press
   * with no matching note is flagged as wrong input. After all lanes are
   * processed, valid hits are registered and any wrong input resets the combo.
   *
   * Uses [hitNotesBuffer] as a pre-allocated scratch buffer to avoid creating
   * a new list per frame.
   *
   * **Chord behaviour**: if the player presses multiple frets and only a subset
   * have matching notes, the matched lanes register hits while the unmatched
   * lanes cause a combo break. Adjust if a stricter all-or-nothing rule is needed.
   */
  private fun resolveInputs(input: PlayerInput) {
    if (input.justPressedFrets == 0) return

    hitNotesBuffer.clear()
    var hasWrongInput = false

    input.justPressedFrets.forEachSetBit { lane ->
      val noteState = findClosestPendingNoteForLane(lane)
      if (noteState == null) {
        hasWrongInput = true
      } else {
        noteState.hit = true
        noteState.holding = noteState.note.duration > 0
        hitNotesBuffer.add(noteState)
      }
    }

    for (i in hitNotesBuffer.indices) {
      val noteState = hitNotesBuffer[i]
      scoring.registerHit(score, special.active)
      onNoteHit?.invoke(snapshot(), noteState)
      handleNoteHit(noteState)
    }

    if (hasWrongInput) resetCombo()
  }

  /**
   * Returns the unhit, unmatched note in [lane] closest to [time] and still
   * within [hitWindow].
   *
   * Searches only [notesByLane] — O(k) where k is the number of live notes
   * in that lane, typically 1–3. Tie-breaking prefers the smallest timing
   * delta, then the earliest hit time.
   *
   * Returns null if no eligible note exists for the given lane.
   */
  private fun findClosestPendingNoteForLane(lane: Int): NoteState? {
    val candidates = notesByLane[lane] ?: return null
    return candidates
      .asSequence()
      .filter { !it.hit && !it.missed && hitWindow.contains(time - it.note.hitTime) }
      .minWithOrNull(compareBy({ Math.abs(time - it.note.hitTime) }, { it.note.hitTime }))
  }

  /**
   * Updates special-phrase progress when a note is successfully hit.
   *
   * Phrase tracking is skipped entirely while special mode is active.
   *
   * - **Special note, not disabled**: starts or continues the current phrase.
   *   If this note is the phrase-end and the sequence is unbroken, energy is
   *   gained and [onSpecialEnergyGained] fires.
   * - **Special note, disabled**: no-op — disabled notes grant nothing.
   * - **Regular note**: if an unbroken special sequence was in progress,
   *   energy is gained and [onSpecialEnergyGained] fires; the sequence ends.
   */
  private fun handleNoteHit(noteState: NoteState) {
    if (special.active) return

    when {
      noteState.note.isSpecial && !noteState.specialDisabled -> {
        if (!special.inSequence) {
          special.inSequence = true
          special.sequenceBroken = false
        }
        if (noteState.specialPhraseEnd && !special.sequenceBroken) {
          special.energy = scoring.gainSpecialEnergy(special.energy)
          onSpecialEnergyGained?.invoke(snapshot(), special.energy)
          special.inSequence = false
          special.sequenceBroken = false
        }
      }

      !noteState.note.isSpecial -> {
        if (special.inSequence && !special.sequenceBroken) {
          special.energy = scoring.gainSpecialEnergy(special.energy)
          onSpecialEnergyGained?.invoke(snapshot(), special.energy)
        }
        special.inSequence = false
        special.sequenceBroken = false
      }
      // isSpecial && specialDisabled → no-op
    }
  }

  /**
   * Handles the consequences of missing a special note.
   *
   * Missing a special note breaks the current phrase and disables all remaining
   * notes in the same phrase. The phrase boundary is found by walking forward
   * from [NoteState.index] in [notes] until the first non-special note —
   * O(phrase length), not O(total notes).
   *
   * Non-special notes and already-disabled notes are ignored.
   */
  private fun handleNoteMiss(noteState: NoteState) {
    if (!noteState.note.isSpecial || noteState.specialDisabled) return

    special.sequenceBroken = true

    var sequenceEndTime = Long.MAX_VALUE
    for (i in noteState.index + 1 until notes.size) {
      if (!notes[i].isSpecial) {
        sequenceEndTime = notes[i].hitTime
        break
      }
    }

    specialDisabledUntilTime = sequenceEndTime

    _notesStates.forEach { state ->
      if (!state.hit && !state.missed &&
        state.note.isSpecial &&
        state.note.hitTime >= noteState.note.hitTime &&
        state.note.hitTime < sequenceEndTime
      ) {
        state.specialDisabled = true
      }
    }
  }

  /**
   * Scores and advances the progress of all currently held sustain notes.
   *
   * Sustain state is updated by checking [PlayerInput.pressedFrets] and
   * [PlayerInput.justReleasedFrets] bitmasks directly via [Int.isLaneBitSet],
   * avoiding any collection lookup. For each note in a [NoteState.holding] state:
   * - **Complete**: progress reached full duration — hold ends cleanly and
   *   [onSustainCompleted] fires.
   * - **Released**: the player released the lane this frame or is no longer
   *   pressing it — hold ends, [NoteState.sustainBroken] is set, and
   *   [onSustainBroken] fires.
   * - **Ongoing**: progress advances by `min(dt, remaining)` milliseconds and
   *   the delta is forwarded to [ScoringSystem.registerSustain].
   */
  private fun processSustain(input: PlayerInput) {
    _notesStates.forEach { state ->
      if (!state.holding) return@forEach

      val remaining = state.note.duration - state.sustainProgress
      if (remaining <= 0.0) {
        state.holding = false
        onSustainCompleted?.invoke(snapshot(), state)
        return@forEach
      }

      // Bitmask check: test the single bit for this note's lane.
      val laneBit = 1 shl state.note.lane
      val released = (input.justReleasedFrets and laneBit) != 0
      val stillHeld = (input.pressedFrets and laneBit) != 0

      if (released || !stillHeld) {
        state.holding = false
        if (state.sustainProgress < state.note.duration) {
          state.sustainBroken = true
          onSustainBroken?.invoke(snapshot(), state)
        }
        return@forEach
      }

      val delta = minOf(dt.toDouble(), remaining)
      scoring.registerSustain(score, delta, special.active)
      state.sustainProgress += delta
      if (state.sustainProgress >= state.note.duration) {
        state.holding = false
        onSustainCompleted?.invoke(snapshot(), state)
      }
    }
  }

  /**
   * Evicts fully resolved notes from [_notesStates], [notesByLane], and
   * [despawnTimeCache] once their post-resolution display window has elapsed.
   *
   * A note is eligible for eviction when it is resolved (hit or missed) *and*
   * `time > despawnTime`. The extra window beyond resolution lets the renderer
   * finish any hit/miss animation before the state disappears.
   *
   * Because notes within a lane are always resolved oldest-first, spent notes
   * are always at the front of their lane's [ArrayDeque], making
   * [ArrayDeque.removeFirst] O(1).
   */
  private fun cleanup() {
    val iterator = _notesStates.iterator()
    while (iterator.hasNext()) {
      val state = iterator.next()
      val despawn = despawnTimeCache[state.note] ?: despawnTime(state.note)
      if ((state.hit || state.missed) && time > despawn) {
        iterator.remove()
        notesByLane[state.note.lane]?.let { deque ->
          if (deque.firstOrNull() === state) deque.removeFirst()
        }
        despawnTimeCache.remove(state.note)
      }
    }
  }

  // ════════════════════════════════════════════════════════════════════════
  // Helpers
  // ════════════════════════════════════════════════════════════════════════

  /**
   * Builds an immutable [GameState] from the current engine state.
   *
   * Called immediately before each callback invocation so the snapshot
   * captures the state *after* the event that triggered it (e.g. after a
   * hit is registered, the snapshot already reflects the updated combo).
   * [ScoreState] and [SpecialState] are copied via their `copy()` functions;
   * [notesStates] is wrapped in an unmodifiable list view — its [NoteState]
   * elements are the live objects, so their fields should be read but not
   * mutated from within a callback.
   */
  private fun snapshot() = GameState(
    time = time,
    score = score.copy(),
    special = special.copy(),
    notesStates = _notesStates.toList()
  )

  /**
   * Resets the combo via [ScoringSystem] and fires [onComboBroke].
   *
   * Centralised so every combo-break site (missed notes, wrong inputs) goes
   * through one place, guaranteeing the callback always accompanies the reset.
   */
  private fun resetCombo() {
    scoring.resetCombo(score)
    onComboBroke?.invoke(snapshot())
  }

  /**
   * Computes the timestamp after which a resolved note can be safely removed.
   *
   * The note is kept alive long enough for the renderer to display a hit or
   * miss animation: at minimum 1 000 ms (for tap notes), or the full sustain
   * [Note.duration] if longer, plus an additional 1 000 ms of post-event
   * display time.
   */
  private fun despawnTime(note: Note): Long {
    val visibleLifetime = maxOf(1_000L, note.duration)
    return note.hitTime + visibleLifetime + 1_000L
  }

  /**
   * Pre-computes the set of [notes] indices that are the last note of each
   * special phrase.
   *
   * A special note at index `i` is a phrase-end when `notes[i + 1]` either
   * does not exist or is not a special note. Called once in the constructor;
   * the result is immutable for the lifetime of the engine.
   */
  private fun buildSpecialPhraseEndIndices(notes: List<Note>): Set<Int> {
    if (notes.isEmpty()) return emptySet()
    return buildSet {
      notes.forEachIndexed { index, note ->
        if (!note.isSpecial) return@forEachIndexed
        val next = notes.getOrNull(index + 1)
        if (next == null || !next.isSpecial) add(index)
      }
    }
  }
}