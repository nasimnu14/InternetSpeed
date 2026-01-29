package com.example.internetspeedtest.utils

import android.content.Context
import android.content.Intent
import com.example.internetspeedtest.models.SpeedTestResult

object SpeedTestUtils {

  /**
   * Share test results
   */
  fun shareResults(context: Context, result: SpeedTestResult) {
    val shareText = buildString {
      appendLine("🚀 My Internet Speed Test Results")
      appendLine()
      appendLine("⬇️ Download: ${formatSpeed(result.downloadSpeed)} Mbps")
      appendLine("⬆️ Upload: ${formatSpeed(result.uploadSpeed)} Mbps")
      appendLine("📡 Latency: ${result.latency.toInt()} ms")
      appendLine()
      appendLine("Test performed with Speed Test App")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, shareText)
      putExtra(Intent.EXTRA_SUBJECT, "My Internet Speed Test Results")
    }

    context.startActivity(Intent.createChooser(intent, "Share results via"))
  }

  /**
   * Format speed value for display
   */
  fun formatSpeed(speed: Double): String {
    return when {
      speed < 0.1 -> "0.0"
      speed < 1.0 -> String.format("%.2f", speed)
      speed < 10.0 -> String.format("%.1f", speed)
      else -> String.format("%.0f", speed)
    }
  }

  /**
   * Format bytes to human-readable format
   */
  fun formatBytes(bytes: Long): String {
    return when {
      bytes < 1024 -> "$bytes B"
      bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
      bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
      else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
  }

  /**
   * Get speed rating
   */
  fun getSpeedRating(downloadSpeed: Double): String {
    return when {
      downloadSpeed < 1 -> "Very Slow"
      downloadSpeed < 5 -> "Slow"
      downloadSpeed < 25 -> "Average"
      downloadSpeed < 100 -> "Fast"
      downloadSpeed < 500 -> "Very Fast"
      else -> "Ultra Fast"
    }
  }

  /**
   * Get speed emoji
   */
  fun getSpeedEmoji(downloadSpeed: Double): String {
    return when {
      downloadSpeed < 1 -> "🐌"
      downloadSpeed < 5 -> "🚶"
      downloadSpeed < 25 -> "🚗"
      downloadSpeed < 100 -> "🚄"
      downloadSpeed < 500 -> "✈️"
      else -> "🚀"
    }
  }

  /**
   * Calculate quality rating from speed
   */
  fun calculateQualityRating(downloadSpeed: Double): Int {
    return when {
      downloadSpeed < 1 -> 1
      downloadSpeed < 5 -> 2
      downloadSpeed < 25 -> 3
      downloadSpeed < 100 -> 4
      else -> 5
    }
  }

  /**
   * Get latency rating
   */
  fun getLatencyRating(latency: Double): String {
    return when {
      latency < 20 -> "Excellent"
      latency < 50 -> "Good"
      latency < 100 -> "Fair"
      else -> "Poor"
    }
  }
}