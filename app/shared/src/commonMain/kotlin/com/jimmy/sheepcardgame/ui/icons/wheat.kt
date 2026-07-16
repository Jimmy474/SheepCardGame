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
public val WheatIcon: ImageVector
  get() {
    if (_wheat != null) {
      return _wheat!!
    }
    _wheat =
      ImageVector.Builder(
          name = "wheat",
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
            moveTo(2.75f, 21.28f)
            quadToRelative(-0.3f, -0.3f, -0.3f, -0.71f)
            reflectiveQuadToRelative(0.3f, -0.71f)
            lineToRelative(1.7f, -1.7f)
            quadTo(3.73f, 17.43f, 3.36f, 16.5f)
            reflectiveQuadTo(3f, 14.58f)
            quadTo(3f, 13.55f, 3.38f, 12.65f)
            reflectiveQuadToRelative(1.08f, -1.6f)
            lineTo(5.88f, 9.65f)
            lineToRelative(0.7f, 0.7f)
            quadTo(6.7f, 9.55f, 7.05f, 8.82f)
            reflectiveQuadTo(7.98f, 7.52f)
            lineTo(9.4f, 6.13f)
            lineToRelative(0.7f, 0.7f)
            quadTo(10.23f, 6.02f, 10.59f, 5.3f)
            quadTo(10.95f, 4.57f, 11.53f, 4f)
            lineTo(13.68f, 1.88f)
            quadTo(13.95f, 1.6f, 14.36f, 1.59f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            quadToRelative(0.28f, 0.27f, 0.28f, 0.69f)
            quadToRelative(0f, 0.41f, -0.28f, 0.71f)
            lineTo(14.38f, 4f)
            quadToRelative(0.58f, 0.57f, 0.93f, 1.3f)
            quadToRelative(0.35f, 0.72f, 0.47f, 1.52f)
            lineTo(19.9f, 2.7f)
            quadTo(20.2f, 2.4f, 20.6f, 2.41f)
            reflectiveQuadToRelative(0.7f, 0.31f)
            quadToRelative(0.28f, 0.3f, 0.28f, 0.7f)
            reflectiveQuadTo(21.3f, 4.13f)
            lineTo(17.2f, 8.25f)
            quadTo(18f, 8.38f, 18.73f, 8.73f)
            reflectiveQuadToRelative(1.3f, 0.92f)
            lineToRelative(0.7f, -0.7f)
            quadToRelative(0.3f, -0.3f, 0.71f, -0.3f)
            reflectiveQuadToRelative(0.71f, 0.3f)
            reflectiveQuadToRelative(0.3f, 0.7f)
            reflectiveQuadToRelative(-0.3f, 0.7f)
            lineToRelative(-2.1f, 2.13f)
            quadToRelative(-0.57f, 0.6f, -1.32f, 0.96f)
            reflectiveQuadToRelative(-1.55f, 0.49f)
            lineToRelative(0.7f, 0.68f)
            lineTo(16.48f, 16f)
            quadToRelative(-0.58f, 0.57f, -1.31f, 0.94f)
            reflectiveQuadToRelative(-1.54f, 0.49f)
            lineToRelative(0.73f, 0.7f)
            lineToRelative(-1.43f, 1.43f)
            quadToRelative(-0.72f, 0.72f, -1.64f, 1.09f)
            reflectiveQuadTo(9.38f, 21f)
            quadTo(8.53f, 21f, 7.66f, 20.64f)
            reflectiveQuadTo(5.9f, 19.52f)
            lineTo(4.15f, 21.28f)
            quadToRelative(-0.3f, 0.3f, -0.7f, 0.3f)
            reflectiveQuadToRelative(-0.7f, -0.3f)
            close()
            moveTo(5.88f, 16.7f)
            quadTo(6.3f, 16.27f, 6.53f, 15.74f)
            reflectiveQuadTo(6.75f, 14.6f)
            quadToRelative(0f, -0.58f, -0.22f, -1.14f)
            reflectiveQuadTo(5.88f, 12.48f)
            quadTo(5.45f, 12.9f, 5.21f, 13.46f)
            reflectiveQuadTo(4.98f, 14.6f)
            quadToRelative(0f, 0.6f, 0.24f, 1.14f)
            reflectiveQuadTo(5.88f, 16.7f)
            close()
            moveTo(9.4f, 19.02f)
            quadTo(10f, 19f, 10.55f, 18.77f)
            reflectiveQuadToRelative(0.97f, -0.65f)
            quadTo(11.1f, 17.7f, 10.54f, 17.48f)
            reflectiveQuadTo(9.4f, 17.25f)
            quadToRelative(-0.57f, 0f, -1.14f, 0.24f)
            reflectiveQuadTo(7.28f, 18.15f)
            quadTo(7.7f, 18.58f, 8.25f, 18.8f)
            quadTo(8.8f, 19.02f, 9.4f, 19.02f)
            close()
            moveToRelative(0f, -5.85f)
            quadTo(9.83f, 12.75f, 10.05f, 12.2f)
            reflectiveQuadToRelative(0.22f, -1.13f)
            quadToRelative(0f, -0.6f, -0.22f, -1.15f)
            quadTo(9.83f, 9.38f, 9.4f, 8.95f)
            quadTo(8.98f, 9.38f, 8.74f, 9.92f)
            quadTo(8.5f, 10.48f, 8.5f, 11.08f)
            quadToRelative(0f, 0.57f, 0.24f, 1.13f)
            reflectiveQuadTo(9.4f, 13.18f)
            close()
            moveToRelative(3.55f, 2.33f)
            quadToRelative(0.6f, 0f, 1.14f, -0.24f)
            quadToRelative(0.54f, -0.24f, 0.96f, -0.66f)
            quadTo(14.63f, 14.18f, 14.09f, 13.95f)
            reflectiveQuadTo(12.95f, 13.73f)
            reflectiveQuadTo(11.8f, 13.96f)
            reflectiveQuadToRelative(-0.97f, 0.66f)
            quadToRelative(0.42f, 0.42f, 0.97f, 0.65f)
            reflectiveQuadToRelative(1.15f, 0.22f)
            close()
            moveToRelative(0f, -5.85f)
            quadTo(13.38f, 9.23f, 13.59f, 8.67f)
            quadTo(13.8f, 8.13f, 13.8f, 7.52f)
            quadToRelative(0f, -0.57f, -0.21f, -1.14f)
            reflectiveQuadTo(12.95f, 5.4f)
            quadTo(12.53f, 5.82f, 12.29f, 6.39f)
            reflectiveQuadTo(12.05f, 7.52f)
            quadToRelative(0f, 0.6f, 0.24f, 1.15f)
            quadToRelative(0.24f, 0.55f, 0.66f, 0.97f)
            close()
            moveToRelative(3.53f, 2.3f)
            quadToRelative(0.57f, 0f, 1.14f, -0.24f)
            quadToRelative(0.56f, -0.24f, 0.99f, -0.66f)
            quadTo(18.15f, 10.63f, 17.59f, 10.4f)
            reflectiveQuadTo(16.45f, 10.17f)
            quadToRelative(-0.57f, 0.03f, -1.12f, 0.25f)
            reflectiveQuadToRelative(-0.97f, 0.65f)
            quadToRelative(0.42f, 0.43f, 0.99f, 0.66f)
            quadToRelative(0.56f, 0.24f, 1.14f, 0.21f)
            close()
          }
        }
        .build()
    return _wheat!!
  }

private var _wheat: ImageVector? = null
