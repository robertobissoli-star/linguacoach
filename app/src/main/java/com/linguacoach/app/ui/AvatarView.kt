package com.linguacoach.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.linguacoach.app.data.AVATAR_PRESETS
import com.linguacoach.app.data.Settings

/**
 * A cheap, lively 2D tutor avatar: a round custom photo (or an emoji preset) that
 * "breathes" when idle and radiates animated rings while the tutor is speaking.
 * No 3D engine, no rigging — just Compose animations.
 */
@Composable
fun TutorAvatar(settings: Settings, speaking: Boolean, sizeDp: Int = 96) {
    val transition = rememberInfiniteTransition(label = "avatar")
    // idle breathing
    val breathe by transition.animateFloat(
        initialValue = 1f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    // speaking pulse rings
    val ring by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring",
    )
    val bob by transition.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(260, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )

    val accent = Color(AVATAR_PRESETS[settings.avatarPreset.coerceIn(0, AVATAR_PRESETS.lastIndex)].second)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size((sizeDp * 1.6f).dp)) {
        if (speaking) {
            Canvas(Modifier.size((sizeDp * 1.6f).dp)) {
                val maxR = size.minDimension / 2f
                // two staggered expanding rings
                listOf(ring, (ring + 0.5f) % 1f).forEach { p ->
                    drawCircle(
                        color = accent.copy(alpha = (1f - p) * 0.35f),
                        radius = maxR * (0.55f + 0.45f * p),
                    )
                }
            }
        }
        val photo = remember(settings.avatar) { AvatarUtil.decode(settings.avatar) }
        val scale = if (speaking) bob else breathe
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                Image(photo.asImageBitmap(), "tutor", Modifier.size(sizeDp.dp).clip(CircleShape))
            } else {
                Text(
                    AVATAR_PRESETS[settings.avatarPreset.coerceIn(0, AVATAR_PRESETS.lastIndex)].first,
                    fontSize = (sizeDp * 0.5f).sp, fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
