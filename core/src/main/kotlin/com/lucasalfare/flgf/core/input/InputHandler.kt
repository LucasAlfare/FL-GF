package com.lucasalfare.flgf.core.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.lucasalfare.flgf.core.game.PlayerInput

/**
 * Polls LibGDX keyboard state each frame and produces a zero-allocation
 * [PlayerInput] bitmask value.
 *
 * ### Zero-allocation guarantee
 * - No [Set], [List], or any other heap object is created per frame.
 * - [PlayerInput] is a `@JvmInline value class` backed by a single `Long`;
 *   on the JVM it is unboxed in most call sites, meaning [update] effectively
 *   returns a primitive.
 * - All fret state is derived from integer bitmask operations (AND, OR, NOT,
 *   shift) on two `Int` fields ([previousMask], computed `pressed`).
 *
 * ### Bitmask layout
 * Each lane i corresponds to bit i of the mask (lane 0 → bit 0, lane 4 → bit 4).
 * The engine supports up to 15 lanes; for a standard 5-fret guitar only bits 0–4
 * are used.
 *
 * ### Edge detection
 * ```
 * justPressed  = pressed  AND NOT previousMask   (bits that flipped 0→1)
 * justReleased = previousMask AND NOT pressed     (bits that flipped 1→0)
 * ```
 * This is computed in two bitwise operations with no branching.
 */
object InputHandler {

  /**
   * Keyboard keys mapped to fret lanes, in lane order.
   * Index i in this array corresponds to lane i in the bitmask.
   * Modify to remap controls without changing any other logic.
   */
  private val laneKeys = intArrayOf(
    Input.Keys.E,
    Input.Keys.T,
    Input.Keys.U,
    Input.Keys.I,
    Input.Keys.O
  )

  /**
   * Bitmask of keys that were pressed at the end of the previous [update] call.
   * Used to derive edge-triggered justPressed / justReleased masks each frame.
   */
  private var previousMask: Int = 0

  /**
   * Polls the keyboard, computes edge-triggered deltas via bitmask arithmetic,
   * and returns the current frame's input as a [PlayerInput].
   *
   * Must be called exactly once per rendered frame, before [GameEngine.tick].
   *
   * Complexity: O(lanes) — one [Gdx.input.isKeyPressed] call per lane,
   * plus O(1) bitmask arithmetic. No allocation of any kind.
   */
  fun update(): PlayerInput {
    // ── Build pressed mask ───────────────────────────────────────────────
    // Poll each lane key and set the corresponding bit if held.
    var pressed = 0
    for (i in laneKeys.indices) {
      if (Gdx.input.isKeyPressed(laneKeys[i])) pressed = pressed or (1 shl i)
    }

    // ── Derive edge masks ────────────────────────────────────────────────
    // justPressed:  bits that are set now but were clear last frame (0 → 1)
    // justReleased: bits that were set last frame but are clear now  (1 → 0)
    val justPressed  = pressed and previousMask.inv()
    val justReleased = previousMask and pressed.inv()

    previousMask = pressed

    // ── Special activation ───────────────────────────────────────────────
    // isKeyJustPressed is already edge-triggered; no extra bookkeeping needed.
    val activateSpecial = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)

    return PlayerInput.of(
      pressed        = pressed,
      justPressed    = justPressed,
      justReleased   = justReleased,
      activateSpecial = activateSpecial
    )
  }

  /**
   * Resets internal state. Call when the game is paused, a menu is opened,
   * or any context switch that should clear held-key memory, to prevent
   * ghost justReleased events on the next [update] call.
   */
  fun reset() {
    previousMask = 0
  }
}