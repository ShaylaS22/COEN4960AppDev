package com.example.lab1_task2.model

data class TranscriptionResult(
    val musicXml: String,
    val wavFilePath: String,
    val timestampMs: Long
) {
    companion object {
        fun empty(): TranscriptionResult {
            return TranscriptionResult(
                musicXml = "",
                wavFilePath = "",
                timestampMs = 0L
            )
        }
    }
}
