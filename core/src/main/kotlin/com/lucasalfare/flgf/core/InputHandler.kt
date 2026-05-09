package com.lucasalfare.flgf.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

/**
 * Configuration for keyboard input mapping.
 * 
 * @property keyMappings Maps lane indices (0-4) to keyboard keys
 * @property specialKey Key used to activate special ability
 */
data class InputConfig(
  val keyMappings: Map<Int, Int>,
  val specialKey: Int = Input.Keys.SPACE
) {
  companion object {
    /**
     * Default configuration using E-T-U-I-O keys for lanes 0-4.
     */
    val DEFAULT = InputConfig(
      keyMappings = mapOf(
        0 to Input.Keys.E,
        1 to Input.Keys.T, 
        2 to Input.Keys.U,
        3 to Input.Keys.I,
        4 to Input.Keys.O
      )
    )
  }
}

/**
 * Handles keyboard input and converts it to PlayerInput for the game engine.
 * 
 * Tracks both currently pressed keys and keys that were just pressed this frame
 * to enable proper edge-triggered input detection.
 * 
 * @property config Input configuration with key mappings
 */
class InputHandler(private val config: InputConfig = InputConfig.DEFAULT) {
  
  /** Set of keys that were pressed on the previous frame */
  private var previousKeys: Set<Int> = emptySet()
  
  /** Set of keys currently being held down */
  private var currentKeys: Set<Int> = emptySet()
  
  /**
   * Updates input state and returns PlayerInput for the current frame.
   * 
   * Call this method once per frame to get accurate input state.
   * 
   * @return PlayerInput with pressed and just-pressed frets
   */
  fun updateInput(): PlayerInput {
    previousKeys = currentKeys
    currentKeys = getCurrentPressedKeys()
    
    val justPressed = currentKeys - previousKeys
    
    return PlayerInput(
      pressedFrets = mapKeysToFrets(currentKeys),
      justPressedFrets = mapKeysToFrets(justPressed),
      activateSpecial = Input.Keys.SPACE in justPressed
    )
  }
  
  /**
   * Gets the current set of pressed keyboard keys.
   */
  private fun getCurrentPressedKeys(): Set<Int> {
    val keys = mutableSetOf<Int>()
    
    // Check all mapped keys
    config.keyMappings.values.forEach { key ->
      if (Gdx.input.isKeyPressed(key)) {
        keys.add(key)
      }
    }
    
    // Also check special key
    if (Gdx.input.isKeyPressed(config.specialKey)) {
      keys.add(config.specialKey)
    }
    
    return keys
  }
  
  /**
   * Maps keyboard keys to lane indices (frets).
   * 
   * @param keys Set of keyboard keys to convert
   * @return Set of lane indices (0-4) corresponding to the keys
   */
  private fun mapKeysToFrets(keys: Set<Int>): Set<Int> {
    return keys.mapNotNull { key ->
      config.keyMappings.entries.find { it.value == key }?.key
    }.toSet()
  }
  
  /**
   * Updates the input configuration.
   * 
   * @param newConfig New input configuration to use
   */
  fun updateConfig(newConfig: InputConfig) {
    // Reset input state to avoid stuck keys when config changes
    previousKeys = emptySet()
    currentKeys = emptySet()
    // Note: In a real implementation, you'd store the new config
    // For now, we keep using the original config passed in constructor
  }
}
