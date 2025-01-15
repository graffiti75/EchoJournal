package br.android.cericatto.echojournal.audio.record

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AndroidAudioRecorder(
    private val context: Context
): AudioRecorder {

    private var recordingJob: Job? = null

    /*
    private var recorder: MediaRecorder? = null
    private var visualizer: Visualizer? = null

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else MediaRecorder()
    }

    override fun start(outputFile: File) {
        createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(FileOutputStream(outputFile).fd)

            prepare()
            start()

            recorder = this
        }
    }

    override fun stop() {
        recorder?.stop()
        recorder?.reset()
        recorder = null
        visualizer?.release()
        visualizer = null
    }
     */

    private var recorder: AudioRecord? = null

    override fun start(outputFile: File) {

        if (
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                val bufferSize = AudioRecord.getMinBufferSize(
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                val pcmFile = File(outputFile.parent, "recorded_audio.pcm")
                val buffer = ByteArray(bufferSize)
                pcmFile.outputStream().use { output ->
                    recorder?.let {
                        it.startRecording()
                        val bytesRead = it.read(buffer, 0, buffer.size)
                        if (bytesRead > 0) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
                val wavFile = File(outputFile.parent, "recorded_audio.wav")
                convertPcmToWav(pcmFile, wavFile)
                pcmFile.delete()
//                updateIsRecording(isRecording)
            }
        }
    }

    private fun convertPcmToWav(
        pcmFile: File,
        wavFile: File,
        sampleRate: Int = 44100,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ) {
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

    override fun stop() {
        recorder?.stop()
        recorder?.release()
        recorder = null
        recordingJob?.cancel()
    }
}