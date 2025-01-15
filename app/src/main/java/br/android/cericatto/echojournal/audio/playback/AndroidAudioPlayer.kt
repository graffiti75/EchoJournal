package br.android.cericatto.echojournal.audio.playback

import android.content.Context
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

class AndroidAudioPlayer(
    private val context: Context
): AudioPlayer {

    private var player: MediaPlayer? = null
    private var visualizer: Visualizer? = null
    private var amplitudeListener: ((List<Int>) -> Unit)? = null
    private var playingJob: Job? = null

    override fun playFile(file: File) {
        playingJob = CoroutineScope(Dispatchers.IO).launch {
            MediaPlayer.create(context, file.toUri()).apply {
                player = this
                start()
                setupVisualizer()
            }
        }
    }

    override fun stop() {
        player?.stop()
        player?.release()
        player = null
        visualizer?.release()
        visualizer = null
        playingJob?.cancel()
    }

    override fun isPlaying() = player?.isPlaying ?: false

    override fun currentPosition() = player?.currentPosition ?: 0

    private fun setupVisualizer() {
        val audioSessionId = player?.audioSessionId ?: return
        visualizer = Visualizer(audioSessionId).apply {
            captureSize = Visualizer.getCaptureSizeRange()[1]
            setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        waveform?.let {
                            val amplitudes = it.map { abs(it.toInt()) }
                            amplitudeListener?.invoke(amplitudes)
                        }
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {}
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                false
            )
            enabled = true
        }
    }

    fun setAmplitudeListener(listener: (List<Int>) -> Unit) {
        amplitudeListener = listener
    }
}
