package com.example.lab1_task2.model

sealed class TuningState {
    object Idle : TuningState()
    data class Listening(val currentResult: TuningResult?) : TuningState()
    data class Error(val message: String) : TuningState()
}
