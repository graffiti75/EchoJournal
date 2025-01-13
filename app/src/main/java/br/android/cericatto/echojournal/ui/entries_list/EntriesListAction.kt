package br.android.cericatto.echojournal.ui.entries_list

import java.io.File

sealed interface EntriesListAction {
	data class OnUpdatePlaying(val isPlaying: Boolean) : EntriesListAction
	data class OnUpdateRecording(val isRecording: Boolean) : EntriesListAction
	data class OnStartRecording(val audioFile: File, val isRecording: Boolean) : EntriesListAction
	data class OnPlayAudio(val audioFile: File, val isPlaying: Boolean) : EntriesListAction
}