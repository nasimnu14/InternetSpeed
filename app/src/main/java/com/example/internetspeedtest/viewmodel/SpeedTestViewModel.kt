package com.example.internetspeedtest.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.internetspeedtest.models.*
import com.example.internetspeedtest.network.SpeedTestClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.round

class SpeedTestViewModel : ViewModel() {
  private val speedTestClient = SpeedTestClient()

  private val _uiState = MutableStateFlow<SpeedTestUiState>(SpeedTestUiState.Idle)
  val uiState: StateFlow<SpeedTestUiState> = _uiState.asStateFlow()

  private val _downloadSpeed = MutableStateFlow(0.0)
  val downloadSpeed: StateFlow<Double> = _downloadSpeed.asStateFlow()

  private val _uploadSpeed = MutableStateFlow(0.0)
  val uploadSpeed: StateFlow<Double> = _uploadSpeed.asStateFlow()

  private val _latency = MutableStateFlow(0.0)
  val latency: StateFlow<Double> = _latency.asStateFlow()

  private val _bytesDownloaded = MutableStateFlow(0L)
  val bytesDownloaded: StateFlow<Long> = _bytesDownloaded.asStateFlow()

  private val _bytesUploaded = MutableStateFlow(0L)
  val bytesUploaded: StateFlow<Long> = _bytesUploaded.asStateFlow()

  private val _testPhase = MutableStateFlow(TestPhase.IDLE)
  val testPhase: StateFlow<TestPhase> = _testPhase.asStateFlow()

  private val _showAdvanced = MutableStateFlow(false)
  val showAdvanced: StateFlow<Boolean> = _showAdvanced.asStateFlow()

  init {
    // Auto-start test when app opens
    startTest()
  }

  fun startTest() {
    viewModelScope.launch {
      try {
        _uiState.value = SpeedTestUiState.Testing
        _testPhase.value = TestPhase.DOWNLOAD

        // Reset values
        _downloadSpeed.value = 0.0
        _uploadSpeed.value = 0.0
        _latency.value = 0.0
        _bytesDownloaded.value = 0L
        _bytesUploaded.value = 0L

        // Measure latency
        _testPhase.value = TestPhase.LATENCY
        val latencyResult = speedTestClient.measureLatency()
        _latency.value = latencyResult

        // Measure download speed
        _testPhase.value = TestPhase.DOWNLOAD
        speedTestClient.measureDownloadSpeed { progress ->
          _downloadSpeed.value = round(progress.speed * 10) / 10
          _bytesDownloaded.value = progress.bytesTransferred
        }

        // Measure upload speed
        _testPhase.value = TestPhase.UPLOAD
        speedTestClient.measureUploadSpeed { progress ->
          _uploadSpeed.value = round(progress.speed * 10) / 10
          _bytesUploaded.value = progress.bytesTransferred
        }

        _testPhase.value = TestPhase.COMPLETE
        _uiState.value = SpeedTestUiState.Success(
          downloadSpeed = _downloadSpeed.value,
          uploadSpeed = _uploadSpeed.value,
          latency = _latency.value
        )
      } catch (e: Exception) {
        _uiState.value = SpeedTestUiState.Error(e.message ?: "Unknown error")
        _testPhase.value = TestPhase.ERROR
      }
    }
  }

  fun stopTest() {
    speedTestClient.cancel()
    _uiState.value = SpeedTestUiState.Idle
    _testPhase.value = TestPhase.IDLE
  }

  fun toggleAdvancedView() {
    _showAdvanced.value = !_showAdvanced.value
  }

  override fun onCleared() {
    super.onCleared()
    speedTestClient.cancel()
  }

  fun setTestPhase(testPhase: TestPhase) {
    stopTest()
    _testPhase.value = testPhase
  }
}

sealed class SpeedTestUiState {
  object Idle : SpeedTestUiState()
  object Testing : SpeedTestUiState()
  data class Success(
    val downloadSpeed: Double,
    val uploadSpeed: Double,
    val latency: Double
  ) : SpeedTestUiState()
  data class Error(val message: String) : SpeedTestUiState()
}

enum class TestPhase {
  IDLE,
  LATENCY,
  DOWNLOAD,
  UPLOAD,
  COMPLETE,
  ERROR
}