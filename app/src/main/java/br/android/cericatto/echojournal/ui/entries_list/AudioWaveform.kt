package br.android.cericatto.echojournal.ui.entries_list

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.android.cericatto.echojournal.ui.theme.audioBarBackgroundColor
import br.android.cericatto.echojournal.ui.theme.audioBarChartWave
import br.android.cericatto.echojournal.ui.theme.audioBarProgressColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Data class to hold both amplitude data and duration.
 */
data class WaveformData(
	val amplitudes: List<Float>,
	val durationInSeconds: Float
)

/**
 * Class to handle WAV file processing.
 */
class WaveformProcessor {
	// Function to extract amplitude data from WAV file
	suspend fun processWaveFile(wavFile: File): WaveformData = withContext(Dispatchers.IO) {
		val fis = FileInputStream(wavFile)
		val header = ByteArray(44)
		fis.read(header)

		// Calculate duration from header
		// Bytes 24-27: Sample Rate (e.g., 44100)
		// Bytes 40-43: Data Size
		val sampleRate = ByteBuffer.wrap(header.slice(24..27).toByteArray())
			.order(ByteOrder.LITTLE_ENDIAN).int
		val dataSize = ByteBuffer.wrap(header.slice(40..43).toByteArray())
			.order(ByteOrder.LITTLE_ENDIAN).int

		// Duration = (data size in bytes) / (bytes per sample * channels * sample rate)
		val duration = dataSize.toFloat() / (2 * 1 * sampleRate)

		val samples = mutableListOf<Float>()
		val buffer = ByteArray(2048)
		var bytesRead: Int

		// Read PCM data
		while (fis.read(buffer).also { bytesRead = it } > 0) {
			for (i in 0 until bytesRead step 2) {
				if (i + 1 < bytesRead) {
					val sample = ByteBuffer
						.wrap(buffer.slice(i..i + 1).toByteArray())
						.order(ByteOrder.LITTLE_ENDIAN)
						.short

					// Normalize to range 0..1
					samples.add(sample / 32768f)
				}
			}
		}

		// Instead of fixed compression, calculate based on desired bars per second
		val barsPerSecond = 30 // You can adjust this value to control density
		val desiredBars = (duration * barsPerSecond).roundToInt()
		val compressionFactor = samples.size / desiredBars
		val compressedAmplitudes = samples.chunked(compressionFactor) { chunk ->
			chunk.maxByOrNull { kotlin.math.abs(it) } ?: 0f
		}
		WaveformData(compressedAmplitudes, duration)
	}
}

@Composable
private fun AudioWaveform(
	waveformData: WaveformData,
	currentProgress: Float,
	modifier: Modifier = Modifier,
	backgroundColor: Color = Color.Transparent,
	barColor: Color = audioBarChartWave,
	playedBarColor: Color = audioBarProgressColor.copy(alpha = 0.3f),
	indicatorColor: Color = audioBarProgressColor.copy(alpha = 0.7f)
) {
	Canvas(
		modifier = modifier
			.fillMaxWidth()
			.height(48.dp)
			.background(
				color = backgroundColor,
				shape = RoundedCornerShape(20.dp)
			)
	) {
		val canvasWidth = size.width
		val canvasHeight = size.height

		// Calculate bar width based on canvas width and duration
		val totalBars = waveformData.amplitudes.size
		val scaledBarWidth = (canvasWidth / totalBars) * 0.8f // 80% of available space for bars
		val scaledSpacing = (canvasWidth / totalBars) * 0.2f  // 20% for spacing

		// Draw all bars
		waveformData.amplitudes.forEachIndexed { index, amplitude ->
			val x = index * (scaledBarWidth + scaledSpacing)
			val barHeight = canvasHeight * kotlin.math.abs(amplitude)
			val startY = (canvasHeight - barHeight) / 2

			// Determine if this bar has been played
			val progressPosition = (currentProgress * canvasWidth)
			val isPlayed = x <= progressPosition

			// Draw amplitude bar with appropriate color
			drawLine(
				color = if (isPlayed) playedBarColor else barColor,
				start = Offset(x, startY),
				end = Offset(x, startY + barHeight),
				strokeWidth = scaledBarWidth
			)

			// Draw progress indicator (ball)
			drawProgressIndicator(
				progress = currentProgress,
				canvasWidth = canvasWidth,
				canvasHeight = canvasHeight,
				color = indicatorColor
			)
		}
	}
}

private fun DrawScope.drawProgressIndicator(
	progress: Float,
	canvasWidth: Float,
	canvasHeight: Float,
	color: Color
) {
	val x = progress * canvasWidth
	val radius = 8.dp.toPx()

	drawCircle(
		color = color,
		radius = radius,
		center = Offset(x, canvasHeight / 2)
	)
}


// AudioPlayerState holds all the state needed for the player
private data class AudioPlayerState(
	val isPlaying: Boolean = false,
	val progress: Float = 0f,
	val waveformData: WaveformData? = null
)

// AudioPlayerController manages MediaPlayer lifecycle and state updates
private class AudioPlayerController(
	private val file: File,
	private val coroutineScope: CoroutineScope
) {
	// State management using StateFlow
	private val _state = MutableStateFlow(AudioPlayerState())
	val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

	// MediaPlayer instance
	private var mediaPlayer: MediaPlayer? = null
	private var progressJob: Job? = null

	init {
		// Initialize MediaPlayer and load waveform data
		initializePlayer()
		loadWaveformData()
	}

	private fun initializePlayer() {
		mediaPlayer = MediaPlayer().apply {
			setDataSource(file.path)
			prepare()

			setOnCompletionListener {
				coroutineScope.launch {
					_state.update { it.copy(
						isPlaying = false,
						progress = 0f
					)}
				}
			}
		}
	}

	private fun loadWaveformData() {
		coroutineScope.launch {
			val waveformProcessor = WaveformProcessor()
			val data = waveformProcessor.processWaveFile(file)
			_state.update { it.copy(waveformData = data) }
		}
	}

	fun togglePlayPause() {
		mediaPlayer?.let { player ->
			val currentState = _state.value

			if (currentState.isPlaying) {
				pausePlayback()
			} else {
				if (currentState.progress >= 1f) {
					player.seekTo(0)
					_state.update { it.copy(progress = 0f) }
				}
				startPlayback()
			}
		}
	}

	private fun startPlayback() {
		mediaPlayer?.start()
		_state.update { it.copy(isPlaying = true) }
		startProgressTracking()
	}

	private fun pausePlayback() {
		mediaPlayer?.pause()
		_state.update { it.copy(isPlaying = false) }
		progressJob?.cancel()
	}

	private fun startProgressTracking() {
		progressJob?.cancel()
		progressJob = coroutineScope.launch {
			while (isActive) {
				mediaPlayer?.let { player ->
					val progress = player.currentPosition.toFloat() / player.duration
					_state.update { it.copy(progress = progress) }
				}
				delay(16) // Approximately 60 FPS
			}
		}
	}

	fun release() {
		progressJob?.cancel()
		mediaPlayer?.release()
		mediaPlayer = null
	}
}

@Composable
fun AudioPlayerWithControls(file: File) {
	// Create the controller with proper lifecycle scope
	val coroutineScope = rememberCoroutineScope()

	// Remember the controller instance
	val controller = remember(file) {
		AudioPlayerController(file, coroutineScope)
	}

	// Collect the state as a Compose State
	val playerState by controller.state.collectAsState()

	// Cleanup when the composable is disposed
	DisposableEffect(controller) {
		onDispose {
			controller.release()
		}
	}

	Row(
		modifier = Modifier
			.background(
				color = audioBarBackgroundColor.copy(alpha = 0.9f),
				shape = RoundedCornerShape(20.dp)
			)
			.fillMaxWidth()
			.padding(8.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.Start
	) {
		IconButton(
			onClick = { controller.togglePlayPause() },
			modifier = Modifier.padding(end = 8.dp)
				.background(
					color = Color.White,
					shape = RoundedCornerShape(30.dp)
				)
		) {
			Icon(
				imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
				contentDescription = if (playerState.isPlaying) "Pause" else "Play",
				tint = audioBarProgressColor.copy(alpha = 0.85f)
			)
		}

		Box(modifier = Modifier.weight(1f)) {
			playerState.waveformData?.let { data ->
				AudioWaveform(
					waveformData = data,
					currentProgress = playerState.progress,
					modifier = Modifier.fillMaxWidth()
				)
			}
		}
	}
}

@Preview
@Composable
private fun AudioWaveformPreview() {
	AudioWaveform(
		waveformData = WaveformData(
			amplitudes = listOf(
				0.0f, 0.0026550293f, 0.0029296875f, 0.0035095215f, -0.0029296875f,
				-0.0030212402f, 0.0026550293f, 0.0027770996f, -0.0026550293f, -0.0031738281f,
				-0.002380371f, 0.009490967f, -0.009613037f, 0.0061035156f, -0.033294678f,
				-0.09576416f, -0.23312378f, 0.4482727f, 0.48446655f, -0.13415527f,
				-0.26391602f, -0.41088867f, -0.3270874f, -0.16595459f, 0.2050476f,
				0.16671753f, 0.22183228f, 0.45864868f, 0.4064331f, 0.23181152f,
				0.20007324f, 0.52215576f, 0.590271f, 0.58200073f, 0.49273682f,
				0.28399658f, 0.1461792f, -0.112701416f, -0.13308716f, -0.0987854f,
				-0.18139648f, -0.13647461f, -0.09335327f, 0.0463562f, -0.035217285f,
				-0.0087890625f, -0.005645752f, -0.0033569336f, 0.015289307f, -0.0042419434f,
				0.0021972656f
			),
			durationInSeconds = 40f
		),
		modifier = Modifier,
		currentProgress = 0f
	)
}

@Preview
@Composable
private fun AudioPlayerWithControlsPreview() {
	val context = LocalContext.current
	AudioPlayerWithControls(
		file = File(context.cacheDir, "recorded_audio.wav")
	)
}