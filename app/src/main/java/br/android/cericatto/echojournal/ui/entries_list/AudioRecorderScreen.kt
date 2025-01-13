package br.android.cericatto.echojournal.ui.entries_list

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.android.cericatto.echojournal.R
import br.android.cericatto.echojournal.ui.theme.audioBarChartBackground
import br.android.cericatto.echojournal.ui.theme.audioBarChartWave
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun RequestRecordAudioPermission(viewModel: EntriesListViewModel) {
	var permissionGranted by remember { mutableStateOf(false) }
	var showDialog by remember { mutableStateOf(!viewModel.isRecordAudioPermissionGranted()) }
	val context = LocalContext.current

	// Launcher to request the RECORD_AUDIO permission.
	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission()
	) { isGranted ->
		// Handle the permission result.
		permissionGranted = isGranted
		if (!isGranted) {
			showDialog = true
		}
	}

	if (showDialog) {
		AlertDialog(
			onDismissRequest = {
				showDialog = false
			},
			title = {
				Text(
					text = context.getString(R.string.record_audio_permission_dialog_title)
				)
			},
			text = {
				Text(
					text = context.getString(R.string.permission_needed)
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						showDialog = false
						launcher.launch(Manifest.permission.RECORD_AUDIO)
					}
				) {
					Text(
						text = context.getString(R.string.dialog__allow)
					)
				}
			},
			dismissButton = {
				TextButton(
					onClick = {
						showDialog = false
					}
				) {
					Text(
						text = context.getString(R.string.dialog__cancel)
					)
				}
			}
		)
	}
}

@Composable
fun AudioPlayer(
	state: EntriesListState,
	onAction: (EntriesListAction) -> Unit,
	file: File,
	modifier: Modifier
) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
		modifier = modifier
	) {
		AudioRecorderScreen(
			onAction = onAction,
			state = state,
			file = file
		)
		AudioVisualizer(
			state = state,
			file = file
		)
	}
}

@Composable
fun AudioRecorderScreen(
	state: EntriesListState,
	onAction: (EntriesListAction) -> Unit,
	file: File
) {
	Log.i("echo", "AudioRecorderScreen -> state.isRecording: ${state.isRecording}")
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(16.dp),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Button(
			modifier = Modifier.padding(bottom = 16.dp),
			onClick = {
				if (state.isRecording) {
					onAction(EntriesListAction.OnUpdateRecording(isRecording = false))
				} else {
					onAction(EntriesListAction.OnUpdateRecording(isRecording = true))
					onAction(
						EntriesListAction.OnStartRecording(
							audioFile = file, isRecording = false
						)
					)
				}
			}
		) {
			Text(
				text = if (state.isRecording) "Stop Recording" else "Start Recording"
			)
		}
		Button(
			onClick = {
				if (!state.isPlaying) {
					onAction(EntriesListAction.OnUpdatePlaying(isPlaying = true))
					onAction(EntriesListAction.OnPlayAudio(audioFile = file, isPlaying = false))
				}
			},
			enabled = !state.isRecording && file.exists()
		) {
			Text(
				text = if (state.isPlaying) "Playing..." else "Play Audio"
			)
		}
	}
}

@Composable
fun AudioVisualizer(
	state: EntriesListState,
	file: File
) {
//	var amplitudes by remember { mutableStateOf(listOf<Int>()) }
	val scope = rememberCoroutineScope()

	/*
	DisposableEffect(file) {
		val visualizer = Visualizer(state.mediaPlayer.audioSessionId).apply {
			captureSize = Visualizer.getCaptureSizeRange()[1]
			setDataCaptureListener(
				object : Visualizer.OnDataCaptureListener {
					override fun onWaveFormDataCapture(
						visualizer: Visualizer?,
						waveform: ByteArray?,
						samplingRate: Int
					) {
						if (waveform != null) {
							amplitudes = waveform.map { abs(it.toInt()) }
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
		visualizer.enabled = true
		onDispose {
			state.mediaPlayer.release()
			visualizer.release()
		}
	}
	 */

	LaunchedEffect(state.isPlaying) {
		scope.launch {
			while (
				state.isPlaying &&
				state.mediaPlayer.isPlaying &&
				state.mediaPlayer.currentPosition < 0.99
			) {
				delay(1000L) // Update every second
			}
		}
	}
	BarChart(amplitudes = state.amplitudes)
}

@Composable
fun BarChart(
	amplitudes: List<Int>
) {
	val maxBars = 50 // Limit number of bars on the screen
	val displayAmplitudes = if (amplitudes.size > maxBars) {
		amplitudes.takeLast(maxBars)
	} else {
		amplitudes
	}

	Row(
		modifier = Modifier
			.background(audioBarChartBackground)
			.fillMaxWidth()
			.height(60.dp),
		horizontalArrangement = Arrangement.SpaceEvenly
	) {
		displayAmplitudes.forEach { amplitude ->
			Canvas(
				modifier = Modifier
					.width(5.dp)
					.fillMaxHeight()
			) {
				drawRoundRect(
					color = audioBarChartWave,
					topLeft = center.copy(y = size.height / 2 - amplitude / 2f),
					size = size.copy(height = amplitude.toFloat())
				)
			}
		}
	}
}

@Preview
@Composable
private fun AudioPlayerPreview() {
	val context = LocalContext.current
	AudioPlayer(
		onAction = {},
		state = EntriesListState(),
		file = File(context.cacheDir, "recorded_audio.wav"),
		modifier = Modifier
	)
}

@Preview
@Composable
private fun AudioRecorderScreenPreview() {
	val context = LocalContext.current
	AudioRecorderScreen(
		onAction = {},
		state = EntriesListState(),
		file = File(context.cacheDir, "recorded_audio.wav")
	)
}

@Preview
@Composable
private fun AudioVisualizerPreview() {
	val context = LocalContext.current
	AudioVisualizer(
		state = EntriesListState(),
		file = File(context.cacheDir, "recorded_audio.wav")
	)
}