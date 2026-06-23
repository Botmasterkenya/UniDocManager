package com.example.mylibrary.ui.splash

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val NetflixRed   = Color(0xFFE50914)
private val DarkBg       = Color(0xFF141414)
private val SubtitleGray = Color(0xFFE0E0E0)
private val TaglineGray  = Color(0xFF888888)

@Composable
fun SplashScreen(onSplashFinished: () -> Unit = {}) {

    val context = LocalContext.current
    var startAnim by remember { mutableStateOf(false) }

    // ── Sound effect ──────────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        val resId = context.resources.getIdentifier("splash_sound", "raw", context.packageName)
        val player = if (resId != 0) {
            try {
                MediaPlayer.create(context, resId)?.apply { start() }
            } catch (e: Exception) { null }
        } else null

        onDispose {
            player?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
    }

    // ── Animations ────────────────────────────────────────────────────────────
    val logoScale by animateFloatAsState(
        targetValue   = if (startAnim) 1f else 0.6f,
        animationSpec = keyframes {
            durationMillis = 1000
            0.6f  at 0   with FastOutSlowInEasing
            1.08f at 600 with FastOutLinearInEasing
            1.0f  at 1000
        },
        label = "logoScale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue   = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "logoAlpha"
    )

    val subtitleOffsetY by animateFloatAsState(
        targetValue   = if (startAnim) 0f else 20f,
        animationSpec = tween(durationMillis = 800, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "subtitleOffsetY"
    )

    val subtitleAlpha by animateFloatAsState(
        targetValue   = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 400, easing = LinearEasing),
        label = "subtitleAlpha"
    )

    val barWidthFraction by animateFloatAsState(
        targetValue   = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 700, delayMillis = 600, easing = FastOutSlowInEasing),
        label = "barWidth"
    )

    val barAlpha by animateFloatAsState(
        targetValue   = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 600, easing = LinearEasing),
        label = "barAlpha"
    )

    val taglineAlpha by animateFloatAsState(
        targetValue   = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 1000, easing = LinearEasing),
        label = "taglineAlpha"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(2600L)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                        radius = 1200f
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {

            Text(
                text          = "Tee's Library",
                color         = NetflixRed,
                fontSize      = 60.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign     = TextAlign.Center,
                modifier      = Modifier.graphicsLayer {
                    scaleX = logoScale
                    scaleY = logoScale
                    alpha  = logoAlpha
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text          = "READ ANYWHERE",
                color         = SubtitleGray,
                fontSize      = 16.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 6.sp,
                textAlign     = TextAlign.Center,
                modifier      = Modifier.graphicsLayer {
                    translationY = subtitleOffsetY
                    alpha        = subtitleAlpha
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .width(60.dp * barWidthFraction)
                    .height(4.dp)
                    .background(
                        color = NetflixRed.copy(alpha = barAlpha),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text          = "DISCOVER EVERYTHING  ·  READ ANYWHERE",
                color         = TaglineGray.copy(alpha = taglineAlpha),
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Normal,
                letterSpacing = 3.sp,
                textAlign     = TextAlign.Center
            )
        }
    }
}