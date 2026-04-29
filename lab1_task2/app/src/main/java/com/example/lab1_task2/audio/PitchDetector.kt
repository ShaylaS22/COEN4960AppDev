package com.example.lab1_task2.audio

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor

class PitchDetector : AudioProcessor {

    companion object {
        const val SAMPLE_RATE = 44100f
        const val BUFFER_SIZE = 2048
    }

    private val readings = mutableListOf<Pair<Float, Double>>()
    var onPitchDetected: ((Float, Float) -> Unit)? = null

    private val pitchHandler = PitchDetectionHandler { result, audioEvent ->
        val pitch = result.pitch
        val probability = result.probability
        val timeStamp = audioEvent.timeStamp

        // Lowered threshold to 0.40 to catch notes even with background noise
        if (pitch > 0 && probability > 0.40f) {
            readings.add(Pair(pitch, timeStamp))
            onPitchDetected?.invoke(pitch, probability)
        }
    }

    private val processor = PitchProcessor(
        PitchProcessor.PitchEstimationAlgorithm.YIN,
        SAMPLE_RATE,
        BUFFER_SIZE,
        pitchHandler
    )

    override fun process(audioEvent: AudioEvent): Boolean {
        return processor.process(audioEvent)
    }

    override fun processingFinished() {
        processor.processingFinished()
    }

    fun getReadings(): List<Pair<Float, Double>> = readings.toList()
    fun clear() { readings.clear() }

    fun convertShortToFloat(shortArray: ShortArray): FloatArray {
        val floatArray = FloatArray(shortArray.size)
        for (i in shortArray.indices) {
            floatArray[i] = shortArray[i].toFloat() / 32768.0f
        }
        return floatArray
    }
}
