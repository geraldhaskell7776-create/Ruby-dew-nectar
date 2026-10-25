package com.rubydew.nectar.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp

/** Shared look for every rounded box in the app, so all screens match the Home banner. */
val RaisedShape = RoundedCornerShape(18.dp)
val RaisedElevation = 10.dp
val GoldRim = BorderStroke(
    2.dp,
    Brush.verticalGradient(listOf(Color(0xFFFFE9A8), CardGold, Color(0xFF8B6508))),
)

/** Light wash from top to bottom so plain boxes look slightly domed. */
private val DomeWash = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.12f), Color.Transparent, Color.Black.copy(alpha = 0.06f))
)

/**
 * A raised, rounded box: gold rim, bevel shading (light top edge, dark bottom edge)
 * and a drop shadow. Use instead of Card everywhere.
 */
@Composable
fun RaisedCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = contentColorFor(containerColor).takeOrElse { MaterialTheme.colorScheme.onSurface },
    background: Brush? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RaisedShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = RaisedElevation,
        border = GoldRim,
    ) {
        Box(
            Modifier
                .background(background ?: DomeWash)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .drawWithContent {
                    drawContent()
                    val edge = 3.dp.toPx()
                    drawRect(Color.White.copy(alpha = 0.28f), size = Size(size.width, edge))
                    drawRect(
                        Color.Black.copy(alpha = 0.25f),
                        topLeft = Offset(0f, size.height - edge),
                        size = Size(size.width, edge),
                    )
                }
        ) {
            content()
        }
    }
}
