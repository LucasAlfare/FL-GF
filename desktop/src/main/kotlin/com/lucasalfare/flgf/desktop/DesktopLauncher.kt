package com.lucasalfare.flgf.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.lucasalfare.flgf.core.GuitarFlashGame

fun main() {
  val config = Lwjgl3ApplicationConfiguration()
  config.setTitle("Meu Jogo")
  config.setWindowedMode(800, 600)
  config.setForegroundFPS(30)

  Lwjgl3Application(GuitarFlashGame(), config)
}