plugins {
  kotlin("jvm") version "2.3.0"
  application
}

dependencies {
  implementation(project(":core"))

  implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.14.0")
  implementation("com.badlogicgames.gdx:gdx-platform:1.14.0:natives-desktop")
}

application {
  mainClass.set("com.lucasalfare.flgf.desktop.DesktopLauncherKt")
}