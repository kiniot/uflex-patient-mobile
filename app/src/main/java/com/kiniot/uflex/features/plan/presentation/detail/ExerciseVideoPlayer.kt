package com.kiniot.uflex.features.plan.presentation.detail

import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kiniot.uflex.R

/**
 * Inline video player for an exercise. Builds an [ExoPlayer] tied to [videoUrl] and renders it
 * through a Media3 [PlayerView] that uses a TextureView surface (inflated from XML) — this avoids
 * the SurfaceView buffer-queue stalls seen on some devices and lets the video be clipped/rounded.
 * The player is paused when the screen is backgrounded and released when it leaves composition.
 */
@Composable
fun ExerciseVideoPlayer(
    videoUrl: String,
    playWhenReady: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(videoUrl, playWhenReady) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            this.playWhenReady = playWhenReady
            prepare()
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) exoPlayer.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(R.layout.view_exercise_player, null) as PlayerView).apply {
                player = exoPlayer
            }
        },
        onRelease = { playerView -> playerView.player = null }
    )
}
