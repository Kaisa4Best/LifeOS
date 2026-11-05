package com.lifeos.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val SplashInk = Color(0xFF090A12)
private val SplashViolet = Color(0xFF9B7BFF)
private val SplashCyan = Color(0xFF55D6D0)

@Composable
fun LifeOSLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.07f
        val ring = Brush.sweepGradient(listOf(SplashViolet, SplashCyan, SplashViolet), center = center)
        drawArc(
            brush = ring, startAngle = -60f, sweepAngle = 300f, useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2), size = Size(s - stroke, s - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        val l = Path().apply {
            moveTo(s * 0.38f, s * 0.28f); lineTo(s * 0.38f, s * 0.68f); lineTo(s * 0.66f, s * 0.68f)
        }
        drawPath(
            l,
            brush = Brush.linearGradient(
                listOf(SplashViolet, SplashCyan),
                start = Offset(s * 0.38f, s * 0.28f), end = Offset(s * 0.66f, s * 0.68f)
            ),
            style = Stroke(s * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(SplashCyan, s * 0.055f, Offset(center.x, stroke / 2))
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (started) 1f else 0f, tween(700, easing = FastOutSlowInEasing), label = "a")
    val scale by animateFloatAsState(if (started) 1f else 0.85f, tween(700, easing = FastOutSlowInEasing), label = "s")
    LaunchedEffect(Unit) {
        started = true
        delay(2200)
        onFinished()
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(SplashInk, Color(0xFF141226), SplashInk))
        )
    ) {
        Column(
            Modifier.align(Alignment.Center).alpha(alpha).scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LifeOSLogo(Modifier.size(132.dp))
            Spacer(Modifier.height(28.dp))
            Text("LIFEOS", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 8.sp)
        }
        Text(
            "Powered by Kaisa4Best",
            color = Color(0xFFB5B8C8), fontSize = 13.sp, letterSpacing = 1.5.sp,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 36.dp).alpha(alpha)
        )
    }
}
