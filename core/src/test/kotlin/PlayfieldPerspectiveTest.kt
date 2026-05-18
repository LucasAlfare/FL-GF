import com.lucasalfare.flgf.core.GameConfig
import com.lucasalfare.flgf.core.PlayfieldLayout
import com.lucasalfare.flgf.core.PlayfieldPerspective
import com.lucasalfare.flgf.core.PlayfieldPerspectiveConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayfieldPerspectiveTest {

  @Test
  fun `should make the top of the playfield narrower with x rotation`() {
    val layout = PlayfieldLayout(GameConfig())
    val perspective = PlayfieldPerspective(
      layout,
      PlayfieldPerspectiveConfig(rotationXDegrees = 30f)
    )

    val bottomLeft = perspective.project(layout.left, layout.bottom)
    val bottomRight = perspective.project(layout.left + layout.laneWidth, layout.bottom)
    val topLeft = perspective.project(layout.left, layout.top)
    val topRight = perspective.project(layout.left + layout.laneWidth, layout.top)

    val bottomLaneWidth = bottomRight.x - bottomLeft.x
    val topLaneWidth = topRight.x - topLeft.x

    assertTrue(topLaneWidth < bottomLaneWidth)
    assertTrue(topLeft.x > bottomLeft.x)
  }

  @Test
  fun `should become identity when perspective is disabled`() {
    val layout = PlayfieldLayout(GameConfig())
    val perspective = PlayfieldPerspective(
      layout,
      PlayfieldPerspectiveConfig(
        enabled = false,
        rotationXDegrees = 30f,
        rotationYDegrees = 10f,
        rotationZDegrees = 15f
      )
    )

    val projected = perspective.project(layout.left + 0.123f, layout.bottom + 0.456f)

    assertEquals(layout.left + 0.123f, projected.x, 0.0001f)
    assertEquals(layout.bottom + 0.456f, projected.y, 0.0001f)
  }

  @Test
  fun `should skew the lane deck with y rotation`() {
    val layout = PlayfieldLayout(GameConfig())
    val perspective = PlayfieldPerspective(
      layout,
      PlayfieldPerspectiveConfig(rotationYDegrees = -20f)
    )

    val leftPoint = perspective.project(layout.left, layout.centerY())
    val rightPoint = perspective.project(layout.left + layout.width, layout.centerY())

    assertTrue(leftPoint.x < layout.left)
    assertTrue(rightPoint.x < layout.left + layout.width)
    assertTrue((rightPoint.x - leftPoint.x) < layout.width)
  }

  @Test
  fun `should rotate the deck in screen space with z rotation`() {
    val layout = PlayfieldLayout(GameConfig())
    val perspective = PlayfieldPerspective(
      layout,
      PlayfieldPerspectiveConfig(rotationZDegrees = 90f)
    )

    val centerX = layout.left + layout.width / 2f
    val centerY = layout.bottom + layout.height / 2f
    val point = perspective.project(centerX + 0.1f, centerY)

    assertEquals(centerX, point.x, 0.0001f)
    assertTrue(point.y > centerY)
  }

  @Test
  fun `should react to config changes after construction`() {
    val layout = PlayfieldLayout(GameConfig())
    val config = PlayfieldPerspectiveConfig(
      enabled = true,
      rotationXDegrees = 25f,
      rotationYDegrees = 0f,
      rotationZDegrees = 0f
    )
    val perspective = PlayfieldPerspective(layout, config)

    val projectedWithPerspective = perspective.project(layout.left, layout.top)
    config.enabled = false
    val projectedWithoutPerspective = perspective.project(layout.left, layout.top)

    assertTrue(projectedWithPerspective.x != projectedWithoutPerspective.x)
    assertEquals(layout.left, projectedWithoutPerspective.x, 0.0001f)
    assertEquals(layout.top, projectedWithoutPerspective.y, 0.0001f)
  }
}

private fun PlayfieldLayout.centerY(): Float = bottom + height / 2f
