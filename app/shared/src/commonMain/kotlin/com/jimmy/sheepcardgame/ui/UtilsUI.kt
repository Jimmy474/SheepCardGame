package com.jimmy.sheepcardgame.ui

import androidx.annotation.FloatRange
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultBlendMode
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.jimmy.sheepcardgame.ui.icons.CrownIcon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


val Double.radians: Double get() = this * PI / 180.0

@Composable
fun VerticalTextWrapper(
    modifier: Modifier = Modifier,
    degrees: Float = -90f,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))

        val layoutWidth = placeable.height
        val layoutHeight = placeable.width

        layout(layoutWidth, layoutHeight) {
            placeable.placeWithLayer(
                x = (layoutWidth - placeable.width) / 2,
                y = (layoutHeight - placeable.height) / 2,
                layerBlock = {
                    rotationZ = degrees
                }
            )
        }
    }
}

fun DrawScope.spiralSunBurst(
    color1: Color,
    color2: Color,
    slices: Int = 12,
    sliceWidthDifference: Float = 0f,
    maxTwistDegrees: Float = 0f,
    radialSteps: Int = 20,
){
    val centerX = size.width / 2
    val centerY = size.height / 2
    val outerRadius = size.maxDimension
    val angleStep = 360f / slices

    var currentAngle = 0f - (angleStep - sliceWidthDifference) / 2
    for (i in 0 until slices) {
        val step = if(i % 2 == 0) angleStep - sliceWidthDifference else angleStep + sliceWidthDifference
        val baseAngle1 = currentAngle
        val baseAngle2 = currentAngle + step
        currentAngle += step

        val path = Path()
        path.moveTo(centerX, centerY)

        fun drawSpiralStep(step: Int, baseAngle: Float) {
            val progress = step.toFloat() / radialSteps
            val currentRadius = progress * outerRadius
            val twist = progress * maxTwistDegrees
            val angleRad = (baseAngle + twist).toDouble().radians

            path.lineTo(
                (centerX + currentRadius * cos(angleRad)).toFloat(),
                (centerY + currentRadius * sin(angleRad)).toFloat()
            )
        }

        for (step in 1..radialSteps) {
            drawSpiralStep(step,baseAngle1)
        }

        for (step in radialSteps downTo 1) {
            drawSpiralStep(step,baseAngle2)
        }

        path.close()

        drawPath(path, if (i % 2 == 0) color1 else color2)
    }
}

fun DrawScope.drawRoundRect(
    useBrush: Boolean = false,
    color: Color,
    brush: Brush,
    topLeft: Offset = Offset.Zero,
    size: Size = Size(this.size.width - topLeft.x, this.size.height - topLeft.y),
    cornerRadius: CornerRadius = CornerRadius.Zero,
    style: DrawStyle = Fill,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DefaultBlendMode,
){
    if(useBrush) drawRoundRect(brush,topLeft, size, cornerRadius, alpha, style, colorFilter, blendMode)
    else drawRoundRect(color,topLeft, size, cornerRadius, style, alpha, colorFilter, blendMode)
}

fun DrawScope.drawBorderedRect(
    topLeft: Offset,
    size: Size,
    corner: Float,
    border: Float,
    useRainbow: Boolean,
    color: Color,
    brush: Brush
) {
    drawRoundRect(Color.Black, topLeft, size, CornerRadius(corner), Stroke(border))
    drawRoundRect(useRainbow, color, brush, topLeft, size, CornerRadius(corner))
}

@Composable
fun SmallListItem(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
    headline: @Composable () -> Unit = {},
    text: @Composable () -> Unit = {}
){
    Row(modifier, Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
        icon()
        Column{
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleLarge){
                headline()
            }
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyLarge){
                text()
            }
        }
    }
}

fun Modifier.pulsatingBorder(
    enabled: Boolean,
    color: Color = Color.Blue,
    shape: Shape = CircleShape,
    blur: Dp = 2.dp,
    spread: Dp = 2.dp,
    durationMillis: Int = 1000
): Modifier = composed {
    if (!enabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "borderAlphaPulse")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    this.innerShadow(shape, Shadow(blur, color, spread, DpOffset.Zero, alpha))
}