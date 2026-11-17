package app.ash.launcher.player

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlayer(private val context: Context) {

    data class Track(
        val id: Long,
        val title: String,
        val artist: String,
        val durationMs: Long,
        val uri: Uri,
    )

    data class Snapshot(
        val loading: Boolean,
        val isPlaying: Boolean,
        val error: Boolean,
        val title: String,
        val artist: String,
        val positionMs: Long,
        val durationMs: Long,
        val index: Int,
        val count: Int,
    )

    var onChanged: (() -> Unit)? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private var focusRequest: AudioFocusRequest? = null
    private var tracks: List<Track> = emptyList()
    private var index = -1
    private var loading = false
    private var loaded = false
    private var error = false
    private var released = false
    private var player: MediaPlayer? = null

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pause()
        }
    }

    init {
        ContextCompat.registerReceiver(
            context,
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    fun loadTracks() {
        if (released || loading || loaded) return
        if (!hasPermission(context)) return
        loading = true
        notifyChanged()
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val music = queryTracks(musicOnly = true)
                    if (music.isEmpty()) queryTracks(musicOnly = false) else music
                }.getOrDefault(emptyList())
            }
            if (released) return@launch
            tracks = result
            loaded = true
            loading = false
            error = false
            if (index !in tracks.indices) index = if (tracks.isEmpty()) -1 else 0
            notifyChanged()
        }
    }

    fun playPause() {
        if (released || tracks.isEmpty()) return
        val mediaPlayer = player
        if (mediaPlayer == null) {
            playIndex(index.coerceAtLeast(0))
            return
        }
        val isPlaying = runCatching { mediaPlayer.isPlaying }.getOrDefault(false)
        if (isPlaying) {
            runCatching { mediaPlayer.pause() }
        } else if (requestFocus()) {
            runCatching { mediaPlayer.start() }
        }
        notifyChanged()
    }

    fun next() = skip(1)

    fun prev() {
        val position = player?.let { runCatching { it.currentPosition }.getOrDefault(0) } ?: 0
        if (position > RESTART_THRESHOLD_MS) {
            runCatching { player?.seekTo(0) }
            notifyChanged()
        } else {
            skip(-1)
        }
    }

    private fun skip(delta: Int) {
        if (tracks.isEmpty()) return
        val current = index.coerceIn(0, tracks.lastIndex)
        index = Math.floorMod(current + delta, tracks.size)
        playIndex(index)
    }

    fun playIndex(position: Int) {
        if (released || position !in tracks.indices) return
        index = position
        error = false
        player?.let { runCatching { it.release() } }
        player = null

        if (!requestFocus()) {
            error = true
            notifyChanged()
            return
        }

        val track = tracks[position]
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        mediaPlayer.setAudioAttributes(audioAttributes)
        mediaPlayer.setOnPreparedListener { prepared ->
            if (released || prepared !== player) return@setOnPreparedListener
            runCatching { prepared.start() }
            notifyChanged()
        }
        mediaPlayer.setOnCompletionListener { if (released.not()) next() }
        mediaPlayer.setOnErrorListener { failed, _, _ ->
            runCatching { failed.release() }
            if (player === failed) player = null
            error = true
            notifyChanged()
            true
        }

        try {
            mediaPlayer.setDataSource(context, track.uri)
            mediaPlayer.prepareAsync()
        } catch (_: Exception) {
            error = true
            if (player === mediaPlayer) player = null
            runCatching { mediaPlayer.release() }
        }
        notifyChanged()
    }

    fun snapshot(): Snapshot {
        val mediaPlayer = player
        val track = tracks.getOrNull(index)
        val position = mediaPlayer?.let { runCatching { it.currentPosition.toLong() }.getOrDefault(0L) } ?: 0L
        val duration = mediaPlayer?.let { runCatching { it.duration.toLong() }.getOrDefault(0L) } ?: 0L
        return Snapshot(
            loading = loading,
            isPlaying = mediaPlayer?.let { runCatching { it.isPlaying }.getOrDefault(false) } == true,
            error = error,
            title = track?.title.orEmpty(),
            artist = track?.artist.orEmpty(),
            positionMs = position,
            durationMs = duration.takeIf { it > 0 } ?: track?.durationMs ?: 0L,
            index = index,
            count = tracks.size,
        )
    }

    fun release() {
        if (released) return
        released = true
        onChanged = null
        scope.cancel()
        runCatching { context.unregisterReceiver(noisyReceiver) }
        player?.let { runCatching { it.release() } }
        player = null
        abandonFocus()
    }

    private fun notifyChanged() {
        if (released) return
        mainHandler.post { onChanged?.invoke() }
    }

    private fun requestFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener({ change ->
                    if (change < 0) mainHandler.post { pause() }
                }, mainHandler)
                .build()
                .also { focusRequest = it }
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { change -> if (change < 0) mainHandler.post { pause() } },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        focusRequest = null
    }

    private fun pause() {
        if (released) return
        player?.let { runCatching { it.pause() } }
        notifyChanged()
    }

    private fun queryTracks(musicOnly: Boolean): List<Track> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        val selection = if (musicOnly) {
            "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        } else {
            "${MediaStore.Audio.Media.DURATION} > $MIN_FALLBACK_DURATION_MS"
        }
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        val result = mutableListOf<Track>()

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)?.takeIf { it.isNotBlank() }
                    ?: cursor.getString(nameColumn)?.substringBeforeLast('.').orEmpty()
                if (title.isBlank()) continue
                result += Track(
                    id = id,
                    title = title,
                    artist = cursor.getString(artistColumn).orEmpty(),
                    durationMs = cursor.getLong(durationColumn),
                    uri = ContentUris.withAppendedId(collection, id),
                )
            }
        }
        return result
    }

    companion object {
        private const val RESTART_THRESHOLD_MS = 3_000
        private const val MIN_FALLBACK_DURATION_MS = 10_000

        fun audioPermission(): String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        fun hasPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, audioPermission()) == PackageManager.PERMISSION_GRANTED
    }
}
