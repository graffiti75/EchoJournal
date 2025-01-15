package br.android.cericatto.echojournal.audio.playback

import android.content.Context
import android.media.MediaPlayer
import java.io.File

class BiluPlayer(
    private val context: Context
): AudioPlayer {

    private var player: MediaPlayer? = null

    override fun playFile(file: File) {
        stop() // Stop any existing playback
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
        }
        /*
        MediaPlayer.create(context, file.toUri()).apply {
            player = this
            start()
        }
         */
    }

    override fun stop() {
        player?.stop()
        player?.release()
        player = null
    }

    override fun isPlaying() = player?.isPlaying ?: false

    override fun currentPosition() = player?.currentPosition ?: 0
}