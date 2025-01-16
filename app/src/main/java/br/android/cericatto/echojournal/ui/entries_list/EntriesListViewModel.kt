package br.android.cericatto.echojournal.ui.entries_list

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.android.cericatto.echojournal.audio.convertPcmToWav
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
import java.io.IOException
import javax.inject.Inject

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

	private fun getMediaPlayerInstance() {
		if (_state.value.mediaPlayer == null) {
			_state.update { state ->
				state.copy(
					mediaPlayer = MediaPlayer()
				)
			}
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

					val pcmFile = createPcmFile(audioFile, audioRecord, bufferSize)
					val wavFile = File(audioFile.parent, "recorded_audio.wav")
					convertPcmToWav(pcmFile, wavFile)
					pcmFile.delete()
				}
				updateIsRecording(isRecording)
			}
		}
	}

	private fun playAudio(audioFile: File, isPlaying: Boolean) {
		Log.i("echo", "EntriesListViewModel.playAudio() -> audioFile: $audioFile")
		Log.i("echo", "EntriesListViewModel.playAudio() -> absolutePath: ${audioFile.absolutePath}")
		viewModelScope.launch {
			withContext(Dispatchers.IO) {
				getMediaPlayerInstance()
				_state.value.mediaPlayer?.let { player ->
					try {
						player.setDataSource(audioFile.absolutePath)
						if (!player.isPlaying) {
							player.setOnPreparedListener { mp: MediaPlayer -> mp.start() }
							player.prepareAsync() // Non-blocking
						}
					} catch (e: IOException) {
						e.printStackTrace()
					} catch (e: IllegalStateException) {
						e.printStackTrace()
					}

					player.setOnCompletionListener {
						player.stop()
						player.reset()
						updateIsPlaying(isPlaying)
					}
				}
			}
		}
	}

	private fun createPcmFile(
		audioFile: File,
		audioRecord: AudioRecord,
		bufferSize: Int
	): File {
		val pcmFile = File(audioFile.parent, "recorded_audio.pcm")
		val buffer = ByteArray(bufferSize)
		pcmFile.outputStream().use { output ->
			audioRecord.startRecording()
			while (_state.value.isRecording) {
				val bytesRead = audioRecord.read(buffer, 0, buffer.size)
				if (bytesRead > 0) {
					output.write(buffer, 0, bytesRead)
				}
			}
			audioRecord.stop()
			audioRecord.release()
		}
		return pcmFile
	}
}