package br.android.cericatto.echojournal.ui.entries_list

import android.media.MediaPlayer
import android.media.audiofx.Visualizer

data class EntriesListState(
	val isLoading : Boolean = false,
	val isRecording : Boolean = false,
	val isPlaying : Boolean = false,
	val mediaPlayer : MediaPlayer? = MediaPlayer(),
	val visualizer : Visualizer? = null,
	val amplitudes : List<Int> = emptyList()
)