package com.example.lab1_task2.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import androidx.core.content.ContextCompat
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.filters.HighPass
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.onsets.PercussionOnsetDetector
import be.tarsos.dsp.writer.WriterProcessor
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorder(private val context: Context) {
    companion object {
        const val SAMPLE_RATE = 44100f
        const val BUFFER_SIZE = 8192
        private const val TAG = "AudioRecorder"
    }

    private var dispatcher: AudioDispatcher? = null
    private var wavFile: File? = null
    private val onsetTimes = mutableListOf<Double>()
    val pitchDetector = PitchDetector()
    private var noiseSuppressor: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null
    private var isDistorted = false

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing RECORD_AUDIO permission")
            return
        }

        // 1. Ensure any previous recording is fully stopped
        stopRecording()

        onsetTimes.clear()
        pitchDetector.clear()
        isDistorted = false

        try {
            val format = TarsosDSPAudioFormat(SAMPLE_RATE, 16, 1, true, false)
            wavFile = File.createTempFile("transcription_", ".wav", context.cacheDir)

            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE.toInt(),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            if (minBufferSize <= 0) {
                Log.e(TAG, "Invalid min buffer size: $minBufferSize")
                return
            }

            val internalBufferSize = maxOf(minBufferSize, BUFFER_SIZE * 4)

            // 2. Use VOICE_RECOGNITION for cleaner audio and better compatibility
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE.toInt(),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                internalBufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord could not be initialized. Source MIC might be busy.")
                audioRecord.release()
                return
            }

            // 3. Setup hardware effects if available
            val sessionId = audioRecord.audioSessionId
            setupAudioEffects(sessionId)

            // 4. Initialize the Dispatcher with safety-wrapped stream
            val audioStream = AndroidAudioInputStream(audioRecord)
            dispatcher = AudioDispatcher(audioStream, BUFFER_SIZE, 0)

            // Add Processors
            dispatcher?.addAudioProcessor(HighPass(100f, SAMPLE_RATE))
            setupMonitors(dispatcher!!)
            dispatcher?.addAudioProcessor(pitchDetector)
            
            val onsetHandler = be.tarsos.dsp.onsets.OnsetHandler { time, _ -> 
                onsetTimes.add(time) 
            }
            dispatcher?.addAudioProcessor(PercussionOnsetDetector(SAMPLE_RATE, BUFFER_SIZE, onsetHandler, 60.0, 5.0))

            // WAV Writer
            val randomAccessFile = RandomAccessFile(wavFile, "rw")
            dispatcher?.addAudioProcessor(WriterProcessor(format, randomAccessFile))

            Thread(dispatcher, "Audio Dispatcher").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
        }
    }

    private fun setupAudioEffects(sessionId: Int) {
        try {
            if (AutomaticGainControl.isAvailable()) {
                agc = AutomaticGainControl.create(sessionId)?.apply { enabled = false }
            }
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply { enabled = false }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not configure hardware audio effects", e)
        }
    }

    private fun setupMonitors(dispatcher: AudioDispatcher) {
        // Distortion Monitor
        dispatcher.addAudioProcessor(object : AudioProcessor {
            private var distortionBufferCount = 0
            override fun process(audioEvent: AudioEvent): Boolean {
                val maxAbs = audioEvent.floatBuffer.maxOfOrNull { Math.abs(it) } ?: 0f
                if (maxAbs >= 0.98f) distortionBufferCount++ else distortionBufferCount = 0
                if (distortionBufferCount >= 5 && !isDistorted) {
                    isDistorted = true
                    Log.w(TAG, "Digital distortion detected!")
                }
                return true
            }
            override fun processingFinished() {}
        })

        // Gain boost for transcription
        dispatcher.addAudioProcessor(object : AudioProcessor {
            override fun process(audioEvent: AudioEvent): Boolean {
                val buffer = audioEvent.floatBuffer
                for (i in buffer.indices) buffer[i] = (buffer[i] * 5f).coerceIn(-1.0f, 1.0f)
                return true
            }
            override fun processingFinished() {}
        })
    }

    fun stopRecording() {
        dispatcher?.stop()
        dispatcher = null
        
        noiseSuppressor?.release()
        noiseSuppressor = null
        agc?.release()
        agc = null
    }

    fun getOnsetTimes(): List<Double> = onsetTimes.toList()
    fun getWavFilePath(): String? = wavFile?.absolutePath
    fun isRecording(): Boolean = dispatcher != null
    fun isDistortionDetected(): Boolean = isDistorted

    /**
     * Inner class to safely wrap AudioRecord for TarsosDSP
     */
    private class AndroidAudioInputStream(private val audioRecord: AudioRecord) : be.tarsos.dsp.io.TarsosDSPAudioInputStream {
        private val format = TarsosDSPAudioFormat(SAMPLE_RATE, 16, 1, true, false)
        private val isClosing = AtomicBoolean(false)

        init {
            try {
                if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.startRecording()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AudioRecord", e)
            }
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (isClosing.get() || audioRecord.state != AudioRecord.STATE_INITIALIZED) return -1
            
            var totalRead = 0
            while (totalRead < len && !isClosing.get()) {
                try {
                    val readResult = audioRecord.read(b, off + totalRead, len - totalRead)
                    if (readResult < 0) return -1
                    if (readResult == 0) {
                        if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) return -1
                        Thread.yield()
                    }
                    totalRead += readResult
                } catch (e: Exception) {
                    return -1
                }
            }
            return if (totalRead == 0 && isClosing.get()) -1 else totalRead
        }

        override fun skip(n: Long): Long = 0
        
        override fun close() {
            if (isClosing.getAndSet(true)) return
            try {
                if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                    if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord.stop()
                    }
                    audioRecord.release()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing AudioRecord", e)
            }
        }

        override fun getFormat(): TarsosDSPAudioFormat = format
        override fun getFrameLength(): Long = -1
    }
}
