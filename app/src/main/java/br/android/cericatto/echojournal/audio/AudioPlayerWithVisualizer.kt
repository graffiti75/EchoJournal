package br.android.cericatto.echojournal.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.android.cericatto.echojournal.ui.theme.audioBarChartWave
import kotlin.math.absoluteValue

@Composable
fun AudioVisualizer(amplitudeData: List<Int>) {
	val barWidth = 10.dp
	val maxAmplitude = amplitudeData.maxOrNull() ?: 1

	Canvas(
		modifier = Modifier.fillMaxWidth()
			.height(60.dp)
	) {
		amplitudeData.forEachIndexed { index, amplitude ->
			val barHeight = (amplitude.toFloat() / maxAmplitude) * size.height
			drawRect(
				color = audioBarChartWave,
//				topLeft = Offset(x = index * barWidth.toPx(), y = size.height - barHeight),
//				size = Size(width = barWidth.toPx(), height = barHeight)
				topLeft = center.copy(y = size.height / 2 - amplitude / 2f),
				size = size.copy(height = amplitude.toFloat())
			)
		}
	}
}

@Composable
fun AudioPlayerWithVisualizer(
	context: Context,
	audioResId: Int
) {
	val amplitudeNumber = 50
	val amplitudeData = remember { mutableStateListOf<Int>() }
	val mediaPlayer = remember { MediaPlayer.create(context, audioResId) }
	var visualizer: Visualizer? = null

	// Start MediaPlayer and update amplitudes
	DisposableEffect(Unit) {
		mediaPlayer.start()

		// Initialize Visualizer
		visualizer = Visualizer(mediaPlayer.audioSessionId).apply {
			captureSize = Visualizer.getCaptureSizeRange()[1] // Max capture size
			enabled = true
		}

		onDispose {
			visualizer?.release()
			mediaPlayer.stop()
			mediaPlayer.release()
		}
	}

	LaunchedEffect(Unit) {
		while (mediaPlayer.isPlaying) {
			/*
			// Simulate amplitude data (replace with actual amplitude logic if available)
			val currentAmplitude = (1..100).random() // Dummy amplitude for demonstration
			amplitudeData.add(currentAmplitude)

			if (amplitudeData.size > amplitudeNumber) { // Limit data to last 100 amplitudes
				amplitudeData.removeAt(0)
			}
			 */

			/*
			visualizer?.let { visualizer ->
				visualizer.setDataCaptureListener(
					object : Visualizer.OnDataCaptureListener {
						override fun onWaveFormDataCapture(
							visualizer: Visualizer?,
							waveform: ByteArray?,
							samplingRate: Int
						) {
							if (waveform != null) {
								amplitudeData = waveform.map { abs(it.toInt()) }

								if (amplitudeData.size > amplitudeNumber) {
									amplitudeData.removeAt(0)
								}
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
			}
			 */

			//
			visualizer?.let { vis ->
				val waveform = ByteArray(vis.captureSize)
				vis.getWaveForm(waveform)

				// Calculate the amplitude as the average absolute value of the waveform
				val amplitude = waveform.map { it.toInt().absoluteValue }.average().toInt()
				amplitudeData.add(amplitude)

				if (amplitudeData.size > amplitudeNumber) {
					amplitudeData.removeAt(0)
				}
			}
			//

			// Update every 50ms
			kotlinx.coroutines.delay(100)
		}
	}

	Column(
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier.fillMaxWidth()
	) {
		AudioVisualizer(amplitudeData = amplitudeData)
	}
}