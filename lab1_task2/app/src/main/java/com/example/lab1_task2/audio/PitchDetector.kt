package com.example.lab1_task2.audio

//import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.TarsosDSPAudioFormat
//import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchProcessor
//import be.tarsos.dsp.util.TarsosDSPAudioFormat

class PitchDetector {

    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE = 4096
        const val OVERLAP = 0
    }

    /**
     * Main function to detect pitch from a given float buffer.
     * Uses YIN algorithm and filters results based on probability and frequency range.
     */
    fun detectPitch(audioBuffer: FloatArray, onPitchDetected: (Float, Float) -> Unit) {
        try {
            val handler = PitchDetectionHandler { result: PitchDetectionResult, _: AudioEvent ->
                val pitch = result.pitch
                val probability = result.probability

                // Only calls callback if probability > 0.8 and frequency between 80-1200 Hz
                if (probability > 0.8f && pitch in 80f..1200f) {
                    onPitchDetected(pitch, probability)
                }
            }

            // Uses YIN algorithm
            // Note: Standard TarsosDSP 2.5 Yin implementation uses a default internal threshold (0.15).
            val algorithm = PitchProcessor.PitchEstimationAlgorithm.YIN
            val processor = PitchProcessor(algorithm, SAMPLE_RATE.toFloat(), BUFFER_SIZE, handler)

            // Wrap the buffer in an AudioEvent for processing
            val format = TarsosDSPAudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, false)
            val audioEvent = AudioEvent(format)
            audioEvent.floatBuffer = audioBuffer

            processor.process(audioEvent)
        } catch (e: Exception) {
            // Handles errors gracefully
            e.printStackTrace()
        }
    }

    /**
     * Converts 16-bit PCM (ShortArray) to normalized float samples (-1.0 to 1.0).
     */
    fun convertShortToFloat(shortArray: ShortArray): FloatArray {
        val floatArray = FloatArray(shortArray.size)
        for (i in shortArray.indices) {
            floatArray[i] = shortArray[i].toFloat() / 32768.0f
        }
        return floatArray
    }
}
