package com.jmrc.jaboard.media

import android.content.ComponentName
import android.media.MediaMetadata
import android.graphics.Bitmap
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

 data class PlaybackInfo(val title: String = "", val artist: String = "", val playing: Boolean = false, val connected: Boolean = false, val positionMs: Long = 0L, val durationMs: Long = 0L, val artwork: Bitmap? = null)

class JaBoardMediaNotificationListener : NotificationListenerService() {
    private var controllers: List<MediaController> = emptyList()
    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) = publish()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refresh()
    }
    override fun onListenerDisconnected() {
        controllers.forEach { runCatching { it.unregisterCallback(callback) } }
        controllers = emptyList()
        cachedController = null
        current = PlaybackInfo()
        super.onListenerDisconnected()
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) { refresh() }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) { refresh() }

    private fun refresh() {
        val manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val sessions = runCatching { manager.getActiveSessions(ComponentName(this, JaBoardMediaNotificationListener::class.java)) }.getOrDefault(emptyList())
        controllers.forEach { runCatching { it.unregisterCallback(callback) } }
        controllers = sessions
        cachedController = sessions.firstOrNull()
        controllers.forEach { runCatching { it.registerCallback(callback) } }
        publish()
    }
    private fun publish() {
        val controller = controllers.firstOrNull()
        val metadata = controller?.metadata
        val state = controller?.playbackState
        current = if (controller == null) PlaybackInfo() else PlaybackInfo(
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
            playing = state?.state == android.media.session.PlaybackState.STATE_PLAYING,
            connected = true,
            positionMs = state?.position?.coerceAtLeast(0L) ?: 0L,
            durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L,
            artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        )
    }

    companion object {
        private val _playback = MutableStateFlow(PlaybackInfo())
        val playback = _playback.asStateFlow()
        private var current: PlaybackInfo
            get() = _playback.value
            set(value) { _playback.value = value }
        fun playPause() {
            active()?.let { if (it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING) it.transportControls.pause() else it.transportControls.play() }
        }
        fun next() { active()?.transportControls?.skipToNext() }
        fun previous() { active()?.transportControls?.skipToPrevious() }
        fun seekTo(positionMs: Long) { active()?.transportControls?.seekTo(positionMs.coerceAtLeast(0L)) }
        private fun active(): MediaController? = _playback.value.takeIf { it.connected }?.let { cachedController }
        @Volatile private var cachedController: MediaController? = null
    }

    private fun updateControllerReference() { cachedController = controllers.firstOrNull() }
}
