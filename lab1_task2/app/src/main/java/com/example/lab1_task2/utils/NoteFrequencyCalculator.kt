package com.example.lab1_task2.utils

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow

object NoteFrequencyCalculator {
    const val A4_FREQUENCY = 440.0f

    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    private val NOTE_FREQUENCIES: Map<String, Float> = mutableMapOf<String, Float>().apply {
        // Octaves 0 through 8
        for (octave in 0..8) {
            for (i in NOTE_NAMES.indices) {
                val noteName = "${NOTE_NAMES[i]}$octave"
                
                // Calculate n such that A4 is 49
                // A4 is octave 4, index 9. 
                // n = 12 * 4 + 9 + offset = 49 => 57 + offset = 49 => offset = -8
                val n = (octave * 12) + i - 8
                
                val frequency = A4_FREQUENCY * 2.0f.pow((n - 49) / 12.0f)
                put(noteName, frequency)
                
                // Stop at C8 if we want exactly "to C8"
                if (octave == 8 && i == 0) break
            }
        }
    }

    fun getClosestNote(frequency: Float): Pair<String, Float> {
        if (frequency < 16f || frequency > 8000f) {
            return Pair("--", 0f)
        }

        var closestNote = "--"
        var minDiff = Float.MAX_VALUE
        var targetFreq = 1f

        for ((note, freq) in NOTE_FREQUENCIES) {
            val diff = abs(frequency - freq)
            if (diff < minDiff) {
                minDiff = diff
                closestNote = note
                targetFreq = freq
            }
        }

        val centsOff = 1200 * log2(frequency / targetFreq)
        return Pair(closestNote, centsOff)
    }

    fun getNoteFrequency(noteName: String): Float? {
        return NOTE_FREQUENCIES[noteName]
    }
}
