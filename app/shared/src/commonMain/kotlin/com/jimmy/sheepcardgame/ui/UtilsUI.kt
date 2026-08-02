package com.jimmy.sheepcardgame.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.jimmy.sheepcardgame.data.Card
import com.jimmy.sheepcardgame.ui.icons.TimerIcon
import com.jimmy.sheepcardgame.ui.screens.DropZoneInfo
import com.jimmy.sheepcardgame.ui.screens.LocalDragDropState
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun Boolean.headTail(): String = if (this) "Heads" else "Tails"

@Composable
fun SmallListItem(
    modifier: Modifier = Modifier,
    headline: @Composable () -> Unit = {},
    text: @Composable () -> Unit = {},
    leadingIcon: @Composable () -> Unit = {},
    trailingIcon: @Composable () -> Unit = {},
) {
    Row(modifier, Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
        leadingIcon()
        Column {
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleLarge) {
                headline()
            }
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyLarge) {
                text()
            }
        }
        trailingIcon()
    }
}

@Composable
fun Modifier.animatedDottedBorder(
    strokeWidth: Dp = 2.dp,
    color: Color = Color.Black,
    dashLength: Dp = 8.dp,
    gapLength: Dp = 8.dp,
    shape: Shape = RoundedCornerShape(4.dp),
    isRoundDots: Boolean = true,
    durationMillis: Int = 1000,
    reverseDirection: Boolean = false
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "BorderAnimation")
    val totalDashPeriod = dashLength + gapLength
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reverseDirection) -1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PhaseShift"
    )

    return this.drawWithCache {
        val x = strokeWidth.toPx() / 2
        val adjustedSize = Size(
            width = size.width - (x*2),
            height = size.height - (x*2)
        )
        val outline = shape.createOutline(adjustedSize, layoutDirection, this)
        val periodPx = totalDashPeriod.toPx()
        val currentPhase = phaseShift * periodPx

        val stroke = Stroke(
            width = strokeWidth.toPx(),
            cap = if (isRoundDots) StrokeCap.Round else StrokeCap.Butt,
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(dashLength.toPx(), gapLength.toPx()),
                phase = currentPhase
            )
        )


        onDrawWithContent {
            drawContent()
            withTransform({
                translate(x,x)
            }){
                drawOutline(outline, color, style = stroke)
            }
        }
    }
}

@Preview
@Composable
fun TestBorderView() {
    Box(
        modifier = Modifier
            .size(150.dp) // Set size first!
            .animatedDottedBorder(
                strokeWidth = 4.dp,
                color = Color.Red,
                dashLength = 12.dp,
                gapLength = 12.dp,
                isRoundDots = true // Works perfectly now with the 0.001f fix
            )
    )
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

@Composable
fun GameTimer(
    gameStartTimeMillis: Long,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }

    LaunchedEffect(gameStartTimeMillis) {
        while (true) {
            currentTime = Clock.System.now().toEpochMilliseconds()
            delay(1000L.milliseconds)
        }
    }

    val elapsed = (currentTime - gameStartTimeMillis).coerceAtLeast(0).milliseconds
    SmallListItem(
        modifier = modifier,
        leadingIcon = { Icon(TimerIcon, contentDescription = "Game Timer") },
        headline = { Text(elapsed.readableTime()) },
    )
}

fun Duration.readableTime(): String {
    val hours = inWholeHours
    val minutes = (inWholeMinutes % 60).toString().padStart(2, '0')
    val seconds = (inWholeSeconds % 60).toString().padStart(2, '0')

    return if (hours > 0) "$hours:$minutes:$seconds" else "$minutes:$seconds"
}

fun Modifier.draggableCard(card: Card, exclusive: Boolean = true, additionalCards: () -> List<Card> = { emptyList() }): Modifier = composed {
    val state = LocalDragDropState.current
    val currentAdditionalCards by rememberUpdatedState(additionalCards)

    var layoutCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    this
        .onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) {
                layoutCoordinates = coordinates
            }
        }
        .pointerInput(card) {
            detectDragGestures(
                onDragStart = { offset ->
                    state.isDragging = true
                    val cards = currentAdditionalCards()
                    state.draggedCards += cards
                    if (!exclusive || cards.isEmpty()) state.draggedCards += card

                    val startPosition = layoutCoordinates?.localToWindow(offset) ?: offset
                    state.currentPosition = startPosition
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    state.currentPosition += dragAmount
                },
                onDragEnd = {
                    state.onDrop()
                },
                onDragCancel = {
                    state.reset()
                }
            )
        }.then(
            if (state.isDragging && card in state.draggedCards) this.alpha(0.5f) else this
        )
}

fun Modifier.dropZone(
    shape: Shape = RoundedCornerShape(4.dp),
    hoverColor: Color = Color.White,
    predicate: (Set<Card>) -> Boolean,
    onCardDropped: (Set<Card>) -> Unit
): Modifier = composed {
    val state = LocalDragDropState.current

    val currentPredicate by rememberUpdatedState(predicate)
    val currentOnDrop by rememberUpdatedState(onCardDropped)

    val zoneId = remember { Any() }

    val zoneInfo = remember {
        DropZoneInfo(
            bounds = Rect.Zero,
            predicate = { currentPredicate(it) },
            onDrop = { currentOnDrop(it) }
        )
    }

    DisposableEffect(zoneId, zoneInfo) {
        state.dropZones[zoneId] = zoneInfo

        onDispose {
            state.dropZones.remove(zoneId)
        }
    }

    val isHovered = state.isDragging &&
            state.draggedCards.isNotEmpty() &&
            zoneInfo.bounds.contains(state.currentPosition) &&
            currentPredicate(state.draggedCards)

    this
        .onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) {
                zoneInfo.bounds = coordinates.boundsInWindow()
            }
        }
        .then(
            if (isHovered) {
                Modifier.animatedDottedBorder(3.dp, hoverColor, shape = shape)
            } else {
                Modifier
            }
        )
}