package br.android.cericatto.echojournal.ui.entries_list

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.android.cericatto.echojournal.ui.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class EntriesListViewModel @Inject constructor(
	private val context: Context
): ViewModel() {

	private val _events = Channel<UiEvent>()
	val events = _events.receiveAsFlow()

	private val _state = MutableStateFlow(EntriesListState())
	val state: StateFlow<EntriesListState> = _state.asStateFlow()

	init {
		_state.update { state ->
			state.copy(
				isLoading = false
			)
		}
	}

	fun onAction(action: EntriesListAction) {
		when (action) {
			is EntriesListAction.OnUpdatePlaying -> updateIsPlaying(action.isPlaying)
			is EntriesListAction.OnUpdateRecording -> updateIsRecording(action.isRecording)
			is EntriesListAction.OnStartRecording -> startRecording(action.audioFile, action.isRecording)
			is EntriesListAction.OnPlayAudio -> playAudio(action.audioFile, action.isPlaying)
		}
	}

	/**
	 * State Methods
	 */

	private fun updateIsRecording(isRecording: Boolean) {
		_state.update { state ->
			state.copy(
				isRecording = isRecording
			)
		}
	}

	private fun updateIsPlaying(isPlaying: Boolean) {
		_state.update { state ->
			state.copy(
				isPlaying = isPlaying
			)
		}
	}

	private fun updateAmplitudes(amplitudes: List<Int>) {
		_state.update { state ->
			state.copy(
				amplitudes = amplitudes
			)
		}
	}

	/**
	 * Audio Methods
	 */

	fun isRecordAudioPermissionGranted(): Boolean {
		return ContextCompat.checkSelfPermission(
			context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
	}

	private fun startRecording(audioFile: File, isRecording: Boolean) {
		if (
			ContextCompat.checkSelfPermission(
				context, Manifest.permission.RECORD_AUDIO
			) == PackageManager.PERMISSION_GRANTED
		) {
			Log.i("echo", "EntriesListViewModel.isRecordAudioPermissionGranted() -> Permission granted!")
			viewModelScope.launch {
				withContext(Dispatchers.IO) {
					val bufferSize = AudioRecord.getMinBufferSize(
						44100,
						AudioFormat.CHANNEL_IN_MONO,
						AudioFormat.ENCODING_PCM_16BIT
					)

					val audioRecord = AudioRecord(
						MediaRecorder.AudioSource.MIC,
						44100,
						AudioFormat.CHANNEL_IN_MONO,
						AudioFormat.ENCODING_PCM_16BIT,
						bufferSize
					)

					val pcmFile = File(audioFile.parent, "recorded_audio.pcm")
					val buffer = ByteArray(bufferSize)
					pcmFile.outputStream().use { output ->
						audioRecord.startRecording()
						Log.i("echo", "EntriesListViewModel.isRecordAudioPermissionGranted() -> " +
							"_state.value.isRecording: ${_state.value.isRecording}")
						while (_state.value.isRecording) {
							val bytesRead = audioRecord.read(buffer, 0, buffer.size)
							if (bytesRead > 0) {
								output.write(buffer, 0, bytesRead)
							}
						}
						Log.i("echo", "EntriesListViewModel.isRecordAudioPermissionGranted() -> " +
							"_state.value.isRecording: ${_state.value.isRecording}")
						audioRecord.stop()
						audioRecord.release()
					}
					val wavFile = File(audioFile.parent, "recorded_audio.wav")
					convertPcmToWav(pcmFile, wavFile)
					pcmFile.delete()
				}
				updateIsRecording(isRecording)
			}
		}
	}

	private fun convertPcmToWav(pcmFile: File, wavFile: File, sampleRate: Int = 44100, channels: Int = 1, bitsPerSample: Int = 16) {
		val pcmSize = pcmFile.length().toInt()
		val wavHeader = ByteArray(44)

		// Write WAV header
		ByteBuffer.wrap(wavHeader).order(ByteOrder.LITTLE_ENDIAN).apply {
			// ChunkID
			put("RIFF".toByteArray())
			putInt(36 + pcmSize) // ChunkSize
			put("WAVE".toByteArray()) // Format
			put("fmt ".toByteArray()) // Subchunk1ID
			putInt(16) // Subchunk1Size
			putShort(1) // AudioFormat (PCM)
			putShort(channels.toShort()) // NumChannels
			putInt(sampleRate) // SampleRate
			putInt(sampleRate * channels * bitsPerSample / 8) // ByteRate
			putShort((channels * bitsPerSample / 8).toShort()) // BlockAlign
			putShort(bitsPerSample.toShort()) // BitsPerSample
			put("data".toByteArray()) // Subchunk2ID
			putInt(pcmSize) // Subchunk2Size
		}

		// Write WAV file
		FileOutputStream(wavFile).use { output ->
			output.write(wavHeader) // Write header
			FileInputStream(pcmFile).use { input ->
				val buffer = ByteArray(1024)
				var bytesRead: Int
				while (input.read(buffer).also { bytesRead = it } != -1) {
					output.write(buffer, 0, bytesRead)
				}
			}
		}
	}

	private fun playAudio(audioFile: File, isPlaying: Boolean) {
		Log.i("echo", "EntriesListViewModel.playAudio() -> audioFile: $audioFile")
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				val mediaPlayer = _state.value.mediaPlayer.apply {
					setDataSource(audioFile.absolutePath)
					prepare()
					start()
				}

				val visualizer = Visualizer(_state.value.mediaPlayer.audioSessionId).apply {
					captureSize = Visualizer.getCaptureSizeRange()[1]
					setDataCaptureListener(
						object : Visualizer.OnDataCaptureListener {
							override fun onWaveFormDataCapture(
								visualizer: Visualizer?,
								waveform: ByteArray?,
								samplingRate: Int
							) {
								if (waveform != null) {
									updateAmplitudes(waveform.map { abs(it.toInt()) })
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

				mediaPlayer.setOnCompletionListener {
					mediaPlayer.release()
					visualizer.release()
					updateIsPlaying(isPlaying)
				}
			}
		}
	}
}