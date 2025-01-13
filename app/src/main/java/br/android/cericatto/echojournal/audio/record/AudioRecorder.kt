package br.android.cericatto.echojournal.audio.record

import java.io.File

interface AudioRecorder {
    fun start(outputFile: File)
    fun stop()
}