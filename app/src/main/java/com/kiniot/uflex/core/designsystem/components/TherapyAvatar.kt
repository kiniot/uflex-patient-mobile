package com.kiniot.uflex.core.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * What the avatar should animate, derived by the caller from the exercise's body part + movement
 * type. Keeps this component decoupled from the `plan`/`device` feature enums (same contract style
 * as [RadialGauge]: presentational, primitives in).
 */
enum class AvatarPose { ElbowFlexion, WristFlexion, ForearmPronation, ForearmSupination, Rest }

/** Default warm, human skin tone used when the caller doesn't override it. */
val DefaultAvatarSkin = Color(0xFFE7B393)

// Fixed presentation angles (degrees, measured from "straight down") so flexion/extension and the
// axial pron/sup movements read clearly in the 2D plane.
private const val UPPER_ARM_ANGLE = 20f          // upper arm hangs down, slightly outward
private const val WRIST_POSE_ELBOW = 78f         // elbow held bent for wrist exercises
private const val ROLL_POSE_ELBOW = 92f          // elbow held ~90° for pron/sup
private const val ELBOW_MAX_VISUAL = 150f         // visual clamp of the forearm swing
private const val WRIST_MAX_VISUAL = 80f          // visual clamp of the hand swing
private const val PRON_SUP_CLINICAL_MAX = 90f     // clinical pron/sup range mapped onto a full flip

/**
 * A flat/material 2D human figure (waist-up, front-facing) whose therapy arm articulates the elbow
 * and wrist to mirror the live movement. Purely presentational: [degrees] is the calibrated flexion
 * of the active joint (null → resting pose + em dash), [pose] selects which joint/axis animates,
 * [jointGlowColor] is the already-resolved kit-LED accent shown as a halo on the active joint, and
 * [buzzerActive] triggers a vibration pulse near the hand. Callers own all gating (calibration /
 * connection); this component never re-derives it.
 */
@Composable
fun TherapyAvatar(
    degrees: Float?,
    pose: AvatarPose,
    jointGlowColor: Color,
    buzzerActive: Boolean,
    modifier: Modifier = Modifier,
    skinColor: Color = DefaultAvatarSkin,
    clothingColor: Color = MaterialTheme.colorScheme.primary,
    hairColor: Color = Color(0xFF473A34),
    valueLabel: (Int) -> String
) {
    val isActive = degrees != null

    // Smooth but responsive: a critically-damped spring settles fast without ringing. The firmware
    // already low-pass-filters the angle at ~10 Hz, so a small snap-threshold (below) plus the
    // spring is enough to kill residual jitter without adding perceptible lag.
    val animatedDegrees by animateFloatAsState(
        targetValue = (degrees ?: 0f),
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "avatarAngle"
    )
    // Fade the whole figure between "resting/idle" and "live" so losing the signal reads gracefully.
    val liveness by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "avatarLiveness"
    )

    val buzzerTransition = rememberInfiniteTransition(label = "buzzer")
    val pulse by buzzerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Restart),
        label = "buzzerPulse"
    )

    // Resolve every color up-front — DrawScope can't read the composition locals.
    val skinShadow = lerp(skinColor, Color.Black, 0.16f)
    val skinHighlight = lerp(skinColor, Color.White, 0.22f)
    val clothingShadow = lerp(clothingColor, Color.Black, 0.20f)
    val clothingHighlight = lerp(clothingColor, Color.White, 0.14f)
    val contactShadow = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val neutralGlow = MaterialTheme.colorScheme.outlineVariant
    val glow = lerp(neutralGlow, jointGlowColor, liveness)
    val restLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val liveLabelColor = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAvatar(
                pose = pose,
                degrees = animatedDegrees,
                liveness = liveness,
                skin = skinColor,
                skinShadow = skinShadow,
                skinHighlight = skinHighlight,
                clothing = clothingColor,
                clothingShadow = clothingShadow,
                clothingHighlight = clothingHighlight,
                hair = hairColor,
                contactShadow = contactShadow,
                glow = glow,
                buzzerActive = buzzerActive,
                pulse = pulse
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ) {
            Text(
                text = if (degrees != null) valueLabel(degrees.toInt()) else "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (degrees != null) liveLabelColor else restLabelColor,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }
    }
}

/** Rotate a vector of [len] that points "straight down" by [angleFromDownDeg] (+ sweeps toward +x). */
private fun project(base: Offset, angleFromDownDeg: Float, len: Float): Offset {
    val r = Math.toRadians(angleFromDownDeg.toDouble())
    return Offset(base.x + (sin(r) * len).toFloat(), base.y + (cos(r) * len).toFloat())
}

private fun DrawScope.drawAvatar(
    pose: AvatarPose,
    degrees: Float,
    liveness: Float,
    skin: Color,
    skinShadow: Color,
    skinHighlight: Color,
    clothing: Color,
    clothingShadow: Color,
    clothingHighlight: Color,
    hair: Color,
    contactShadow: Color,
    glow: Color,
    buzzerActive: Boolean,
    pulse: Float
) {
    val w = size.width
    val h = size.height

    val cx = w * 0.45f                 // shift the body slightly left so the active (right) arm fits
    val headR = w * 0.13f              // bigger head → a stockier, less elongated figure
    val headCy = h * 0.20f
    val shoulderY = h * 0.42f
    val shoulderHalfW = w * 0.25f      // broader shoulders
    val limbW = w * 0.145f             // thicker limbs
    val depth = w * 0.02f

    val leftShoulder = Offset(cx - shoulderHalfW, shoulderY)
    val rightShoulder = Offset(cx + shoulderHalfW, shoulderY)

    // ---- soft contact shadow under the figure --------------------------------------------------
    drawOval(
        color = contactShadow,
        topLeft = Offset(cx - w * 0.30f, h * 0.93f),
        size = androidx.compose.ui.geometry.Size(w * 0.60f, h * 0.06f)
    )

    // ---- torso (clothing) ----------------------------------------------------------------------
    val torso = torsoPath(cx, shoulderY, shoulderHalfW, h)
    translate(0f, depth) { drawPath(torso, clothingShadow) }
    drawPath(torso, clothing)
    // collar highlight
    drawPath(collarPath(cx, shoulderY, shoulderHalfW), clothingHighlight)

    // ---- resting (left) arm, drawn behind, relaxed downward ------------------------------------
    val restElbow = project(leftShoulder, -12f, h * 0.165f)
    val restWrist = project(restElbow, -6f, h * 0.155f)
    drawLimb(leftShoulder, restElbow, limbW, skin, skinShadow, skinHighlight, depth)
    drawLimb(restElbow, restWrist, limbW * 0.9f, skin, skinShadow, skinHighlight, depth)
    drawHand(restWrist, -6f, 0f, limbW, skin, skinShadow)

    // ---- neck + head ---------------------------------------------------------------------------
    // Short, sturdy neck: the limb starts inside the head so only a little neck shows.
    val neckTop = Offset(cx, headCy + headR * 0.45f)
    drawLimb(neckTop, Offset(cx, shoulderY), limbW * 1.05f, skin, skinShadow, skinHighlight, depth)
    translate(0f, depth) { drawCircle(skinShadow, headR, Offset(cx, headCy)) }
    drawCircle(skin, headR, Offset(cx, headCy))
    // hair cap
    drawHair(cx, headCy, headR, hair)

    // ---- active (right) arm --------------------------------------------------------------------
    val upperLen = h * 0.165f
    val foreLen = h * 0.155f

    val elbowAngle: Float
    val foreAngle: Float
    // Signed visual axial rotation of the hand in degrees (pron/sup). 0 = palm on, ±90 = edge-on,
    // ±180 = back of hand. The clinical range (~0..90°) is amplified onto a full flip so the axial
    // movement — otherwise near-invisible from the front — reads clearly.
    val handAxialDeg: Float
    val activeJoint: Offset

    when (pose) {
        AvatarPose.ElbowFlexion -> {
            val flex = degrees.coerceIn(0f, ELBOW_MAX_VISUAL)
            elbowAngle = UPPER_ARM_ANGLE
            foreAngle = UPPER_ARM_ANGLE - flex
            handAxialDeg = 0f
            activeJoint = project(rightShoulder, elbowAngle, upperLen)
        }
        AvatarPose.WristFlexion -> {
            val flex = degrees.coerceIn(0f, WRIST_MAX_VISUAL)
            elbowAngle = UPPER_ARM_ANGLE
            foreAngle = UPPER_ARM_ANGLE - WRIST_POSE_ELBOW
            handAxialDeg = 0f
            activeJoint = project(project(rightShoulder, elbowAngle, upperLen), foreAngle, foreLen)
            // wrist swing handled below via handSwing
        }
        AvatarPose.ForearmPronation, AvatarPose.ForearmSupination -> {
            elbowAngle = UPPER_ARM_ANGLE
            foreAngle = UPPER_ARM_ANGLE - ROLL_POSE_ELBOW
            val sign = if (pose == AvatarPose.ForearmSupination) 1f else -1f
            handAxialDeg = sign * (degrees.coerceIn(0f, PRON_SUP_CLINICAL_MAX) / PRON_SUP_CLINICAL_MAX) * 180f
            activeJoint = project(project(rightShoulder, elbowAngle, upperLen), foreAngle, foreLen)
        }
        AvatarPose.Rest -> {
            elbowAngle = UPPER_ARM_ANGLE
            foreAngle = UPPER_ARM_ANGLE
            handAxialDeg = 0f
            activeJoint = project(rightShoulder, elbowAngle, upperLen)
        }
    }

    val elbow = project(rightShoulder, elbowAngle, upperLen)
    val wrist = project(elbow, foreAngle, foreLen)
    val handSwing = if (pose == AvatarPose.WristFlexion) {
        foreAngle - degrees.coerceIn(0f, WRIST_MAX_VISUAL)
    } else {
        foreAngle
    }

    // glow behind the active joint
    drawJointGlow(activeJoint, limbW * 2.4f, glow, liveness)

    drawLimb(rightShoulder, elbow, limbW, skin, skinShadow, skinHighlight, depth)
    drawLimb(elbow, wrist, limbW * 0.92f, skin, skinShadow, skinHighlight, depth)
    drawHand(wrist, handSwing + 180f, handAxialDeg, limbW, skin, skinShadow)

    // small joint caps so the articulation reads as a joint, not a bend
    drawCircle(skinHighlight, limbW * 0.34f, elbow)
    drawCircle(skinHighlight, limbW * 0.30f, wrist)

    // ---- buzzer vibration pulse near the hand --------------------------------------------------
    if (buzzerActive) {
        val ringBase = limbW * 0.9f
        for (i in 0..1) {
            val t = (pulse + i * 0.5f) % 1f
            drawCircle(
                color = glow.copy(alpha = (1f - t) * 0.5f),
                radius = ringBase + ringBase * 1.6f * t,
                center = wrist,
                style = Stroke(width = w * 0.012f)
            )
        }
    }
}

/** A rounded, slightly tapered torso path from the shoulders down to the bottom edge. */
private fun torsoPath(cx: Float, shoulderY: Float, shoulderHalfW: Float, h: Float): Path {
    val waistHalf = shoulderHalfW * 0.9f
    val bottom = h
    val neckHalf = shoulderHalfW * 0.34f
    val neckY = shoulderY - h * 0.03f
    return Path().apply {
        moveTo(cx - neckHalf, neckY)
        // left shoulder
        quadraticBezierTo(cx - shoulderHalfW, shoulderY - h * 0.01f, cx - shoulderHalfW, shoulderY + h * 0.02f)
        lineTo(cx - waistHalf, bottom)
        lineTo(cx + waistHalf, bottom)
        lineTo(cx + shoulderHalfW, shoulderY + h * 0.02f)
        // right shoulder
        quadraticBezierTo(cx + shoulderHalfW, shoulderY - h * 0.01f, cx + neckHalf, neckY)
        close()
    }
}

/** A soft collar highlight to give the shirt a bit of depth around the neckline. */
private fun collarPath(cx: Float, shoulderY: Float, shoulderHalfW: Float): Path {
    val neckHalf = shoulderHalfW * 0.34f
    return Path().apply {
        moveTo(cx - neckHalf, shoulderY - shoulderHalfW * 0.14f)
        quadraticBezierTo(cx, shoulderY + shoulderHalfW * 0.22f, cx + neckHalf, shoulderY - shoulderHalfW * 0.14f)
        quadraticBezierTo(cx, shoulderY + shoulderHalfW * 0.06f, cx - neckHalf, shoulderY - shoulderHalfW * 0.14f)
        close()
    }
}

/** A limb segment as a rounded capsule with an offset shadow beneath and a thin top highlight. */
private fun DrawScope.drawLimb(
    from: Offset,
    to: Offset,
    width: Float,
    color: Color,
    shadow: Color,
    highlight: Color,
    depth: Float
) {
    drawLine(shadow, from.plus(Offset(0f, depth)), to.plus(Offset(0f, depth)), width, StrokeCap.Round)
    drawLine(color, from, to, width, StrokeCap.Round)
    drawLine(highlight, from, to, width * 0.28f, StrokeCap.Round)
}

/**
 * The hand, drawn in a frame aligned to the forearm ([alongAngleDeg]) so [axialDeg] can rotate it
 * about the forearm's long axis. Three cues make that axial (pronation/supination) rotation legible
 * from the front:
 *  - a "coin flip": the apparent width scales by |cos(axialDeg)|, so the hand turns edge-on and back;
 *  - palm/back two-tone: the lighter palm shows while facing the viewer, the darker back once flipped;
 *  - an asymmetric shape (palm slab + finger stubs + a thumb that crosses sides with the rotation).
 */
private fun DrawScope.drawHand(
    wrist: Offset,
    alongAngleDeg: Float,
    axialDeg: Float,
    limbW: Float,
    skin: Color,
    skinShadow: Color,
    mirror: Boolean = false
) {
    val handLen = limbW * 1.5f
    val handW = limbW * 1.02f
    val axialRad = Math.toRadians(axialDeg.toDouble())
    val cosA = cos(axialRad).toFloat()

    val facingPalm = cosA >= 0f
    val face = if (facingPalm) skin else lerp(skin, Color.Black, 0.16f)
    val faceShadow = if (facingPalm) skinShadow else lerp(skin, Color.Black, 0.30f)
    val palmW = handW * abs(cosA)          // apparent width (coin-flip foreshortening)

    val top = handLen * 0.14f
    val bottom = handLen * 0.74f
    val palmH = bottom - top

    withTransform({
        translate(wrist.x, wrist.y)
        rotate(alongAngleDeg, pivot = Offset.Zero)
        // Mirror across the forearm axis for the right (active) arm so the thumb/asymmetry sit on the
        // medial side — otherwise the shared hand shape reads as a left hand on a right arm.
        if (mirror) scale(-1f, 1f, pivot = Offset.Zero)
    }) {
        // thumb: a point on the wrist "cylinder" 90° from the palm normal, so it sits at the palm
        // edge when palm-on and crosses to the other side as the forearm rotates.
        val thumbX = cosA * handW * 0.5f
        drawOval(
            color = faceShadow,
            topLeft = Offset(thumbX - handW * 0.16f, top + palmH * 0.12f),
            size = Size(handW * 0.32f, handLen * 0.34f)
        )

        if (palmW > 0.5f) {
            drawRoundRect(
                color = face,
                topLeft = Offset(-palmW / 2f, top),
                size = Size(palmW, palmH),
                cornerRadius = CornerRadius(palmW * 0.42f, palmW * 0.42f)
            )
            // finger stubs across the (foreshortened) far edge
            val fingers = 4
            val fingerH = handLen * 0.22f
            val fw = palmW / fingers
            for (i in 0 until fingers) {
                val fx = -palmW / 2f + fw * i + fw * 0.12f
                drawRoundRect(
                    color = face,
                    topLeft = Offset(fx, bottom - fingerH * 0.25f),
                    size = Size(fw * 0.76f, fingerH),
                    cornerRadius = CornerRadius(fw * 0.4f, fw * 0.4f)
                )
            }
            // a palm crease / knuckle hint so palm and back read differently
            drawLine(
                color = faceShadow,
                start = Offset(-palmW * 0.3f, top + palmH * 0.52f),
                end = Offset(palmW * 0.3f, top + palmH * 0.52f),
                strokeWidth = handW * 0.06f,
                cap = StrokeCap.Round
            )
        }
    }
}

/** A radial glow disc centered on the active joint, tinted by the kit-LED accent. */
private fun DrawScope.drawJointGlow(center: Offset, radius: Float, color: Color, liveness: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.35f * (0.4f + 0.6f * liveness)), color.copy(alpha = 0f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/**
 * A short, styled haircut: a rounded, voluminous crown over the top of the head plus a soft,
 * side-swept fringe across the forehead (rather than a flat semicircle).
 */
private fun DrawScope.drawHair(cx: Float, cy: Float, r: Float, hair: Color) {
    val path = Path().apply {
        moveTo(cx - r * 0.96f, cy - r * 0.12f)
        // voluminous crown sweeping over the top of the head
        cubicTo(
            cx - r * 1.18f, cy - r * 1.32f,
            cx + r * 1.18f, cy - r * 1.32f,
            cx + r * 0.96f, cy - r * 0.12f
        )
        // side-swept fringe curving back across the forehead
        cubicTo(
            cx + r * 0.52f, cy - r * 0.5f,
            cx + r * 0.06f, cy - r * 0.16f,
            cx - r * 0.22f, cy - r * 0.3f
        )
        cubicTo(
            cx - r * 0.5f, cy - r * 0.42f,
            cx - r * 0.78f, cy - r * 0.34f,
            cx - r * 0.96f, cy - r * 0.12f
        )
        close()
    }
    drawPath(path, hair)
}

// ---- Previews ----------------------------------------------------------------------------------

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 220, heightDp = 260)
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true, widthDp = 220, heightDp = 260,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun TherapyAvatarPreview() {
    com.kiniot.uflex.core.designsystem.theme.UFlexTheme {
        Surface {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                TherapyAvatar(
                    degrees = 95f,
                    pose = AvatarPose.ElbowFlexion,
                    jointGlowColor = MaterialTheme.colorScheme.tertiary,
                    buzzerActive = false,
                    modifier = Modifier.size(150.dp, 220.dp),
                    valueLabel = { "$it°" }
                )
                TherapyAvatar(
                    degrees = 55f,
                    pose = AvatarPose.ForearmSupination,
                    jointGlowColor = MaterialTheme.colorScheme.error,
                    buzzerActive = true,
                    modifier = Modifier.size(150.dp, 220.dp),
                    valueLabel = { "$it°" }
                )
                TherapyAvatar(
                    degrees = null,
                    pose = AvatarPose.Rest,
                    jointGlowColor = MaterialTheme.colorScheme.outline,
                    buzzerActive = false,
                    modifier = Modifier.size(150.dp, 220.dp),
                    valueLabel = { "$it°" }
                )
            }
        }
    }
}
