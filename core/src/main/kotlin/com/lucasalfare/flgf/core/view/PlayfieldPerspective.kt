package com.lucasalfare.flgf.core.view

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

data class ProjectedPoint(val x: Float, val y: Float)

class PlayfieldPerspective(
  private val layout: PlayfieldLayout,
  private val config: PlayfieldPerspectiveConfig = PlayfieldPerspectiveConfig()
) {
  private val centerX = layout.left + layout.width / 2f
  private val centerY = layout.bottom + layout.height / 2f
  private val cameraDistance: Float
    get() = maxOf(layout.width, layout.height) * config.cameraDistanceMultiplier

  private data class Vec3(var x: Float, var y: Float, var z: Float)

  private val degreesToRadians = Math.PI / 180.0
  private val sinX: Float
    get() = kotlin.math.sin(config.rotationXDegrees * degreesToRadians).toFloat()
  private val cosX: Float
    get() = kotlin.math.cos(config.rotationXDegrees * degreesToRadians).toFloat()
  private val sinY: Float
    get() = kotlin.math.sin(config.rotationYDegrees * degreesToRadians).toFloat()
  private val cosY: Float
    get() = kotlin.math.cos(config.rotationYDegrees * degreesToRadians).toFloat()
  private val sinZ: Float
    get() = kotlin.math.sin(config.rotationZDegrees * degreesToRadians).toFloat()
  private val cosZ: Float
    get() = kotlin.math.cos(config.rotationZDegrees * degreesToRadians).toFloat()

  fun project(x: Float, y: Float): ProjectedPoint {
    if (!config.enabled) {
      return ProjectedPoint(x, y)
    }

    val local = Vec3(x - centerX, y - centerY, 0f)
    rotateX(local)
    rotateY(local)
    rotateZ(local)

    val perspective = cameraDistance / (cameraDistance + local.z).coerceAtLeast(cameraDistance * 0.1f)

    return ProjectedPoint(
      x = centerX + local.x * perspective,
      y = centerY + local.y * perspective
    )
  }

  private fun rotateX(v: Vec3) {
    val y = v.y * cosX - v.z * sinX
    val z = v.y * sinX + v.z * cosX
    v.y = y
    v.z = z
  }

  private fun rotateY(v: Vec3) {
    val x = v.x * cosY + v.z * sinY
    val z = -v.x * sinY + v.z * cosY
    v.x = x
    v.z = z
  }

  private fun rotateZ(v: Vec3) {
    val x = v.x * cosZ - v.y * sinZ
    val y = v.x * sinZ + v.y * cosZ
    v.x = x
    v.y = y
  }

  fun drawProjectedRect(
    shapeRenderer: ShapeRenderer,
    x: Float,
    y: Float,
    width: Float,
    height: Float
  ) {
    if (!config.enabled) {
      shapeRenderer.rect(x, y, width, height)
      return
    }

    val bottomLeft = project(x, y)
    val bottomRight = project(x + width, y)
    val topRight = project(x + width, y + height)
    val topLeft = project(x, y + height)
    drawQuad(shapeRenderer, bottomLeft, bottomRight, topRight, topLeft)
  }

  fun drawProjectedTriangle(
    shapeRenderer: ShapeRenderer,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    x3: Float,
    y3: Float
  ) {
    if (!config.enabled) {
      shapeRenderer.triangle(x1, y1, x2, y2, x3, y3)
      return
    }

    val p1 = project(x1, y1)
    val p2 = project(x2, y2)
    val p3 = project(x3, y3)

    shapeRenderer.triangle(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
  }

  private fun drawQuad(
    shapeRenderer: ShapeRenderer,
    bottomLeft: ProjectedPoint,
    bottomRight: ProjectedPoint,
    topRight: ProjectedPoint,
    topLeft: ProjectedPoint
  ) {
    shapeRenderer.triangle(
      bottomLeft.x, bottomLeft.y,
      bottomRight.x, bottomRight.y,
      topRight.x, topRight.y
    )
    shapeRenderer.triangle(
      bottomLeft.x, bottomLeft.y,
      topRight.x, topRight.y,
      topLeft.x, topLeft.y
    )
  }

  fun drawProjectedTexture(
    batch: PolygonSpriteBatch,
    texture: Texture,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    color: Color
  ) {

    val bottomLeft = project(x, y)
    val bottomRight = project(x + width, y)
    val topRight = project(x + width, y + height)
    val topLeft = project(x, y + height)

    val c = color.toFloatBits()

    val vertices = floatArrayOf(

      // bottomLeft
      bottomLeft.x,
      bottomLeft.y,
      c,
      0f,
      1f,

      // bottomRight
      bottomRight.x,
      bottomRight.y,
      c,
      1f,
      1f,

      // topRight
      topRight.x,
      topRight.y,
      c,
      1f,
      0f,

      // topLeft
      topLeft.x,
      topLeft.y,
      c,
      0f,
      0f
    )

    val triangles = shortArrayOf(
      0, 1, 2,
      2, 3, 0
    )

    batch.draw(
      texture,
      vertices,
      0,
      vertices.size,
      triangles,
      0,
      triangles.size
    )
  }
}
