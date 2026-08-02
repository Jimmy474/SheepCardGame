package com.jimmy.sheepcardgame.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val PlayingCardsIcon: ImageVector
  get() {
    if (_playing_cards != null) {
      return _playing_cards!!
    }
    _playing_cards =
      ImageVector.Builder(
          name = "playing_cards",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero,
          ) {
            moveTo(15.38f, 14.13f)
            lineToRelative(0.78f, -2.8f)
            quadToRelative(0.1f, -0.3f, -0.01f, -0.6f)
            quadToRelative(-0.11f, -0.3f, -0.39f, -0.5f)
            lineTo(13.38f, 8.6f)
            quadTo(13.18f, 8.45f, 12.94f, 8.54f)
            reflectiveQuadTo(12.63f, 8.88f)
            lineToRelative(-0.77f, 2.8f)
            quadToRelative(-0.1f, 0.3f, 0.01f, 0.6f)
            quadToRelative(0.11f, 0.3f, 0.39f, 0.5f)
            lineToRelative(2.38f, 1.63f)
            quadToRelative(0.2f, 0.15f, 0.44f, 0.06f)
            reflectiveQuadToRelative(0.31f, -0.34f)
            close()
            moveTo(4f, 18.83f)
            lineTo(3.18f, 18.43f)
            quadTo(2.4f, 18.1f, 2.13f, 17.31f)
            reflectiveQuadTo(2.2f, 15.75f)
            lineTo(4f, 11.85f)
            verticalLineToRelative(6.98f)
            close()
            moveTo(8f, 21f)
            quadTo(7.18f, 21f, 6.59f, 20.4f)
            reflectiveQuadTo(6f, 18.98f)
            verticalLineTo(13f)
            lineToRelative(2.68f, 7.35f)
            quadToRelative(0.07f, 0.17f, 0.13f, 0.34f)
            reflectiveQuadTo(8.98f, 21f)
            horizontalLineTo(8f)
            close()
            moveToRelative(5.15f, -0.13f)
            quadTo(12.38f, 21.15f, 11.6f, 20.8f)
            reflectiveQuadTo(10.55f, 19.68f)
            lineTo(6.13f, 7.45f)
            quadTo(5.85f, 6.68f, 6.2f, 5.91f)
            reflectiveQuadTo(7.33f, 4.88f)
            lineTo(14.85f, 2.13f)
            quadTo(15.63f, 1.85f, 16.39f, 2.2f)
            reflectiveQuadToRelative(1.04f, 1.13f)
            lineToRelative(4.45f, 12.23f)
            quadToRelative(0.28f, 0.78f, -0.07f, 1.54f)
            reflectiveQuadToRelative(-1.13f, 1.04f)
            lineToRelative(-7.53f, 2.75f)
            close()
            moveTo(12.45f, 19f)
            lineTo(20f, 16.25f)
            lineTo(15.53f, 4f)
            lineTo(8f, 6.75f)
            lineTo(12.45f, 19f)
            close()
            moveTo(14f, 11.5f)
            close()
          }
        }
        .build()
    return _playing_cards!!
  }

private var _playing_cards: ImageVector? = null
