package com.example.lab1_task2.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.lab1_task2.audio.AudioRecorder
import com.example.lab1_task2.databinding.FragmentTranscriptionBinding
import com.example.lab1_task2.model.TranscriptionState
import com.example.lab1_task2.viewmodel.TranscriptionViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Base64

class TranscriptionFragment : Fragment() {

    private var _binding: FragmentTranscriptionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TranscriptionViewModel by viewModels()
    private lateinit var audioRecorder: AudioRecorder
    private var mediaPlayer: MediaPlayer? = null
    
    private var lastMusicXml: String? = null
    private var lastFileName: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecordingProcess()
        }
    }

    override fun onCreateView(
        LayoutInflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTranscriptionBinding.inflate(LayoutInflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        audioRecorder = AudioRecorder(requireContext())
        
        setupWebView()
        setupUI()
        observeViewModel()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.wvSheetMusic.apply {
            settings.javaScriptEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = WebViewClient()
        }
    }

    private fun setupUI() {
        binding.btnRecord.setOnClickListener {
            checkAndRequestPermission()
        }

        binding.btnStop.setOnClickListener {
            audioRecorder.stopRecording()
            viewModel.onRecordingStopped()
        }

        binding.btnPlayback.setOnClickListener {
            playRecording()
        }

        binding.btnTranscribe.setOnClickListener {
            val wavPath = audioRecorder.getWavFilePath()
            if (wavPath != null) {
                viewModel.transcribe(
                    audioRecorder.getOnsetTimes(),
                    audioRecorder.pitchDetector.getReadings(),
                    wavPath
                )
            }
        }
        
        binding.btnViewXml.setOnClickListener {
            lastMusicXml?.let { renderSheetMusic(it) }
        }

        binding.btnShowCode.setOnClickListener {
            showXmlCodeDialog()
        }
        
        // Add a long-click listener to the file name to share it
        binding.tvFileName.setOnLongClickListener {
            shareMusicXml()
            true
        }
    }

    private fun shareMusicXml() {
        val fileName = lastFileName ?: return
        val file = File(requireContext().filesDir, fileName)
        if (!file.exists()) return

        val uri: Uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share MusicXML"))
    }

    private fun showXmlCodeDialog() {
        val xml = lastMusicXml ?: return
        val textView = TextView(requireContext()).apply {
            text = xml
            setPadding(32, 32, 32, 32)
            textSize = 10f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        val scrollView = ScrollView(requireContext()).apply {
            addView(textView)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("MusicXML Code")
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .setNeutralButton("Share") { _, _ -> shareMusicXml() }
            .show()
    }

    private fun checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startRecordingProcess()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecordingProcess() {
        audioRecorder.startRecording()
        viewModel.onRecordingStarted()
        binding.cvResult.visibility = View.GONE
        binding.tvDistortionWarning.visibility = View.GONE
        
        // Monitor for distortion during recording
        viewLifecycleOwner.lifecycleScope.launch {
            while (audioRecorder.isRecording()) {
                if (audioRecorder.isDistortionDetected()) {
                    binding.tvDistortionWarning.visibility = View.VISIBLE
                }
                delay(200)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            updateUIState(state)
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            binding.tvStatus.text = message
        }

        viewModel.transcriptionResult.observe(viewLifecycleOwner) { result ->
            if (result != null && result.musicXml.isNotEmpty()) {
                lastMusicXml = result.musicXml
                lastFileName = "transcription_${result.timestampMs}.xml"
                saveMusicXmlAndNotify(result.musicXml, result.timestampMs)
            }
        }
    }

    private fun saveMusicXmlAndNotify(musicXml: String, timestamp: Long) {
        val fileName = "transcription_$timestamp.xml"
        lastFileName = fileName
        
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val file = File(requireContext().filesDir, fileName)
                file.writeText(musicXml)
            }

            binding.cvResult.visibility = View.VISIBLE
            binding.tvFileName.text = "File: $fileName (Long-press to share)"
            
            renderSheetMusic(musicXml)
            Snackbar.make(binding.root, "Sheet music generated!", Snackbar.LENGTH_LONG)
                .setAction("SHARE") { shareMusicXml() }
                .show()
        }
    }

    private fun renderSheetMusic(xml: String) {
        val encodedXml = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(xml.toByteArray())
        } else {
            android.util.Base64.encodeToString(xml.toByteArray(), android.util.Base64.NO_WRAP)
        }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <script src="https://cdn.jsdelivr.net/npm/opensheetmusicdisplay@1.8.8/build/opensheetmusicdisplay.min.js"></script>
                <style>
                    body { margin: 0; padding: 10px; background: white; }
                    #osmd-container { width: 100%; }
                </style>
            </head>
            <body>
                <div id="osmd-container"></div>
                <script>
                    window.onload = function() {
                        var osmd = new opensheetmusicdisplay.OpenSheetMusicDisplay("osmd-container", {
                            drawTitle: false,
                            drawSubtitle: false,
                            drawComposer: false,
                            drawMetronomeMarks: false,
                            backend: "svg"
                        });
                        var xmlData = atob('$encodedXml');
                        osmd.load(xmlData).then(function() {
                            osmd.render();
                        });
                    };
                </script>
            </body>
            </html>
        """.trimIndent()

        binding.wvSheetMusic.loadDataWithBaseURL("https://localhost", html, "text/html", "UTF-8", null)
    }

    private fun updateUIState(state: TranscriptionState) {
        when (state) {
            TranscriptionState.IDLE -> {
                binding.btnRecord.isEnabled = true
                binding.btnStop.isEnabled = false
                binding.btnPlayback.isEnabled = false
                binding.btnTranscribe.isEnabled = false
                binding.btnTranscribe.visibility = View.GONE
                binding.tvDistortionWarning.visibility = View.GONE
            }
            TranscriptionState.RECORDING -> {
                binding.btnRecord.isEnabled = false
                binding.btnStop.isEnabled = true
                binding.btnPlayback.isEnabled = false
                binding.btnTranscribe.isEnabled = false
                binding.btnTranscribe.visibility = View.GONE
                binding.tvStatus.text = "Recording..."
            }
            TranscriptionState.RECORDED -> {
                binding.btnRecord.isEnabled = true
                binding.btnStop.isEnabled = false
                binding.btnPlayback.isEnabled = true
                binding.btnTranscribe.isEnabled = true
                binding.btnTranscribe.visibility = View.VISIBLE
            }
            is TranscriptionState.PROCESSING -> {
                binding.btnRecord.isEnabled = false
                binding.btnStop.isEnabled = false
                binding.btnPlayback.isEnabled = false
                binding.btnTranscribe.isEnabled = false
                binding.btnTranscribe.visibility = View.VISIBLE
                binding.tvDistortionWarning.visibility = View.GONE
            }
        }
    }

    private fun playRecording() {
        val path = audioRecorder.getWavFilePath() ?: return
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            start()
            setOnCompletionListener {
                it.release()
                mediaPlayer = null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        audioRecorder.stopRecording()
        _binding = null
    }
}
