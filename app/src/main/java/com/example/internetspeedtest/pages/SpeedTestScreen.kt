package com.example.internetspeedtest.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.internetspeedtest.viewmodel.SpeedTestUiState
import com.example.internetspeedtest.viewmodel.SpeedTestViewModel
import com.example.internetspeedtest.viewmodel.TestPhase
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(
  viewModel: SpeedTestViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsState()
  val downloadSpeed by viewModel.downloadSpeed.collectAsState()
  val uploadSpeed by viewModel.uploadSpeed.collectAsState()
  val latency by viewModel.latency.collectAsState()
  val bytesDownloaded by viewModel.bytesDownloaded.collectAsState()
  val bytesUploaded by viewModel.bytesUploaded.collectAsState()
  val testPhase by viewModel.testPhase.collectAsState()
  val showAdvanced by viewModel.showAdvanced.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Speed Test") },
        actions = {
          IconButton(onClick = { viewModel.toggleAdvancedView() }) {
            Icon(
              imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
              contentDescription = "Toggle advanced view"
            )
          }
        }
      )
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.background),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
      ) {
        // Speed type
//        TestPhaseDropdown(selectedPhase = testPhase) {phase ->
//            viewModel.setTestPhase(phase)
//        }
        // Test Status Indicator
        TestStatusIndicator(testPhase)

        Spacer(modifier = Modifier.height(32.dp))

        // Main Speed Display
        when (testPhase) {
          TestPhase.DOWNLOAD -> {
            SpeedDisplay(
              speed = downloadSpeed,
              label = "Download",
              isActive = true
            )
          }
          TestPhase.UPLOAD -> {
            SpeedDisplay(
              speed = uploadSpeed,
              label = "Upload",
              isActive = true
            )
          }
          TestPhase.LATENCY -> {
            LatencyDisplay(latency = latency, isActive = true)
          }
          TestPhase.COMPLETE -> {
            SpeedDisplay(
              speed = downloadSpeed,
              label = "Download",
              isActive = false
            )
          }
          else -> {
            SpeedDisplay(
              speed = 0.0,
              label = "Starting",
              isActive = false
            )
          }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Control Button
        ControlButton(
          uiState = uiState,
          testPhase = testPhase,
          onStart = { viewModel.startTest() },
          onStop = { viewModel.stopTest() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Phase Indicator
        PhaseIndicator(testPhase)

        // Advanced Metrics
        AnimatedVisibility(
          visible = true , // showAdvanced && testPhase != TestPhase.IDLE,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(top = 24.dp)
          ) {
            Spacer(modifier = Modifier.padding(vertical = 16.dp))

            Text(
              text = "Advanced Metrics",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            MetricRow(
              label = "Download Speed",
              value = "${formatSpeed(downloadSpeed)} Mbps"
            )

            MetricRow(
              label = "Upload Speed",
              value = "${formatSpeed(uploadSpeed)} Mbps"
            )

            MetricRow(
              label = "Latency",
              value = "${latency.roundToInt()} ms"
            )

            MetricRow(
              label = "Downloaded",
              value = formatBytes(bytesDownloaded)
            )

            MetricRow(
              label = "Uploaded",
              value = formatBytes(bytesUploaded)
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestPhaseDropdown(
  selectedPhase: TestPhase,
  onPhaseSelected: (TestPhase) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded }
  ) {
    OutlinedTextField(
      modifier = Modifier
        .menuAnchor()
        .fillMaxWidth(),
      value = selectedPhase.name,
      onValueChange = {},
      readOnly = true,
      label = { Text("Test Phase") },
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
      }
    )

    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      TestPhase.values().forEach { phase ->
        DropdownMenuItem(
          text = { Text(phase.name) },
          onClick = {
            onPhaseSelected(phase)
            expanded = false
          }
        )
      }
    }
  }
}


@Composable
fun TestStatusIndicator(testPhase: TestPhase) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val alpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "alpha"
  )

  Box(
    modifier = Modifier
      .size(80.dp)
      .clip(CircleShape)
      .background(
        when (testPhase) {
          TestPhase.IDLE -> MaterialTheme.colorScheme.surfaceVariant
          TestPhase.COMPLETE -> Color(0xFF4CAF50)
          TestPhase.ERROR -> MaterialTheme.colorScheme.error
          else -> MaterialTheme.colorScheme.primary.copy(alpha = alpha)
        }
      ),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = when (testPhase) {
        TestPhase.IDLE -> Icons.Default.PlayArrow
        TestPhase.COMPLETE -> Icons.Default.Check
        TestPhase.ERROR -> Icons.Default.Warning
        else -> Icons.Default.FavoriteBorder
      },
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(40.dp)
    )
  }
}

@Composable
fun SpeedDisplay(
  speed: Double,
  label: String,
  isActive: Boolean
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.Center
    ) {
      Text(
        text = formatSpeed(speed),
        fontSize = 72.sp,
        fontWeight = FontWeight.Bold,
        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Mbps",
        fontSize = 24.sp,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 12.dp)
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = label,
      fontSize = 18.sp,
      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    )
  }
}

@Composable
fun LatencyDisplay(
  latency: Double,
  isActive: Boolean
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.Center
    ) {
      Text(
        text = latency.roundToInt().toString(),
        fontSize = 72.sp,
        fontWeight = FontWeight.Bold,
        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "ms",
        fontSize = 24.sp,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 12.dp)
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Latency",
      fontSize = 18.sp,
      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    )
  }
}

@Composable
fun ControlButton(
  uiState: SpeedTestUiState,
  testPhase: TestPhase,
  onStart: () -> Unit,
  onStop: () -> Unit
) {
  when (uiState) {
    is SpeedTestUiState.Testing -> {
      Button(
        onClick = onStop,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error
        ),
        modifier = Modifier
          .height(56.dp)
          .widthIn(min = 200.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Stop Test", fontSize = 16.sp)
      }
    }
    else -> {
      Button(
        onClick = onStart,
        modifier = Modifier
          .height(56.dp)
          .widthIn(min = 200.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (testPhase == TestPhase.COMPLETE) "Test Again" else "Start Test",
          fontSize = 16.sp
        )
      }
    }
  }
}

@Composable
fun PhaseIndicator(testPhase: TestPhase) {
  val phaseText = when (testPhase) {
    TestPhase.IDLE -> ""
    TestPhase.LATENCY -> "Measuring latency..."
    TestPhase.DOWNLOAD -> "Testing download speed..."
    TestPhase.UPLOAD -> "Testing upload speed..."
    TestPhase.COMPLETE -> "Test completed"
    TestPhase.ERROR -> "Test failed"
  }

  AnimatedVisibility(
    visible = phaseText.isNotEmpty(),
    enter = fadeIn(),
    exit = fadeOut()
  ) {
    Text(
      text = phaseText,
      fontSize = 14.sp,
      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
      textAlign = TextAlign.Center
    )
  }
}

@Composable
fun MetricRow(
  label: String,
  value: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onBackground
    )
  }
}

fun formatSpeed(speed: Double): String {
  return when {
    speed < 1.0 -> String.format("%.1f", speed)
    speed < 10.0 -> String.format("%.1f", speed)
    speed < 100.0 -> String.format("%.0f", speed)
    else -> String.format("%.0f", speed / 10 * 10)
  }
}

fun formatBytes(bytes: Long): String {
  return when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
    bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
  }
}

@Composable
@Preview(showBackground = true)
fun SpeedTestScreenPreview() {
  SpeedTestScreen()
}