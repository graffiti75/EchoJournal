package br.android.cericatto.echojournal.audio.record

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class BiluRecorder(private val context: Context) : AudioRecorder {

    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val sampleRate = 44100 // Standard sample rate
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    override fun start(outputFile: File) {
        if (
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        ) {
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            // Calculate buffer size
            if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
                throw IllegalArgumentException("Invalid buffer size.")
            }

            try {
                isRecording = true
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                val pcmFile = File(outputFile.parent, "recorded_audio.pcm")
                val buffer = ByteArray(bufferSize)

                recordingJob = scope.launch {
                    audioRecord?.startRecording()
                    pcmFile.outputStream().use { outputStream ->
                        while (isRecording) {
                            val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                            if (readBytes > 0) {
                                outputStream.write(buffer, 0, readBytes)
                            }
                        }
                    }
                }

                scope.launch {
                    convertPcmToWav(pcmFile, outputFile)
                    pcmFile.delete() // Clean up temporary PCM file
                }
            } catch (e: Exception) {
                e.printStackTrace() // Handle any exceptions
            }
        } else {
            throw SecurityException("RECORD_AUDIO permission not granted.")
        }
    }

    override fun stop() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        recordingJob?.cancel()
        recordingJob = null
    }

    private fun convertPcmToWav(pcmFile: File, wavFile: File) {
        val pcmInputStream = pcmFile.inputStream()
        val wavOutputStream = FileOutputStream(wavFile)

        val totalAudioLen = pcmFile.length()
        val totalDataLen = totalAudioLen + 36
        val byteRate = 16 * sampleRate / 8

        writeWavHeader(wavOutputStream, totalAudioLen, totalDataLen, sampleRate, channelConfig, byteRate)

        val buffer = ByteArray(1024)
        while (pcmInputStream.read(buffer) != -1) {
            wavOutputStream.write(buffer)
        }

        pcmInputStream.close()
        wavOutputStream.close()
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        audioLength: Long,
        dataLength: Long,
        sampleRate: Int,
        channels: Int,
        byteRate: Int
    ) {
        val header = ByteArray(44)

        // RIFF chunk descriptor
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (dataLength and 0xff).toByte()
        header[5] = ((dataLength shr 8) and 0xff).toByte()
        header[6] = ((dataLength shr 16) and 0xff).toByte()
        header[7] = ((dataLength shr 24) and 0xff).toByte()
        // WAVE
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // fmt subchunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // PCM
        header[20] = 1 // Audio format
        header[22] = channels.toByte()
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte()
        header[34] = 16 // Bits per sample
        // data subchunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (audioLength and 0xff).toByte()
        header[41] = ((audioLength shr 8) and 0xff).toByte()
        header[42] = ((audioLength shr 16) and 0xff).toByte()
        header[43] = ((audioLength shr 24) and 0xff).toByte()

        out.write(header, 0, 44)
    }
}
