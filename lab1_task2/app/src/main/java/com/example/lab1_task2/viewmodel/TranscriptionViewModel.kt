package com.example.lab1_task2.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab1_task2.model.TranscriptionResult
import com.example.lab1_task2.model.TranscriptionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

class TranscriptionViewModel : ViewModel() {

    val state = MutableLiveData<TranscriptionState>(TranscriptionState.IDLE)
    val statusMessage = MutableLiveData<String>()
    val transcriptionResult = MutableLiveData<TranscriptionResult>()

    fun onRecordingStarted() {
        state.postValue(TranscriptionState.RECORDING)
    }

    fun onRecordingStopped() {
        state.postValue(TranscriptionState.RECORDED)
        statusMessage.postValue("Recording complete. Play back or transcribe.")
    }

    fun transcribe(
        onsetTimes: List<Double>,
        pitchReadings: List<Pair<Float, Double>>,
        wavFilePath: String
    ) {
        state.postValue(TranscriptionState.PROCESSING("Transcribing..."))

        viewModelScope.launch(Dispatchers.Default) {
            Log.d("TranscriptionVM", "Received ${pitchReadings.size} pitch readings")
            val notes = detectNotesFromPitches(pitchReadings)
            Log.d("TranscriptionVM", "Detected ${notes.size} notes")

            if (notes.isNotEmpty()) {
                val musicXml = buildMusicXMLManual(notes)
                if (musicXml.isNotEmpty()) {
                    val result = TranscriptionResult(
                        musicXml = musicXml,
                        wavFilePath = wavFilePath,
                        timestampMs = System.currentTimeMillis()
                    )
                    transcriptionResult.postValue(result)
                    state.postValue(TranscriptionState.IDLE)
                    statusMessage.postValue("Done! Sheet music ready.")
                } else {
                    state.postValue(TranscriptionState.RECORDED)
                    statusMessage.postValue("Error generating MusicXML.")
                }
            } else {
                state.postValue(TranscriptionState.RECORDED)
                statusMessage.postValue("No notes detected. Try playing closer to the mic.")
            }
        }
    }

    private fun detectNotesFromPitches(readings: List<Pair<Float, Double>>): List<Triple<Int, Int, String>> {
        if (readings.isEmpty()) return emptyList()

        val notes = mutableListOf<Triple<Int, Int, String>>()
        var currentMidi = -1
        var startTime = -1.0
        var lastTime = -1.0

        for (reading in readings) {
            val midi = (69 + 12 * (ln(reading.first / 440.0) / ln(2.0))).roundToInt()
            
            // Filter out clearly invalid MIDI notes
            if (midi < 21 || midi > 108) continue

            if (midi != currentMidi) {
                if (currentMidi != -1) {
                    val durationMs = ((lastTime - startTime) * 1000).toInt()
                    // Lowered duration threshold to 50ms to catch faster notes
                    if (durationMs > 50) { 
                        val (xmlDuration, xmlType) = quantizeDuration(durationMs)
                        notes.add(Triple(currentMidi, xmlDuration, xmlType))
                    }
                }
                currentMidi = midi
                startTime = reading.second
            }
            lastTime = reading.second
        }

        if (currentMidi != -1) {
            val durationMs = ((lastTime - startTime) * 1000).toInt()
            if (durationMs > 50) {
                val (xmlDuration, xmlType) = quantizeDuration(durationMs)
                notes.add(Triple(currentMidi, xmlDuration, xmlType))
            }
        }

        return notes
    }

    private fun quantizeDuration(durationMs: Int): Pair<Int, String> {
        val targets = listOf(
            2000 to (16 to "whole"),
            1000 to (8 to "half"),
            500 to (4 to "quarter"),
            250 to (2 to "eighth"),
            125 to (1 to "sixteenth")
        )
        val bestMatch = targets.minBy { abs(it.first - durationMs) }
        return bestMatch.second
    }

    private fun buildMusicXMLManual(notes: List<Triple<Int, Int, String>>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<!DOCTYPE score-partwise PUBLIC \"-//Recordare//DTD MusicXML 4.0 Partwise//EN\" \"http://www.musicxml.org/dtds/partwise.dtd\">\n")
        sb.append("<score-partwise version=\"4.0\">\n")
        sb.append("  <part-list>\n")
        sb.append("    <score-part id=\"P1\">\n")
        sb.append("      <part-name>Music</part-name>\n")
        sb.append("    </score-part>\n")
        sb.append("  </part-list>\n")
        sb.append("  <part id=\"P1\">\n")

        var measureNumber = 1
        var totalDivisionsInMeasure = 0
        val divisionsPerMeasure = 16 // 4 beats * 4 divisions
        
        fun openMeasure(num: Int) {
            sb.append("    <measure number=\"$num\">\n")
            if (num == 1) {
                sb.append("      <attributes>\n")
                sb.append("        <divisions>4</divisions>\n")
                sb.append("        <key><fifths>0</fifths></key>\n")
                sb.append("        <time><beats(4)></beats><beat-type>4</beat-type></time>\n")
                sb.append("        <time><beats>4</beats><beat-type>4</beat-type></time>\n")
                sb.append("        <clef><sign>G</sign><line>2</line></clef>\n")
                sb.append("      </attributes>\n")
            }
        }
        
        // Re-writing the opening logic to fix XML tags
        val xmlHeader = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE score-partwise PUBLIC "-//Recordare//DTD MusicXML 4.0 Partwise//EN" "http://www.musicxml.org/dtds/partwise.dtd">
<score-partwise version="4.0">
  <part-list>
    <score-part id="P1">
      <part-name>Music</part-name>
    </score-part>
  </part-list>
  <part id="P1">
"""
        val finalSb = StringBuilder(xmlHeader)

        fun appendMeasureStart(num: Int, sb: StringBuilder) {
            sb.append("    <measure number=\"$num\">\n")
            if (num == 1) {
                sb.append("""      <attributes>
        <divisions>4</divisions>
        <key><fifths>0</fifths></key>
        <time><beats>4</beats><beat-type>4</beat-type></time>
        <clef><sign>G</sign><line>2</line></clef>
      </attributes>
""")
            }
        }

        appendMeasureStart(measureNumber, finalSb)

        for ((midi, duration, typeName) in notes) {
            if (totalDivisionsInMeasure + duration > divisionsPerMeasure) {
                // Fill rest of measure if needed (simplified: just start new measure)
                finalSb.append("    </measure>\n")
                measureNumber++
                appendMeasureStart(measureNumber, finalSb)
                totalDivisionsInMeasure = 0
            }

            val step = getStep(midi)
            val octave = (midi / 12) - 1
            val alter = getAlter(midi)
            
            finalSb.append("      <note>\n")
            finalSb.append("        <pitch>\n")
            finalSb.append("          <step>$step</step>\n")
            if (alter != 0) {
                finalSb.append("          <alter>$alter</alter>\n")
            }
            finalSb.append("          <octave>$octave</octave>\n")
            finalSb.append("        </pitch>\n")
            finalSb.append("        <duration>$duration</duration>\n")
            finalSb.append("        <type>$typeName</type>\n")
            finalSb.append("      </note>\n")
            
            totalDivisionsInMeasure += duration
        }

        finalSb.append("    </measure>\n")
        finalSb.append("  </part>\n")
        finalSb.append("</score-partwise>")
        
        return finalSb.toString()
    }

    private fun getStep(midi: Int): String {
        return when (midi % 12) {
            0, 1 -> "C"
            2, 3 -> "D"
            4 -> "E"
            5, 6 -> "F"
            7, 8 -> "G"
            9, 10 -> "A"
            11 -> "B"
            else -> "C"
        }
    }

    private fun getAlter(midi: Int): Int {
        return when (midi % 12) {
            1, 3, 6, 8, 10 -> 1
            else -> 0
        }
    }
}
