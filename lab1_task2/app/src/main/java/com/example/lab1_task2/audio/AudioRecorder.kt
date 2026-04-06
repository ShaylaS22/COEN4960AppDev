package com.example.lab1_task2.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioRecorder {
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null

    fun interface AudioDataCallback {
        fun onAudioData(audioData: ShortArray)
    }

    @SuppressLint("MissingPermission")
    fun startRecording(callback: AudioDataCallback) {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            return
        }

        // We use a fixed buffer size of 4096 to match PitchDetector's requirements
        // but ensure it's at least minBufferSize
        val bufferSize = maxOf(minBufferSize, 4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                // PitchDetector specifically expects 4096 samples
                val readBuffer = ShortArray(4096)
                while (isActive && isRecording) {
                    val readResult = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: -1
                    if (readResult > 0) {
                        // If we read exactly 4096 samples, send it
                        if (readResult == 4096) {
                            callback.onAudioData(readBuffer.copyOf())
                        } else {
                            // If we read fewer (shouldn't happen often if we request 4096), 
                            // we pad with zeros or skip. For tuning, skipping is safer than padding.
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording()
        }
    }

    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioRecord = null
        }
    }

    fun isRecording(): Boolean = isRecording
}
