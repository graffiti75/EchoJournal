package br.android.cericatto.echojournal.audio.playback

import java.io.File

interface AudioPlayer {
    fun playFile(file: File)
    fun stop()
    fun isPlaying(): Boolean
    fun currentPosition(): Int
}