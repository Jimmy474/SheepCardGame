package com.jimmy.sheepcardgame.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.*
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jimmy.sheepcardgame.GameDialogs
import com.jimmy.sheepcardgame.GameEvents
import com.jimmy.sheepcardgame.data.Card
import com.jimmy.sheepcardgame.data.Deck
import com.jimmy.sheepcardgame.data.SheepColor
import com.jimmy.sheepcardgame.data.SheepSide
import com.jimmy.sheepcardgame.ui.screens.LocalOnEvent
import com.jimmy.sheepcardgame.ui.theme.CardGameTheme
import com.jimmy.sheepcardgame.ui.theme.colorScheme

@Composable
@OptIn(ExperimentalFoundationStyleApi::class)
fun CardDisplay(card: Card, modifier: Modifier = Modifier, isClickable: Boolean = false, isSelected: Boolean = false, hasMenu: Boolean = true, onSelected: (Card) -> Unit = {}) {

    val interactionSource = remember { MutableInteractionSource() }
    val styleState = rememberUpdatedStyleState(interactionSource){
        it.isSelected = isSelected
    }
    val style = Style {
        border(4.dp, Color.White)
        shape(RoundedCornerShape(4.dp))
        clip(true)

        hovered {
            animate {
                if(isClickable) border(4.dp, colorScheme.tertiary)
            }
        }

        selected {
            animate {
                border(4.dp, colorScheme.inversePrimary)
            }
        }
    }
    var isMenuExpanded by remember { mutableStateOf(false) }

    val onEvent = LocalOnEvent.current

    Box(
        modifier
            .size(80.dp,112.dp)
            .hoverable(interactionSource, isClickable)
            .styleable(styleState, style)
            .pointerInput(isClickable) {
                if (!isClickable) return@pointerInput
                detectTapGestures(onLongPress = { isMenuExpanded = true }, onTap = { onSelected(card) })
            }
            .pointerInput(isClickable && hasMenu) {
                if (!isClickable || !hasMenu) return@pointerInput
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (isClickable && hasMenu && event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            event.changes.forEach { it.consume() }
                            isMenuExpanded = true
                        }
                    }
                }
            }
    ) {
        when (card) {
            is Card.GoldCard -> DisplayGoldCard(card)
            is Card.ModifierCard -> DisplayModifierCard(card)
            is Card.SheepCard -> DisplaySheepCard(card)
            is Card.SpecialCard -> DisplaySpecialCard(card)
        }

        DropdownMenu(
            expanded = isMenuExpanded && hasMenu,
            onDismissRequest = { isMenuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Discard") },
                onClick = {
                    isMenuExpanded = false
                    onEvent(GameEvents.ChangeDialog(GameDialogs.DiscardConfirmation(card)))
                }
            )
        }
    }
}

@Composable
fun BoxScope.DisplayGoldCard(card: Card.GoldCard) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        spiralSunBurst(Color(0xFFeda81b), Color(0xFFf7c522), maxTwistDegrees = -180f)
    }
    Box(
        Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .align(Alignment.Center)
            .background(Color(0xFF593c2b), RoundedCornerShape(4.dp))
            .padding(4.dp)
            .background(Color(0xFFffecb5), RoundedCornerShape(2.dp))
            .clip(RoundedCornerShape(2.dp))
    ) {
        Column(Modifier.align(Alignment.Center).padding(2.dp), Arrangement.Center, Alignment.CenterHorizontally) {
            Text(card.goldCardType.title.uppercase(), color = Color(0xFF3ea37c), style = MaterialTheme.typography.bodySmall)
            Text(
                card.goldCardType.amount.uppercase(),
                maxLines = 1,
                color = Color(0xFFde782b),
                autoSize = TextAutoSize.StepBased(1.sp, 12.sp),
                style = MaterialTheme.typography.labelSmall
            )
            Text(card.goldCardType.location.uppercase(), color = Color(0xFFa69888), fontSize = 5.sp, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
        }
    }
    Text(
        "FLIP THE COIN\nto decide",
        Modifier.padding(8.dp, 4.dp).align(Alignment.BottomCenter),
        Color(0xFF7f5728),
        fontSize = 6.sp,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall
    )
}

@Composable
fun BoxScope.DisplayModifierCard(card: Card.ModifierCard) {

    Canvas(modifier = Modifier.fillMaxSize()) {
        spiralSunBurst(Color(0xFFc882ae), Color(0xFFa56fa3), 10, 10f, -90f)
    }

    Text(
        card.modifierType.title.uppercase(),
        Modifier.padding(horizontal = 2.dp).align(Alignment.TopEnd),
        style = MaterialTheme.typography.bodyMedium.copy(Color.White, 12.sp, shadow = Shadow(Color(0xFF422444), Offset(-3f, 2f)))
    )
    Text(
        card.modifierType.description,
        Modifier.padding(8.dp, 4.dp).align(Alignment.BottomCenter),
        Color(0xFF5e365f),
        fontSize = 6.sp,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall
    )
}

@Composable
fun BoxScope.DisplaySheepCard(card: Card.SheepCard) {

    Canvas(modifier = Modifier.fillMaxSize()) {
        spiralSunBurst(Color(0xFFf299ba), Color(0xFFf8c7d5), 20, 0f, 90f)

        val body = Size(40.dp.toPx(), 30.dp.toPx())
        val border = 2.dp.toPx()
        val cornerRadius = 4.dp.toPx()
        val bodyTL = Offset(if (card.sheepSide == SheepSide.Front) size.width - body.width else 0f, (size.height / 2f) - (body.height / 2f) - (border / 2f))
        val bodyPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(bodyTL.x, bodyTL.y, bodyTL.x + body.width, bodyTL.y + body.height),
                    topLeft = if (card.sheepSide == SheepSide.Front) CornerRadius(cornerRadius) else CornerRadius.Zero,
                    topRight = if (card.sheepSide == SheepSide.Front) CornerRadius.Zero else CornerRadius(cornerRadius),
                    bottomRight = if (card.sheepSide == SheepSide.Front) CornerRadius.Zero else CornerRadius(cornerRadius),
                    bottomLeft = if (card.sheepSide == SheepSide.Front) CornerRadius(cornerRadius) else CornerRadius.Zero
                )
            )
        }

        val head = 25.dp.toPx()
        val headTL = Offset(bodyTL.x - (head / 2f), bodyTL.y - (head / 2f))

        val tail = 8.dp.toPx()
        val tailTL = Offset(bodyTL.x + body.width - (tail / 2f), bodyTL.y + (tail / 2f))

        val leg = Size(8.dp.toPx(), 16.dp.toPx())
        val legTL = Offset(bodyTL.x + leg.width, bodyTL.y + body.height - 2.dp.toPx())

        var stop = 0f
        fun next(): Float {
            stop += 1 / 13f
            return stop
        }

        val (start, end) =
            if (card.sheepSide == SheepSide.Front) {
                headTL to Offset(size.width + tailTL.x + tail, legTL.y + leg.height)
            } else {
                Offset(headTL.x - size.width, headTL.y) to Offset(tailTL.x + tail, legTL.y + leg.height)
            }

        val brush = Brush.linearGradient(
            next() to Color(0xFFE7372F),
            stop to Color(0xFFEE7625),
            next() to Color(0xFFEE7625),
            stop to Color(0xFFFCBE0B),
            next() to Color(0xFFFCBE0B),
            stop to Color(0xFF89BD24),
            next() to Color(0xFF89BD24),
            stop to Color(0xFF2BA43C),
            next() to Color(0xFF2BA43C),
            stop to Color(0xFF6DBE97),
            next() to Color(0xFF6DBE97),
            stop to Color(0xFF01B0E4),
            next() to Color(0xFF01B0E4),
            stop to Color(0xFF6DBE97),
            next() to Color(0xFF6DBE97),
            stop to Color(0xFF01B0E4),
            next() to Color(0xFF01B0E4),
            stop to Color(0xFF284B9B),
            next() to Color(0xFF284B9B),
            stop to Color(0xFF8A569D),
            next() to Color(0xFF8A569D),
            stop to Color(0xFFE45D9D),
            next() to Color(0xFFE45D9D),
            stop to Color(0xFFF4AECE),
            start = start,
            end = end
        )

        drawBorderedRect(legTL, leg, cornerRadius / 2, border, card.sheepColor == SheepColor.Rainbow, Color(card.sheepColor.color), brush)

        if (card.sheepColor == SheepColor.Rainbow) {
            drawPath(bodyPath, brush)
            drawPath(bodyPath, Color.Black, style = Stroke(border / 2f))
        } else {
            drawPath(bodyPath, Color(card.sheepColor.color))
            drawPath(bodyPath, Color.Black, style = Stroke(border / 2f))
        }

        val leg2TL = Offset(bodyTL.x + body.width - leg.width * 1.5f, legTL.y)
        drawBorderedRect(leg2TL, leg, cornerRadius / 2, border, card.sheepColor == SheepColor.Rainbow, Color(card.sheepColor.color), brush)

        if (card.sheepSide == SheepSide.Front) {
            drawBorderedRect(headTL, Size(head, head), cornerRadius, border, card.sheepColor == SheepColor.Rainbow, Color(card.sheepColor.color), brush)

            val faceOffset = 5.dp.toPx()
            drawRoundRect(Color(0xFFfbddc9), headTL.plus(Offset(faceOffset / 2f, faceOffset / 2f)), Size(head - faceOffset, head - faceOffset), CornerRadius(cornerRadius))
        } else {
            drawBorderedRect(tailTL, Size(tail, tail), cornerRadius / 2, border, card.sheepColor == SheepColor.Rainbow, Color(card.sheepColor.color), brush)
        }

    }

    VerticalTextWrapper(
        Modifier.align(if (card.sheepSide == SheepSide.Front) Alignment.BottomStart else Alignment.TopEnd).padding(0.dp, 2.dp),
        if (card.sheepSide == SheepSide.Front) -90f else 90f
    ) {
        Text(
            text = card.sheepColor.name.uppercase(),
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 12.sp, shadow = Shadow(color = Color(0xFFa9576f), offset = Offset(-2f, 2f))),
            softWrap = false
        )
    }

}

@Composable
fun BoxScope.DisplaySpecialCard(card: Card.SpecialCard) {

    Canvas(modifier = Modifier.fillMaxSize()) {
        spiralSunBurst(Color(0xFF94CBA2), Color(0xFF71C0AD), 16, -5f)
    }

    Text(
        card.specialType.title.uppercase(),
        Modifier.padding(horizontal = 2.dp).align(Alignment.TopEnd),
        style = MaterialTheme.typography.bodySmall.copy(Color.White, shadow = Shadow(Color(0xFF216951), Offset(-2f, 2f)))
    )
    Text(
        card.specialType.description,
        Modifier.padding(8.dp, 4.dp).align(Alignment.BottomCenter),
        Color(0xFF44866c),
        fontSize = 6.sp,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall
    )
}

@OptIn(ExperimentalFlexBoxApi::class)
@Preview(widthDp = 1000, heightDp = 1000)
@Composable
fun CardDisplayPreview() {
    val deck = Deck()

    CompositionLocalProvider(LocalOnEvent provides {}){
        CardGameTheme {
            Surface {
                Scaffold {
                    FlexBox(Modifier.padding(it).padding(8.dp), {
                        alignItems(FlexAlignItems.Center)
                        justifyContent(FlexJustifyContent.Center)
                        wrap(FlexWrap.Wrap)
                    }) {
                        deck.cards.forEach { card ->
                            CardDisplay(card)
                        }
                    }
                }
            }
        }
    }

}