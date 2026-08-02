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
val ExitIcon: ImageVector
  get() {
    if (_exit_to_app != null) {
      return _exit_to_app!!
    }
    _exit_to_app =
      ImageVector.Builder(
          name = "exit_to_app",
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
            moveTo(5f, 21f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(16f)
            quadTo(3f, 15.58f, 3.29f, 15.29f)
            reflectiveQuadTo(4f, 15f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(5f, 16f)
            verticalLineToRelative(3f)
            horizontalLineTo(19f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            verticalLineTo(8f)
            quadTo(5f, 8.42f, 4.71f, 8.71f)
            reflectiveQuadTo(4f, 9f)
            reflectiveQuadTo(3.29f, 8.71f)
            reflectiveQuadTo(3f, 8f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineTo(19f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(21f, 5f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(5f)
            close()
            moveToRelative(6.65f, -8f)
            horizontalLineTo(4f)
            quadTo(3.58f, 13f, 3.29f, 12.71f)
            quadTo(3f, 12.43f, 3f, 12f)
            reflectiveQuadTo(3.29f, 11.29f)
            reflectiveQuadTo(4f, 11f)
            horizontalLineToRelative(7.65f)
            lineTo(9.8f, 9.15f)
            quadTo(9.5f, 8.85f, 9.51f, 8.45f)
            reflectiveQuadTo(9.8f, 7.75f)
            quadToRelative(0.3f, -0.3f, 0.71f, -0.31f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            lineTo(14.8f, 11.3f)
            quadToRelative(0.15f, 0.15f, 0.21f, 0.33f)
            reflectiveQuadTo(15.08f, 12f)
            reflectiveQuadToRelative(-0.06f, 0.38f)
            reflectiveQuadTo(14.8f, 12.7f)
            lineToRelative(-3.57f, 3.57f)
            quadToRelative(-0.3f, 0.3f, -0.71f, 0.29f)
            reflectiveQuadTo(9.8f, 16.25f)
            quadTo(9.53f, 15.95f, 9.51f, 15.55f)
            reflectiveQuadTo(9.8f, 14.85f)
            lineTo(11.65f, 13f)
            close()
          }
        }
        .build()
    return _exit_to_app!!
  }

private var _exit_to_app: ImageVector? = null
