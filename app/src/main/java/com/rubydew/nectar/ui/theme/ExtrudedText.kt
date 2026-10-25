package com.rubydew.nectar.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bold lettering with a 3D look: a soft drop shadow, stacked "extrusion" layers,
 * a dark outline and a light-to-dark gradient face.
 */
@Composable
fun ExtrudedText(
    text: String,
    fontSize: TextUnit,
    face: Color,
    modifier: Modifier = Modifier,
    faceHighlight: Color = Color.White,
    outline: Color = Color(0xFF1A1A1A),
    depthColor: Color = Color(0xFF2B0A0E),
    depth: Dp = 4.dp,
    letterSpacing: TextUnit = 1.sp,
) {
    val base = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Black, letterSpacing = letterSpacing)
    val outlineWidth = with(LocalDensity.current) { (fontSize.value / 9f).coerceAtLeast(2f).dp.toPx() }
    val layers = 4

    Box(modifier) {
        // Soft shadow cast on the background
        Text(
            text,
            style = base.copy(
                color = Color.Transparent,
                shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 0f), blurRadius = 14f),
            ),
            modifier = Modifier.offset(depth + 2.dp, depth + 3.dp),
        )
        // Extrusion: stacked copies stepping down-right
        for (i in layers downTo 1) {
            val step = depth * i / layers
            Text(
                text,
                style = base.copy(color = depthColor, drawStyle = Stroke(width = outlineWidth, join = StrokeJoin.Round)),
                modifier = Modifier.offset(step, step),
            )
            Text(text, style = base.copy(color = depthColor), modifier = Modifier.offset(step, step))
        }
        // Outline around the face
        Text(text, style = base.copy(color = outline, drawStyle = Stroke(width = outlineWidth, join = StrokeJoin.Round)))
        // Face with a light-to-dark gradient, like light hitting raised letters
        Text(text, style = base.merge(TextStyle(brush = Brush.verticalGradient(listOf(faceHighlight, face, face)))))
    }
}
