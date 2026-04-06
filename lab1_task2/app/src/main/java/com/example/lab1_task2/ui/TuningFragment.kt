package com.example.lab1_task2.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.lab1_task2.databinding.FragmentTuningBinding
import com.example.lab1_task2.model.TuningResult
import com.example.lab1_task2.model.TuningState
import com.example.lab1_task2.viewmodel.TuningViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TuningFragment : Fragment() {

    private var _binding: FragmentTuningBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TuningViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startTuningProcess()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreateView(
        LayoutInflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTuningBinding.inflate(LayoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeTuningState()
    }

    private fun setupUI() {
        binding.btnStartStop.setOnClickListener {
            if (viewModel.tuningState.value is TuningState.Idle) {
                checkAndRequestPermission()
            } else {
                viewModel.stopTuning()
            }
        }
    }

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startTuningProcess()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startTuningProcess() {
        viewModel.startTuning()
    }

    private fun observeTuningState() {
        viewModel.tuningState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TuningState.Idle -> {
                    resetUI()
                    updateButtonState(false)
                }
                is TuningState.Listening -> {
                    updateButtonState(true)
                    state.currentResult?.let { updateUI(it) }
                }
                is TuningState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    viewModel.stopTuning()
                }
            }
        }
    }

    private fun updateUI(result: TuningResult) {
        binding.tvCurrentNote.text = result.detectedNote
        binding.tvFrequency.text = "${result.frequency.toInt()} Hz"
        
        val centsText = if (result.centsOff > 0) {
            "♯ ${result.centsOff.toInt()} cents"
        } else if (result.centsOff < 0) {
            "♭ ${kotlin.math.abs(result.centsOff).toInt()} cents"
        } else {
            "0 cents"
        }
        binding.tvCentsOff.text = centsText
        
        binding.tuningNeedle.setCentsOff(result.centsOff)
        
        // Update note color based on tuning
        if (result.isInTune) {
            binding.tvCurrentNote.setTextColor(Color.GREEN)
        } else {
            binding.tvCurrentNote.setTextColor(ContextCompat.getColor(requireContext(), com.google.android.material.R.color.design_default_color_primary))
        }

        binding.progressBarAccuracy.progress = (result.confidence * 100).toInt()
    }

    private fun resetUI() {
        binding.tvCurrentNote.text = "--"
        binding.tvFrequency.text = "--- Hz"
        binding.tvCentsOff.text = "♭ 0 cents ♯"
        binding.tuningNeedle.setCentsOff(0f)
        binding.tvCurrentNote.setTextColor(ContextCompat.getColor(requireContext(), com.google.android.material.R.color.design_default_color_primary))
        binding.progressBarAccuracy.progress = 0
    }

    private fun updateButtonState(isRecording: Boolean) {
        binding.btnStartStop.text = if (isRecording) "Stop Tuning" else "Start Tuning"
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Microphone Permission Required")
            .setMessage("This app needs microphone access to detect musical pitches for tuning your instrument.")
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopTuning()
        _binding = null
    }
}
