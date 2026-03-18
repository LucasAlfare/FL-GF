package com.lucasalfare.flgf.a_domain

/**
 * Responsible for evaluating hit accuracy.
 */
class HitJudge {
  fun judge(note: Note, inputTime: Double, window: HitWindow): Judgement {
    val delta = kotlin.math.abs(note.time - inputTime)

    return when {
      delta <= window.perfect -> Judgement.PERFECT
      delta <= window.good -> Judgement.GOOD
      else -> Judgement.MISS
    }
  }
}