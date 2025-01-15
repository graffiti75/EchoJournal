package br.android.cericatto.echojournal.audio

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun convertPcmToWav(
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