package com.lucasalfare.flgf.core.view.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.lucasalfare.flgf.core.game.NoteState
import com.lucasalfare.flgf.core.view.PlayfieldLayout
import com.lucasalfare.flgf.core.view.PlayfieldPerspective
import kotlin.math.max

private val specialActiveColor = Color(0.1f, 0.92f, 1f, 1f)
private val brokenColor = Color(0.55f, 0.55f, 0.58f, 0.25f)

private enum class NoteRenderState {
  NORMAL,
  SPECIAL,
  SPECIAL_ACTIVE
}

private fun NoteState.renderState(specialActive: Boolean): NoteRenderState = when {
  specialActive -> NoteRenderState.SPECIAL_ACTIVE
  note.isSpecial && !specialDisabled -> NoteRenderState.SPECIAL
  else -> NoteRenderState.NORMAL
}

class NoteRenderer(
  private val layout: PlayfieldLayout,
  private val perspective: PlayfieldPerspective
) {

  private val colorInactive = Color(0.55f, 0.55f, 0.58f, 1f)

  private val polygonBatch = PolygonSpriteBatch()
  private val ghostNoteTexture = Texture(Gdx.files.internal("ghost_note.png"))

  fun draw(
    shapeRenderer: ShapeRenderer,
    noteStates: List<NoteState>,
    songTime: Long,
    specialActive: Boolean
  ) {
    noteStates.forEach { state ->
      val renderState = state.renderState(specialActive)
      val headY = yForTime(state.note.hitTime, songTime)
      val bodyTopY = yForTime(state.note.hitTime + state.note.duration, songTime)
      val visibleBottom = layout.hitLineY - layout.noteHeight
      val visibleTop = layout.top
      val noteTopY = max(headY + layout.noteHeight, bodyTopY)

      if (noteTopY < visibleBottom || headY > visibleTop) {
        return@forEach
      }

      drawSustainBody(shapeRenderer, state, headY, bodyTopY, renderState)
      drawHead(shapeRenderer, state, headY, renderState)
    }
  }

  fun draw2(
    shapeRenderer: ShapeRenderer,
    noteStates: List<NoteState>,
    songTime: Long,
    specialActive: Boolean
  ) {

    polygonBatch.projectionMatrix = shapeRenderer.projectionMatrix

    polygonBatch.begin()

    noteStates.forEach { state ->

      val renderState = state.renderState(specialActive)

      val headY = yForTime(state.note.hitTime, songTime)
      val bodyTopY = yForTime(state.note.hitTime + state.note.duration, songTime)

      val visibleBottom = layout.hitLineY - layout.noteHeight
      val visibleTop = layout.top
      val noteTopY = max(headY + layout.noteHeight, bodyTopY)

      if (noteTopY < visibleBottom || headY > visibleTop) {
        return@forEach
      }

      drawHeadTexture(state, headY, renderState)
    }

    polygonBatch.end()
  }

  private fun yForTime(noteTime: Long, songTime: Long): Float {
    val distanceMs = noteTime - songTime
    return layout.hitLineY + distanceMs * layout.noteSpeedPerMs
  }

  private fun drawHead(
    shapeRenderer: ShapeRenderer,
    state: NoteState,
    headY: Float,
    renderState: NoteRenderState
  ) {
    if (state.hit) return

    val laneX = layout.xForLane(state.note.lane)
    val laneWidth = layout.laneWidth - layout.laneGap

    shapeRenderer.color = when {
      state.missed || state.sustainBroken -> colorInactive
      else -> colorForState(state, renderState)
    }

    if (renderState == NoteRenderState.SPECIAL) {
      drawSpecialHead(shapeRenderer, laneX, headY, laneWidth)
      return
    }

    perspective.drawProjectedRect(
      shapeRenderer,
      laneX,
      headY,
      laneWidth,
      layout.noteHeight
    )
  }

  private fun drawHeadTexture(
    state: NoteState,
    headY: Float,
    renderState: NoteRenderState
  ) {

    if (state.hit) return

    val laneX = layout.xForLane(state.note.lane)
    val laneWidth = layout.laneWidth - layout.laneGap

    val color = when {
      state.missed || state.sustainBroken -> colorInactive
      else -> colorForState(state, renderState)
    }

    perspective.drawProjectedTexture(
      polygonBatch,
      ghostNoteTexture,
      laneX,
      headY,
      laneWidth,
      layout.noteHeight,
      color
    )
  }

  private fun drawSustainBody(
    shapeRenderer: ShapeRenderer,
    state: NoteState,
    headY: Float,
    bodyTopY: Float,
    renderState: NoteRenderState
  ) {

    if (state.note.duration <= 0L) return

    if (
      state.hit &&
      !state.sustainBroken &&
      state.sustainProgress >= state.note.duration
    ) return

    val rawBodyStartY = headY + layout.noteHeight

    val bodyStartY = if (
      state.hit &&
      !state.sustainBroken &&
      state.holding
    ) {
      max(rawBodyStartY, layout.hitLineY)
    } else {
      rawBodyStartY
    }

    val bodyHeight = bodyTopY - bodyStartY

    if (bodyHeight <= 0f) return

    val laneX = layout.xForLane(state.note.lane)
    val laneWidth = layout.laneWidth - layout.laneGap

    val sustainWidth = laneWidth * layout.sustainBodyWidthRatio
    val sustainX = laneX + (laneWidth - sustainWidth) / 2f

    shapeRenderer.color = when {

      state.sustainBroken || state.missed -> brokenColor

      state.hit -> colorForState(state, renderState)
        .cpy()
        .apply {
          a = if (renderState == NoteRenderState.SPECIAL_ACTIVE) {
            0.58f
          } else {
            0.55f
          }
        }

      else -> colorForState(state, renderState)
        .cpy()
        .apply {
          a = if (
            renderState == NoteRenderState.SPECIAL_ACTIVE ||
            renderState == NoteRenderState.SPECIAL
          ) {
            0.45f
          } else {
            0.5f
          }
        }
    }

    perspective.drawProjectedRect(
      shapeRenderer,
      sustainX,
      bodyStartY,
      sustainWidth,
      bodyHeight
    )
  }

  private fun colorForState(
    state: NoteState,
    renderState: NoteRenderState
  ): Color = when (renderState) {

    NoteRenderState.SPECIAL_ACTIVE -> specialActiveColor

    NoteRenderState.SPECIAL ->
      LanePalette.colorForLane(state.note.lane)

    NoteRenderState.NORMAL ->
      LanePalette.colorForLane(state.note.lane)
  }

  private fun drawSpecialHead(
    shapeRenderer: ShapeRenderer,
    laneX: Float,
    headY: Float,
    laneWidth: Float
  ) {

    val topY = headY + layout.noteHeight
    val centerX = laneX + laneWidth / 2f

    perspective.drawProjectedTriangle(
      shapeRenderer,
      laneX,
      topY,
      laneX + laneWidth,
      topY,
      centerX,
      headY
    )
  }

  fun dispose() {
    polygonBatch.dispose()
    ghostNoteTexture.dispose()
  }
}