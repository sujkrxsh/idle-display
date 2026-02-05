package com.example.idledisplay

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object MediaRepo {
    private val _mediaState = MutableStateFlow(MediaState())
    val mediaState = _mediaState.asStateFlow()

    private val _notificationState = MutableStateFlow<NotificationData?>(null)
    val notificationState = _notificationState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var dismissJob: Job? = null

    fun updateMedia(newState: MediaState) {
        // Prevent unnecessary updates if nothing changed
        if (_mediaState.value != newState) {
            _mediaState.value = newState
        }
    }

    fun postNotification(data: NotificationData) {
        dismissJob?.cancel()
        _notificationState.value = data
        dismissJob = scope.launch {
            delay(5000) // Show drawer for 5 seconds
            _notificationState.value = null
        }
    }
}

class MediaListenerService : NotificationListenerService() {
    private lateinit var mediaSessionManager: MediaSessionManager
    private lateinit var componentName: ComponentName
    private var currentController: MediaController? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        componentName = ComponentName(this, MediaListenerService::class.java)
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener({ controllers ->
                val best = pickBestController(controllers)
                registerCallback(best)
            }, componentName)
            registerCallback(pickBestController(mediaSessionManager.getActiveSessions(componentName)))
        } catch (e: SecurityException) { }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.isOngoing || sbn.notification.priority < Notification.PRIORITY_DEFAULT) return
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val icon = sbn.notification.getLargeIcon() ?: sbn.notification.smallIcon
        val bitmap = icon?.loadDrawable(this)?.toBitmap()
        MediaRepo.postNotification(NotificationData(title, text, bitmap, System.currentTimeMillis()))
    }

    private fun pickBestController(controllers: List<MediaController>?): MediaController? {
        if (controllers.isNullOrEmpty()) return null
        // Prioritize playing content
        return controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: controllers.first()
    }

    private fun registerCallback(newController: MediaController?) {
        if (newController == null) return
        currentController?.unregisterCallback(mediaCallback)
        currentController = newController
        currentController?.registerCallback(mediaCallback)
        updateMediaInfo(newController)
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) { currentController?.let { updateMediaInfo(it) } }
        override fun onPlaybackStateChanged(state: PlaybackState?) { currentController?.let { updateMediaInfo(it) } }
    }

    private fun updateMediaInfo(controller: MediaController) {
        val metadata = controller.metadata ?: return
        val playbackState = controller.playbackState

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).takeIf { it > 0 } ?: 1L
        val position = playbackState?.position ?: 0L

        // CRITICAL FIX: Dynamically check if playing.
        // If paused/stopped, this becomes false, triggering the Idle Clock.
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING

        val artBitmap = try { metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) } catch (e: Exception) { null }

        val pkg = controller.packageName
        val appName = try {
            val pm = applicationContext.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) { pkg }

        MediaRepo.updateMedia(MediaState(title, artist, appName, pkg, artBitmap, isPlaying, duration, position, System.currentTimeMillis()))
    }

    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable) return bitmap
        val bitmap = Bitmap.createBitmap(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }
}