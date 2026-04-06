package com.example.lab1_task2.model

data class TuningResult(
    val detectedNote: String,        // e.g., "A4"
    val frequency: Float,             // detected Hz
    val targetFrequency: Float,       // correct Hz for the note
    val centsOff: Float,             // how far off-tune (-50 to +50)
    val isInTune: Boolean,           // true if within ±5 cents
    val confidence: Float,            // 0.0 to 1.0
    val timestamp: Long = System.currentTimeMillis()
)
