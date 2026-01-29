package com.example.internetspeedtest.network

import com.example.internetspeedtest.models.SpeedTestProgress
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class SpeedTestClient {
  private var jobs = mutableListOf<Job>()
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  // Test servers - using public speed test servers
  private val testServers = listOf(
    "http://speedtest.ftp.otenet.gr/files/test100Mb.db",
    "http://speedtest.tele2.net/100MB.zip",
    "http://ipv4.download.thinkbroadband.com/100MB.zip"
  )

  // Configuration
  private val minConnections = 1
  private val maxConnections = 8
  private val minDuration = 5000L // 5 seconds
  private val maxDuration = 30000L // 30 seconds
  private val progressFrequency = 150L // ms

  private val _isRunning = MutableStateFlow(false)
  val isRunning: StateFlow<Boolean> = _isRunning

  private val snapshots = mutableListOf<SpeedSnapshot>()

  /**
   * Measure latency to test servers
   */
  suspend fun measureLatency(): Double {
    return withContext(Dispatchers.IO) {
      val latencies = mutableListOf<Long>()

      repeat(5) { // Take 5 measurements
        val server = testServers.random()
        val start = System.currentTimeMillis()

        try {
          val connection = URL(server).openConnection() as HttpURLConnection
          connection.requestMethod = "HEAD"
          connection.connectTimeout = 5000
          connection.readTimeout = 5000
          connection.connect()

          val end = System.currentTimeMillis()
          if (connection.responseCode == 200) {
            latencies.add(end - start)
          }
          connection.disconnect()
        } catch (e: Exception) {
          e.printStackTrace()
        }

        delay(100)
      }

      // Return median latency
      if (latencies.isEmpty()) return@withContext 0.0
      latencies.sorted()[latencies.size / 2].toDouble()
    }
  }

  /**
   * Measure download speed with progressive connection scaling
   */
  suspend fun measureDownloadSpeed(onProgress: (SpeedTestProgress) -> Unit) {
    withContext(Dispatchers.IO) {
      _isRunning.value = true
      snapshots.clear()

      val startTime = System.currentTimeMillis()
      var totalBytes = 0L
      var activeConnections = minConnections
      val measurements = mutableListOf<SpeedSnapshot>()

      val progressJob = launch {
        while (isActive) {
          delay(progressFrequency)

          val currentTime = System.currentTimeMillis()
          val duration = currentTime - startTime

          synchronized(measurements) {
            val recentMeasurements = measurements.filter {
              currentTime - it.end < 1000
            }

            if (recentMeasurements.isNotEmpty()) {
              val totalRecentBytes = recentMeasurements.sumOf { it.bytes }
              val totalRecentTime = recentMeasurements.sumOf { it.time }

              if (totalRecentTime > 0) {
                val speedBps = (totalRecentBytes * 8.0 * 1000.0) / totalRecentTime
                val speedMbps = speedBps / 1_000_000.0

                onProgress(SpeedTestProgress(speedMbps, totalBytes, duration))

                // Scale connections based on speed
                if (speedMbps > 50 && activeConnections < maxConnections) {
                  activeConnections = min(maxConnections, activeConnections + 2)
                } else if (speedMbps > 10 && activeConnections < 5) {
                  activeConnections = 5
                } else if (speedMbps > 1 && activeConnections < 3) {
                  activeConnections = 3
                }
              }
            }
          }

          // Check if test should stop
          if (duration > maxDuration) {
            cancel()
          } else if (duration > minDuration && isSpeedStable(measurements)) {
            cancel()
          }
        }
      }

      // Start download connections
      val connectionJobs = mutableListOf<Job>()
      repeat(activeConnections) { connectionId ->
        val job = launch {
          downloadWorker(connectionId, measurements) { bytes ->
            synchronized(this@SpeedTestClient) {
              totalBytes += bytes
            }
          }
        }
        connectionJobs.add(job)
        jobs.add(job)
      }

      // Monitor and add connections dynamically
      val monitorJob = launch {
        while (isActive && connectionJobs.size < maxConnections) {
          delay(1000)

          val currentActive = connectionJobs.count { it.isActive }
          val needed = activeConnections - currentActive

          if (needed > 0) {
            repeat(needed) { i ->
              val connectionId = connectionJobs.size + i
              val job = launch {
                downloadWorker(connectionId, measurements) { bytes ->
                  synchronized(this@SpeedTestClient) {
                    totalBytes += bytes
                  }
                }
              }
              connectionJobs.add(job)
              jobs.add(job)
            }
          }
        }
      }
      jobs.add(monitorJob)

      progressJob.join()
      connectionJobs.forEach { it.cancelAndJoin() }
      monitorJob.cancelAndJoin()

      _isRunning.value = false
    }
  }

  /**
   * Download worker - downloads data from test server
   */
  private suspend fun downloadWorker(
    connectionId: Int,
    measurements: MutableList<SpeedSnapshot>,
    onBytesDownloaded: (Long) -> Unit
  ) {
    withContext(Dispatchers.IO) {
      var connection: HttpURLConnection? = null

      try {
        val server = testServers[connectionId % testServers.size]
        connection = URL(server).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        // Add range header to download in chunks
        val rangeStart = Random.nextInt(0, 10_000_000)
        val rangeEnd = rangeStart + 10_000_000
        connection.setRequestProperty("Range", "bytes=$rangeStart-$rangeEnd")

        connection.connect()

        val inputStream: InputStream = connection.inputStream
        val buffer = ByteArray(8192)
        var bytesRead: Int = 0
        var totalBytesRead = 0L
        val startTime = System.currentTimeMillis()
        var lastMeasurementTime = startTime
        var bytesInWindow = 0L

        while (isActive && inputStream.read(buffer).also { bytesRead = it } != -1) {
          totalBytesRead += bytesRead
          bytesInWindow += bytesRead
          onBytesDownloaded(bytesRead.toLong())

          val currentTime = System.currentTimeMillis()
          val windowDuration = currentTime - lastMeasurementTime

          // Record measurement every 200ms
          if (windowDuration >= 200) {
            synchronized(measurements) {
              measurements.add(
                SpeedSnapshot(
                  bytes = bytesInWindow,
                  time = windowDuration,
                  start = lastMeasurementTime,
                  end = currentTime
                )
              )
            }
            bytesInWindow = 0L
            lastMeasurementTime = currentTime
          }
        }

        inputStream.close()
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        connection?.disconnect()
      }
    }
  }

  /**
   * Measure upload speed
   */
  suspend fun measureUploadSpeed(onProgress: (SpeedTestProgress) -> Unit) {
    withContext(Dispatchers.IO) {
      _isRunning.value = true
      snapshots.clear()

      val startTime = System.currentTimeMillis()
      var totalBytes = 0L
      val measurements = mutableListOf<SpeedSnapshot>()

      val progressJob = launch {
        while (isActive) {
          delay(progressFrequency)

          val currentTime = System.currentTimeMillis()
          val duration = currentTime - startTime

          synchronized(measurements) {
            val recentMeasurements = measurements.filter {
              currentTime - it.end < 1000
            }

            if (recentMeasurements.isNotEmpty()) {
              val totalRecentBytes = recentMeasurements.sumOf { it.bytes }
              val totalRecentTime = recentMeasurements.sumOf { it.time }

              if (totalRecentTime > 0) {
                val speedBps = (totalRecentBytes * 8.0 * 1000.0) / totalRecentTime
                val speedMbps = speedBps / 1_000_000.0

                onProgress(SpeedTestProgress(speedMbps, totalBytes, duration))
              }
            }
          }

          // Stop after min duration for upload (shorter than download)
          if (duration > 10000L) {
            cancel()
          }
        }
      }

      // Start upload connections (fewer than download)
      val connectionJobs = mutableListOf<Job>()
      repeat(min(3, maxConnections)) { connectionId ->
        val job = launch {
          uploadWorker(connectionId, measurements) { bytes ->
            synchronized(this@SpeedTestClient) {
              totalBytes += bytes
            }
          }
        }
        connectionJobs.add(job)
        jobs.add(job)
      }

      progressJob.join()
      connectionJobs.forEach { it.cancelAndJoin() }

      _isRunning.value = false
    }
  }

  /**
   * Upload worker - simulates upload by generating and "sending" data
   */
  private suspend fun uploadWorker(
    connectionId: Int,
    measurements: MutableList<SpeedSnapshot>,
    onBytesUploaded: (Long) -> Unit
  ) {
    withContext(Dispatchers.IO) {
      try {
        // Note: Real upload testing requires a server endpoint that accepts POST data
        // This is a simulation
        val buffer = ByteArray(8192)
        Random.nextBytes(buffer)

        var totalBytesUploaded = 0L
        val startTime = System.currentTimeMillis()
        var lastMeasurementTime = startTime
        var bytesInWindow = 0L

        while (isActive) {
          // Simulate upload
          delay(10) // Simulate network delay
          val bytesUploaded = buffer.size.toLong()
          totalBytesUploaded += bytesUploaded
          bytesInWindow += bytesUploaded
          onBytesUploaded(bytesUploaded)

          val currentTime = System.currentTimeMillis()
          val windowDuration = currentTime - lastMeasurementTime

          if (windowDuration >= 200) {
            synchronized(measurements) {
              measurements.add(
                SpeedSnapshot(
                  bytes = bytesInWindow,
                  time = windowDuration,
                  start = lastMeasurementTime,
                  end = currentTime
                )
              )
            }
            bytesInWindow = 0L
            lastMeasurementTime = currentTime
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  /**
   * Check if speed measurements are stable
   */
  private fun isSpeedStable(measurements: List<SpeedSnapshot>): Boolean {
    if (measurements.size < 10) return false

    val recentMeasurements = measurements.takeLast(10)
    val speeds = recentMeasurements.map { snapshot ->
      if (snapshot.time > 0) {
        (snapshot.bytes * 8.0 * 1000.0) / snapshot.time / 1_000_000.0
      } else 0.0
    }

    if (speeds.isEmpty()) return false

    val avgSpeed = speeds.average()
    val maxDelta = speeds.maxOfOrNull { kotlin.math.abs(it - avgSpeed) / avgSpeed * 100 } ?: 0.0

    return maxDelta < 5.0 // Less than 5% variation
  }

  /**
   * Cancel all ongoing tests
   */
  fun cancel() {
    jobs.forEach { it.cancel() }
    jobs.clear()
    _isRunning.value = false
  }
}

data class SpeedSnapshot(
  val bytes: Long,
  val time: Long,
  val start: Long,
  val end: Long
)