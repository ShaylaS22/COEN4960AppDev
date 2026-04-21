package com.example.lab1_task2.model

sealed class TranscriptionState {
    object IDLE : TranscriptionState()
    object RECORDING : TranscriptionState()
    object RECORDED : TranscriptionState()
    data class PROCESSING(val progressMessage: String? = null) : TranscriptionState()
}
