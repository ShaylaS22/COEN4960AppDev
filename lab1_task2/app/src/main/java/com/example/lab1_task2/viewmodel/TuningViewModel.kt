package com.example.lab1_task2.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab1_task2.audio.AudioRecorder
import com.example.lab1_task2.audio.PitchDetector
import com.example.lab1_task2.model.TuningResult
import com.example.lab1_task2.model.TuningState
import com.example.lab1_task2.utils.NoteFrequencyCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.sqrt

class TuningViewModel : ViewModel() {

    private val _tuningState = MutableLiveData<TuningState>(TuningState.Idle)
    val tuningState: LiveData<TuningState> = _tuningState

    private var audioRecorder: AudioRecorder? = null
    private val pitchDetector = PitchDetector()
    private val frequencyBuffer = mutableListOf<Float>()
    
    private var processingJob: Job? = null

    /**
     * Initializes the AudioRecorder and starts listening for pitch data.
     */
    fun startTuning() {
        if (audioRecorder != null && audioRecorder!!.isRecording()) return

        audioRecorder = AudioRecorder()
        audioRecorder?.startRecording { audioData ->
            // --- AUDIO CONFIRMATION LOGGING ---
            val rms = calculateRMS(audioData)
            if (rms > 0) {
                Log.d("AudioInputCheck", "Receiving audio... RMS: $rms")
            }
            // ----------------------------------

            // Throttle processing using a coroutine and delay
            if (processingJob?.isActive == true) return@startRecording

            processingJob = viewModelScope.launch(Dispatchers.Default) {
                try {
                    val floatBuffer = pitchDetector.convertShortToFloat(audioData)
                    pitchDetector.detectPitch(floatBuffer) { frequency, confidence ->
                        processPitch(frequency, confidence)
                    }
                    // Add delay between processing cycles as requested
                    delay(150)
                } catch (e: Exception) {
                    if(e !is CancellationException){
                        Log.e("TuningViewModel", "Error: ${e.message}")
                        _tuningState.postValue(TuningState.Error(e.message ?: "Unknown error during tuning"))
                    }
                }
            }
        }
        
        // Initial state update to Listening
        _tuningState.postValue(TuningState.Listening(null))
    }

    private fun calculateRMS(audioData: ShortArray): Double {
        var sum = 0.0
        for (sample in audioData) {
            sum += sample.toDouble() * sample.toDouble()
        }
        return sqrt(sum / audioData.size)
    }

    private fun processPitch(frequency: Float, probability: Float) {
        Log.d("TuningViewModel", "Pitch detected: $frequency Hz, confidence: $probability")

        if (probability > 0.7f && frequency > 0) {
            Log.d("TuningViewModel", "Valid pitch, processing...")
            val smoothedFreq = smoothFrequency(frequency)
            val (note, cents) = NoteFrequencyCalculator.getClosestNote(smoothedFreq)
            val targetFreq = NoteFrequencyCalculator.getNoteFrequency(note) ?: 0f

            val result = TuningResult(
                detectedNote = note,
                frequency = smoothedFreq,
                targetFrequency = targetFreq,
                centsOff = cents,
                isInTune = abs(cents) <= 5f,
                confidence = probability
            )

            _tuningState.postValue(TuningState.Listening(result))
        } else {
            Log.d("TuningViewModel", "Pitch rejected - too low confidence or invalid")
        }
    }

    /**
     * Stops the audio recorder and clears buffers.
     */
    fun stopTuning() {
        audioRecorder?.stopRecording()
        audioRecorder = null
        processingJob?.cancel()
        frequencyBuffer.clear()
        _tuningState.value = TuningState.Idle
    }

    /**
     * Smooths frequency readings using a median filter and removes outliers.
     */
    private fun smoothFrequency(newFrequency: Float): Float {
        frequencyBuffer.add(newFrequency)
        if (frequencyBuffer.size > 5) {
            frequencyBuffer.removeAt(0)
        }

        val sorted = frequencyBuffer.sorted()
        val median = if (sorted.isEmpty()) {
            newFrequency
        } else if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2f
        } else {
            sorted[sorted.size / 2]
        }

        // Remove outliers (>50 Hz difference from median) for future readings
        frequencyBuffer.removeAll { abs(it - median) > 50f }

        return median
    }

    override fun onCleared() {
        super.onCleared()
        stopTuning()
    }
}
