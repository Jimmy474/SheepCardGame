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
public val PetsIcon: ImageVector
  get() {
    if (_pets != null) {
      return _pets!!
    }
    _pets =
      ImageVector.Builder(
          name = "pets",
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
            moveTo(4.5f, 12.13f)
            quadToRelative(-1.05f, 0f, -1.78f, -0.73f)
            quadTo(2f, 10.68f, 2f, 9.63f)
            quadTo(2f, 8.57f, 2.73f, 7.85f)
            quadTo(3.45f, 7.13f, 4.5f, 7.13f)
            reflectiveQuadTo(6.28f, 7.85f)
            reflectiveQuadTo(7f, 9.63f)
            reflectiveQuadTo(6.28f, 11.4f)
            quadTo(5.55f, 12.13f, 4.5f, 12.13f)
            close()
            moveTo(7.23f, 7.4f)
            quadTo(6.5f, 6.68f, 6.5f, 5.63f)
            reflectiveQuadTo(7.23f, 3.85f)
            quadTo(7.95f, 3.13f, 9f, 3.13f)
            reflectiveQuadToRelative(1.78f, 0.72f)
            reflectiveQuadTo(11.5f, 5.63f)
            reflectiveQuadTo(10.78f, 7.4f)
            quadTo(10.05f, 8.13f, 9f, 8.13f)
            reflectiveQuadTo(7.23f, 7.4f)
            close()
            moveToRelative(6f, 0f)
            quadTo(12.5f, 6.68f, 12.5f, 5.63f)
            reflectiveQuadTo(13.23f, 3.85f)
            quadTo(13.95f, 3.13f, 15f, 3.13f)
            reflectiveQuadToRelative(1.78f, 0.72f)
            reflectiveQuadTo(17.5f, 5.63f)
            reflectiveQuadTo(16.78f, 7.4f)
            quadTo(16.05f, 8.13f, 15f, 8.13f)
            reflectiveQuadTo(13.23f, 7.4f)
            close()
            moveToRelative(6.27f, 4.73f)
            quadToRelative(-1.05f, 0f, -1.77f, -0.73f)
            quadTo(17f, 10.68f, 17f, 9.63f)
            quadTo(17f, 8.57f, 17.73f, 7.85f)
            quadTo(18.45f, 7.13f, 19.5f, 7.13f)
            reflectiveQuadToRelative(1.78f, 0.72f)
            reflectiveQuadTo(22f, 9.63f)
            reflectiveQuadTo(21.28f, 11.4f)
            quadToRelative(-0.73f, 0.73f, -1.78f, 0.73f)
            close()
            moveToRelative(-12.85f, 10f)
            quadToRelative(-1.13f, 0f, -1.89f, -0.86f)
            reflectiveQuadTo(4f, 19.23f)
            quadToRelative(0f, -1.3f, 0.89f, -2.28f)
            quadTo(5.78f, 15.98f, 6.65f, 15.03f)
            quadTo(7.38f, 14.25f, 7.9f, 13.34f)
            quadTo(8.43f, 12.43f, 9.15f, 11.63f)
            quadTo(9.7f, 10.98f, 10.43f, 10.55f)
            quadTo(11.15f, 10.13f, 12f, 10.13f)
            reflectiveQuadToRelative(1.58f, 0.4f)
            quadToRelative(0.72f, 0.4f, 1.28f, 1.05f)
            quadToRelative(0.7f, 0.8f, 1.24f, 1.72f)
            quadToRelative(0.54f, 0.93f, 1.26f, 1.73f)
            quadToRelative(0.88f, 0.95f, 1.76f, 1.92f)
            quadTo(20f, 17.93f, 20f, 19.23f)
            quadToRelative(0f, 1.17f, -0.76f, 2.04f)
            reflectiveQuadToRelative(-1.89f, 0.86f)
            quadTo(16f, 22.13f, 14.68f, 21.9f)
            quadTo(13.35f, 21.68f, 12f, 21.68f)
            reflectiveQuadTo(9.33f, 21.9f)
            quadTo(8f, 22.13f, 6.65f, 22.13f)
            close()
          }
        }
        .build()
    return _pets!!
  }

private var _pets: ImageVector? = null
