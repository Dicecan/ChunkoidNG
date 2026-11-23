package com.noches.chunkoidng.ui.screens.console

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class CalabiyauEasterEggActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        enableEdgeToEdge()
        hideSystemBars()

        setContent {
            CalabiyauTerminationScreen(
                onConfirm = { finish() }
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
private fun CalabiyauTerminationScreen(onConfirm: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "calabiyau_pulse")
    val errorPulse by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "error_alpha"
    )

    val headlineRed = Color(0xFFFF3A5C)
    val bodyRed = Color(0xFFD64B62)
    val footerRed = Color(0xFFC03C52)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131720))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF17181E),
                        Color(0xFF1A1E28),
                        Color(0xFF131822)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x28FF2E55),
                        Color(0x0EFF2E55),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.24f, h * 0.42f),
                    radius = w * 0.48f
                )
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x243A4B66),
                        Color(0x10273348),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.84f, h * 0.46f),
                    radius = w * 0.45f
                )
            )

            rotate(degrees = -42f, pivot = Offset(w * 0.72f, h * 0.56f)) {
                drawRect(
                    color = Color(0x26293447),
                    topLeft = Offset(w * 0.48f, h * 0.16f),
                    size = Size(w * 0.44f, h * 0.68f)
                )
                drawRect(
                    color = Color(0x1F36445C),
                    topLeft = Offset(w * 0.54f, h * 0.24f),
                    size = Size(w * 0.32f, h * 0.46f)
                )
                drawRect(
                    color = Color(0x1C141923),
                    topLeft = Offset(w * 0.36f, h * 0.48f),
                    size = Size(w * 0.40f, h * 0.34f)
                )
            }

            val stripeStep = 14f
            var y = 0f
            while (y < h) {
                drawLine(
                    color = Color(0x09FFFFFF),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
                y += stripeStep
            }

            val arcCenter = Offset(w * 0.88f, h * 0.38f)
            for (i in 1..14) {
                drawCircle(
                    color = Color(0x0B8EA4C8),
                    radius = w * (0.14f + i * 0.036f),
                    center = arcCenter,
                    style = Stroke(width = 1.2f)
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .displayCutoutPadding()
        ) {
            val compact = maxHeight < 440.dp
            val horizontalStart = if (compact) 40.dp else 56.dp
            val topPad = if (compact) 20.dp else 40.dp
            val bottomPad = if (compact) 18.dp else 30.dp
            val emoticonSize = if (compact) 34.sp else 46.sp
            val titleSize = if (compact) 30.sp else 42.sp
            val titleLineHeight = if (compact) 35.sp else 48.sp
            val bodySize = if (compact) 12.5.sp else 14.5.sp
            val bodyLineHeight = if (compact) 18.sp else 22.sp
            val btnWidth = if (compact) 154.dp else 180.dp
            val btnHeight = if (compact) 34.dp else 40.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = horizontalStart,
                        end = 36.dp,
                        top = topPad,
                        bottom = bottomPad
                    )
            ) {
                Spacer(modifier = Modifier.weight(0.35f))

                Column {
                    Text(
                        text = ":(",
                        style = TextStyle(
                            color = headlineRed,
                            fontSize = emoticonSize,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            shadow = Shadow(
                                color = Color(0x66FF2952),
                                offset = Offset(0f, 0f),
                                blurRadius = 18f
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(if (compact) 6.dp else 10.dp))

                    Text(
                        text = "发现恶意引航者",
                        style = TextStyle(
                            color = headlineRed,
                            fontSize = titleSize,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            lineHeight = titleLineHeight,
                            shadow = Shadow(
                                color = Color(0x66FF2952),
                                offset = Offset(0f, 0f),
                                blurRadius = 20f
                            )
                        )
                    )

                    Text(
                        text = "战局强制关闭",
                        style = TextStyle(
                            color = headlineRed,
                            fontSize = titleSize,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            lineHeight = titleLineHeight,
                            shadow = Shadow(
                                color = Color(0x66FF2952),
                                offset = Offset(0f, 0f),
                                blurRadius = 20f
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(if (compact) 8.dp else 14.dp))

                    Text(
                        text = "检测到恶意引航者，为了保证正确将卡拉彼丘导向正常，\n你所在的推演战局已被终止，本局胜负将不会录入正式记录。",
                        style = TextStyle(
                            color = bodyRed,
                            fontSize = bodySize,
                            fontWeight = FontWeight.Medium,
                            lineHeight = bodyLineHeight,
                            letterSpacing = 0.4.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(if (compact) 14.dp else 22.dp))

                    TacticalConfirmButton(
                        text = "确认",
                        onClick = onConfirm,
                        modifier = Modifier
                            .width(btnWidth)
                            .height(btnHeight)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 2.dp, top = 8.dp)
                ) {
                    TacticalErrorReticle(
                        color = footerRed.copy(alpha = errorPulse),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "System operation: error",
                        style = TextStyle(
                            color = footerRed.copy(alpha = errorPulse),
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.9.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TacticalConfirmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val baseColor = if (pressed) Color(0xFFC92140) else Color(0xFFE92C4E)
    val highlightColor = if (pressed) Color(0xFFD92B4B) else Color(0xFFF63B5D)

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cut = 6.dp.toPx()

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(baseColor, highlightColor)
                ),
                size = size
            )

            drawRect(
                color = Color(0xCCFFFFFF),
                topLeft = Offset(4.dp.toPx(), 6.dp.toPx()),
                size = Size(1.6.dp.toPx(), h - 12.dp.toPx())
            )

            val topRightCut = Path().apply {
                moveTo(w - cut, 0f)
                lineTo(w, 0f)
                lineTo(w, cut)
                close()
            }
            drawPath(topRightCut, color = Color(0xFF171B24))

            val bottomRightCut = Path().apply {
                moveTo(w - cut, h)
                lineTo(w, h)
                lineTo(w, h - cut)
                close()
            }
            drawPath(bottomRightCut, color = Color(0xFF171B24))
        }

        Text(
            text = text,
            style = TextStyle(
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        )
    }
}

@Composable
private fun TacticalErrorReticle(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val len = w * 0.30f
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Square)

        drawLine(color, Offset(0f, 0f), Offset(len, 0f), strokeWidth = stroke.width)
        drawLine(color, Offset(0f, 0f), Offset(0f, len), strokeWidth = stroke.width)

        drawLine(color, Offset(w - len, 0f), Offset(w, 0f), strokeWidth = stroke.width)
        drawLine(color, Offset(w, 0f), Offset(w, len), strokeWidth = stroke.width)

        drawLine(color, Offset(0f, h - len), Offset(0f, h), strokeWidth = stroke.width)
        drawLine(color, Offset(0f, h), Offset(len, h), strokeWidth = stroke.width)

        drawLine(color, Offset(w - len, h), Offset(w, h), strokeWidth = stroke.width)
        drawLine(color, Offset(w, h - len), Offset(w, h), strokeWidth = stroke.width)

        val dot = w * 0.24f
        drawRect(
            color = color,
            topLeft = Offset((w - dot) / 2f, (h - dot) / 2f),
            size = Size(dot, dot)
        )
    }
}
