package com.homeremote.roku

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

val ChristmasRed = Color(0xFFD62839)
val ChristmasGreen = Color(0xFF1E8E4E)
val ChristmasGold = Color(0xFFF2C14E)

private val NightTop = Color(0xFF0A1630)
private val NightMid = Color(0xFF14284A)
private val PineDark = Color(0xFF0D3B2A)

private class Flake(val x: Float, val start: Float, val speed: Float, val radius: Float, val sway: Float, val phase: Float)
private class Star(val x: Float, val y: Float, val radius: Float, val phase: Float)

/** A snowy Christmas night behind the whole app: stars, falling snow, hills and pine trees. */
@Composable
fun ChristmasBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val flakes = remember {
        val r = Random(25)
        List(80) {
            Flake(
                x = r.nextFloat(),
                start = r.nextFloat(),
                speed = 0.6f + r.nextFloat() * 1.2f,
                radius = 1.2f + r.nextFloat() * 2.8f,
                sway = 6f + r.nextFloat() * 16f,
                phase = r.nextFloat() * 2f * PI.toFloat(),
            )
        }
    }
    val stars = remember {
        val r = Random(12)
        List(45) { Star(r.nextFloat(), r.nextFloat() * 0.55f, 0.8f + r.nextFloat() * 1.6f, r.nextFloat() * 2f * PI.toFloat()) }
    }
    val time = rememberInfiniteTransition(label = "snow")
    // One slow loop; each flake moves at its own speed within it.
    val t by time.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )

    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(NightTop, NightMid, PineDark)))
            val angle = t * 2f * PI.toFloat()
            for (s in stars) {
                val twinkle = 0.35f + 0.65f * ((sin(angle * 6f + s.phase) + 1f) / 2f)
                drawCircle(Color.White.copy(alpha = twinkle * 0.8f), s.radius.dp.toPx() / 2f, Offset(s.x * size.width, s.y * size.height))
            }
            drawHillsAndTrees()
            for (f in flakes) {
                // speed is in screen-heights per loop, rounded so the loop restarts seamlessly.
                val loops = kotlin.math.round(f.speed * 3f)
                val y = ((f.start + t * loops) % 1f) * (size.height + 20f) - 10f
                val x = f.x * size.width + sin(angle * loops + f.phase) * f.sway.dp.toPx()
                drawCircle(Color.White.copy(alpha = 0.85f), f.radius.dp.toPx() / 2f + 0.5f, Offset(x, y))
            }
        }
        content()
    }
}

private fun DrawScope.drawHillsAndTrees() {
    val w = size.width
    val h = size.height
    val back = Path().apply {
        moveTo(0f, h * 0.84f)
        cubicTo(w * 0.25f, h * 0.78f, w * 0.55f, h * 0.88f, w, h * 0.80f)
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(back, Color(0xFFCFDDEB).copy(alpha = 0.55f))

    val trees = listOf(0.08f to 0.82f, 0.2f to 0.86f, 0.74f to 0.84f, 0.88f to 0.80f, 0.95f to 0.85f)
    for ((tx, base) in trees) {
        val cx = w * tx
        val bottom = h * base
        val tall = h * 0.11f
        val half = tall * 0.38f
        for (layer in 0 until 3) {
            val top = bottom - tall + layer * tall * 0.28f
            val wide = half * (0.6f + layer * 0.25f)
            val tri = Path().apply {
                moveTo(cx, top)
                lineTo(cx + wide, top + tall * 0.42f)
                lineTo(cx - wide, top + tall * 0.42f)
                close()
            }
            drawPath(tri, Color(0xFF0B4D33))
            // Snow on each branch tip.
            drawLine(Color.White.copy(alpha = 0.7f), Offset(cx - wide * 0.85f, top + tall * 0.40f), Offset(cx + wide * 0.85f, top + tall * 0.40f), strokeWidth = 2.dp.toPx())
        }
        drawCircle(ChristmasGold, 3.dp.toPx(), Offset(cx, bottom - tall))
    }

    val front = Path().apply {
        moveTo(0f, h * 0.9f)
        cubicTo(w * 0.3f, h * 0.86f, w * 0.7f, h * 0.95f, w, h * 0.88f)
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(front, Color(0xFFF2F6FA).copy(alpha = 0.8f))
}

/** A string of twinkling Christmas lights. */
@Composable
fun ChristmasLights(modifier: Modifier = Modifier) {
    val time = rememberInfiniteTransition(label = "lights")
    val t by time.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_000, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
    val bulbs = listOf(ChristmasRed, ChristmasGreen, ChristmasGold, Color(0xFF4FA3FF))
    Canvas(modifier.fillMaxWidth().height(22.dp)) {
        val count = 14
        val gap = size.width / count
        val sag = 7.dp.toPx()
        val wire = Path().apply {
            moveTo(0f, 2.dp.toPx())
            for (i in 0 until count) {
                val x0 = i * gap
                quadraticBezierTo(x0 + gap / 2f, 2.dp.toPx() + sag * 2f, x0 + gap, 2.dp.toPx())
            }
        }
        drawPath(wire, Color(0xFF2B3A2E), style = Stroke(1.5.dp.toPx()))
        for (i in 0 until count) {
            val x = i * gap + gap / 2f
            val y = 2.dp.toPx() + sag + 4.dp.toPx()
            val color = bulbs[i % bulbs.size]
            val on = ((sin((t + i * 0.37f) * 2f * PI.toFloat()) + 1f) / 2f)
            drawCircle(color.copy(alpha = 0.25f * on), 8.dp.toPx(), Offset(x, y))
            drawCircle(color.copy(alpha = 0.55f + 0.45f * on), 4.dp.toPx(), Offset(x, y))
        }
    }
}
