package com.jimmy.sheepcardgame.ui.icons

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val CoinHeadIcon: ImageVector
    get() {
        if (coinHead != null) {
            return coinHead!!
        }

        coinHead = ImageVector.Builder(
            name = "coin_flip",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            addPath(
                addPathNodes("M 0 12 A 1 1 0 0 0 24 12 A 1 1 0 0 0 0 12 M 2 12 A 1 1 0 0 1 22 12 A 1 1 0 0 1 2 12 M 5 8 C 5 7 6 6 7 6 L 17 6 C 18 6 19 7 19 8 L 18 17 C 18 18 17 19 16 19 L 8 19 C 7 19 6 18 6 17 M 7 10 A 1 1 0 0 0 10 10 A 1 1 0 0 0 7 10 M 17 10 A 1 1 0 0 0 14 10 A 1 1 0 0 0 17 10 M 12 17 C 13 17 15 17 15 16 C 14 17 10 17 9 16 C 9 17 11 17 12 17"),
                fill = SolidColor(Color.Black),
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                pathFillType = PathFillType.NonZero
            )

        }.build()

        return coinHead!!
    }

@Suppress("CheckReturnValue")
val CoinTailIcon: ImageVector
    get() {
        if (coinTail != null) {
            return coinTail!!
        }

        coinTail = ImageVector.Builder(
            name = "coin_flip",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            addPath(
                addPathNodes("M6 17a1 1 0 003 0V14c2 1 4 1 6 0v3a1 1 0 003 0l1-6A1 1 0 005 11M2 12a1 1 0 0020 0A1 1 0 002 12M0 12a1 1 0 0124 0A1 1 0 010 12"),
                fill = SolidColor(Color.Black),
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                pathFillType = PathFillType.NonZero
            )

        }.build()

        return coinTail!!
    }

private var coinHead: ImageVector? = null
private var coinTail: ImageVector? = null

@Preview
@Composable
fun CoinFlipIconPreview() {
    Row{
        Icon(CoinHeadIcon,"")
        Icon(CoinTailIcon,"")
    }
}