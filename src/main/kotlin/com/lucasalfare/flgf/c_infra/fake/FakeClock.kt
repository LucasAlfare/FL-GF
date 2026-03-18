package com.lucasalfare.flgf.c_infra.fake

import com.lucasalfare.flgf.a_domain.GameClock

class FakeClock(var time: Double = 0.0) : GameClock {
  override fun currentTime(): Double = time
}