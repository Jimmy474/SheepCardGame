package com.jimmy.sheepcardgame.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jimmy.sheepcardgame.GameDialogs
import com.jimmy.sheepcardgame.GameScreenEvents
import com.jimmy.sheepcardgame.data.Card
import com.jimmy.sheepcardgame.data.Deck
import com.jimmy.sheepcardgame.ui.screens.LocalOnEvent
import com.jimmy.sheepcardgame.ui.theme.CardGameTheme
import com.jimmy.sheepcardgame.ui.theme.colorScheme
import sheepcardgame.app.shared.generated.resources.Res
import kotlin.random.Random

enum class CardSize(val size: DpSize){
    Minimal(DpSize(40.dp, 56.dp)),
    Small(DpSize(80.dp, 112.dp)),
    Medium(DpSize(120.dp, 168.dp)),
    Large(DpSize(160.dp, 224.dp)),
    Huge(DpSize(200.dp, 280.dp)),
    Giant(DpSize(240.dp, 336.dp)),
}

@Composable
@OptIn(ExperimentalFoundationStyleApi::class)
fun CardDisplay(card: Card, modifier: Modifier = Modifier, isClickable: Boolean = false, isSelected: Boolean = false, hasMenu: Boolean = true, cardSize: CardSize = CardSize.Small, onSelected: (Card) -> Unit = {}) {

    val interactionSource = remember { MutableInteractionSource() }
    val styleState = rememberUpdatedStyleState(interactionSource) {
        it.isSelected = isSelected
    }

    val style = Style {
        shape(RoundedCornerShape(4.dp))
        clip(true)

        hovered {
            animate {
                if (isClickable) {
                    innerShadow(Shadow(6.dp, colorScheme.tertiary, 3.dp))
                }
            }
        }
    }
    var isMenuExpanded by remember { mutableStateOf(false) }

    val onEvent = LocalOnEvent.current

    Box(
        modifier
            .size(cardSize.size)
            .hoverable(interactionSource, isClickable)
            .styleable(styleState, style)
            .then(if(isSelected) Modifier.animatedDottedBorder(3.dp, MaterialTheme.colorScheme.onTertiaryContainer) else Modifier)
            .pointerInput(isClickable) {
                if (!isClickable) return@pointerInput
                detectTapGestures(onLongPress = { isMenuExpanded = true }, onTap = { onSelected(card) }, onDoubleTap = { onEvent(GameScreenEvents.OpenDialog(GameDialogs.ExpandedView(card))) })
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
        var imageBytes by remember { mutableStateOf<ByteArray?>(null) }

        LaunchedEffect(card) {
            imageBytes = null
            try {
                imageBytes = Res.readBytes("files/cards/${card.resourceName}.png")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        imageBytes?.let {
            AsyncImage(
                model = it,
                contentDescription = card.accessibilityName,
                contentScale = ContentScale.Crop,
            )
        }

        DropdownMenu(
            expanded = isMenuExpanded && hasMenu,
            onDismissRequest = { isMenuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("View") },
                onClick = {
                    isMenuExpanded = false
                    onEvent(GameScreenEvents.OpenDialog(GameDialogs.ExpandedView(card)))
                }
            )
            DropdownMenuItem(
                text = { Text("Select") },
                onClick = {
                    isMenuExpanded = false
                    onSelected(card)
                }
            )
            DropdownMenuItem(
                text = { Text("Discard") },
                onClick = {
                    isMenuExpanded = false
                    onEvent(GameScreenEvents.OpenDialog(GameDialogs.DiscardConfirmation(listOf(card))))
                }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationStyleApi::class)
fun DrawHiddenCard(cardId: Int, isSelected: Boolean, onSelect: (Int) -> Unit) {
    val style = Style {
        border(0.dp, Color.Transparent)
        shape(RoundedCornerShape(4.dp))
        clip(true)

        hovered {
            animate {
                innerShadow(Shadow(6.dp, colorScheme.secondary, 3.dp))
            }
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = rememberUpdatedStyleState(interactionSource) {
        it.isSelected = isSelected
    }
    Box(Modifier
        .size(80.dp, 112.dp)
        .hoverable(interactionSource)
        .styleable(styleState, style)
        .then(if(isSelected) Modifier.animatedDottedBorder(3.dp, MaterialTheme.colorScheme.onTertiaryContainer) else Modifier)
        .clickable { onSelect(cardId) }
    ) {
        var imageBytes by remember { mutableStateOf<ByteArray?>(null) }

        LaunchedEffect(Unit) {
            imageBytes = null
            try {
                imageBytes = Res.readBytes("files/cards/Back.png")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        imageBytes?.let {
            AsyncImage(
                model = it,
                contentDescription = "Hidden card",
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
expect fun PreviewContext()

@OptIn(ExperimentalFlexBoxApi::class)
@Preview(widthDp = 1000, heightDp = 1000, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun CardDisplayPreview() {
    PreviewContext()
    CompositionLocalProvider(LocalOnEvent provides {}) {
        CardGameTheme {
            Surface {
                Scaffold {
                    FlexBox(Modifier.padding(it).padding(8.dp), {
                        gap(8.dp)
                        alignItems(FlexAlignItems.Center)
                        justifyContent(FlexJustifyContent.Center)
                        wrap(FlexWrap.Wrap)
                    }) {
                        Deck.getFullDeckUnshuffled().forEach { card ->
                            CardDisplay(card, Modifier, true, Random.nextBoolean())
                        }
                        DrawHiddenCard(1, false) { }
                    }
                }
            }
        }
    }

}