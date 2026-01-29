package com.example.internetspeedtest.pages

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.internetspeedtest.models.SpeedTestResult
import com.example.internetspeedtest.utils.SpeedTestUtils

@Composable
fun ResultCard(
  result: SpeedTestResult,
  onShare: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Test Results",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )

        Text(
          text = SpeedTestUtils.getSpeedEmoji(result.downloadSpeed),
          fontSize = 32.sp
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Speed Rating
      val rating = SpeedTestUtils.getSpeedRating(result.downloadSpeed)
      Text(
        text = "Your connection is $rating",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Metrics Grid
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        MetricItem(
          icon = Icons.Default.Build,
          label = "Download",
          value = "${SpeedTestUtils.formatSpeed(result.downloadSpeed)} Mbps",
          color = Color(0xFF4CAF50)
        )

        MetricItem(
          icon = Icons.Default.KeyboardArrowUp,
          label = "Upload",
          value = "${SpeedTestUtils.formatSpeed(result.uploadSpeed)} Mbps",
          color = Color(0xFF2196F3)
        )

        MetricItem(
          icon = Icons.Outlined.Refresh,
          label = "Latency",
          value = "${result.latency.toInt()} ms",
          color = Color(0xFFFF9800)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Quality Bars
      QualityBars(
        downloadSpeed = result.downloadSpeed,
        latency = result.latency
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Share Button
      Button(
        onClick = onShare,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        )
      ) {
        Icon(
          imageVector = Icons.Default.Share,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Share Results")
      }
    }
  }
}

@Composable
fun MetricItem(
  icon: ImageVector,
  label: String,
  value: String,
  color: Color
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.width(100.dp)
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(color.copy(alpha = 0.1f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = color,
        modifier = Modifier.size(24.dp)
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
fun QualityBars(
  downloadSpeed: Double,
  latency: Double
) {
  Column {
    // Download Quality
    QualityBar(
      label = "Download Quality",
      rating = SpeedTestUtils.calculateQualityRating(downloadSpeed),
      maxRating = 5,
      color = Color(0xFF4CAF50)
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Latency Quality
    val latencyRating = when {
      latency < 20 -> 5
      latency < 50 -> 4
      latency < 100 -> 3
      latency < 150 -> 2
      else -> 1
    }

    QualityBar(
      label = "Latency Quality - ${SpeedTestUtils.getLatencyRating(latency)}",
      rating = latencyRating,
      maxRating = 5,
      color = Color(0xFFFF9800)
    )
  }
}

@Composable
fun QualityBar(
  label: String,
  rating: Int,
  maxRating: Int,
  color: Color
) {
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      repeat(maxRating) { index ->
        Box(
          modifier = Modifier
            .weight(1f)
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
              if (index < rating) color else color.copy(alpha = 0.2f)
            )
        )
      }
    }
  }
}