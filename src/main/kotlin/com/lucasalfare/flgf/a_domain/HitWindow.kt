package com.lucasalfare.flgf.a_domain

/**
 * Defines timing thresholds for hit accuracy.
 */
data class HitWindow(
  val perfect: Double,
  val good: Double
)