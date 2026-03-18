package com.lucasalfare.flgf.a_domain

/**
 * Responsible for evaluating hit accuracy.
 */
class HitJudge {
  fun judge(note: Note, currentTime: Double, window: HitWindow): Judgement {
    val diff = kotlin.math.abs(currentTime - note.time)

    return when {
      diff <= window.perfect -> Judgement.PERFECT
      diff <= window.good -> Judgement.GOOD
      else -> Judgement.MISS
    }
  }
}