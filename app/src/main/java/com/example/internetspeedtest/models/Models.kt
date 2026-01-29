package com.example.internetspeedtest.models

data class SpeedTestProgress(
  val speed: Double, // Mbps
  val bytesTransferred: Long,
  val duration: Long // milliseconds
)

data class SpeedTestResult(
  val downloadSpeed: Double, // Mbps
  val uploadSpeed: Double, // Mbps
  val latency: Double, // ms
  val bytesDownloaded: Long,
  val bytesUploaded: Long,
  val timestamp: Long = System.currentTimeMillis()
)

data class LatencyMeasurement(
  val latency: Double, // ms
  val timestamp: Long = System.currentTimeMillis()
)

data class SpeedSnapshot(
  val bytes: Long,
  val time: Long, // milliseconds
  val start: Long,
  val end: Long
)

data class ConnectionMetrics(
  val connectionId: Int,
  val bytesTransferred: Long,
  val duration: Long,
  val speed: Double
)