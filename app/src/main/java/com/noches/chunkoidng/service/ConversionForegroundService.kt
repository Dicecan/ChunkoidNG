package com.noches.chunkoidng.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.noches.chunkoidng.MainActivity
import com.noches.chunkoidng.R
import com.noches.chunkoidng.core.conversion.ConversionConfig
import com.noches.chunkoidng.core.conversion.ConversionEngine
import com.noches.chunkoidng.core.conversion.ConversionEvent
import com.noches.chunkoidng.core.runtime.JavaProcessManager
import com.noches.chunkoidng.core.runtime.JavaRuntimeEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground service ensuring world conversions finish smoothly without being killed in background.
 */
class ConversionForegroundService : Service() {
    private val TAG = "ConversionService"
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private var conversionJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var engine: ConversionEngine? = null
    private var lastNotificationAt = 0L
    private var lastNotificationPercent = -1

    private val _progressPercent = MutableStateFlow(0)
    val progressPercent: StateFlow<Int> = _progressPercent.asStateFlow()

    private val _statusMessage = MutableStateFlow("准备就绪")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _events = MutableSharedFlow<ConversionEvent>(replay = 50)
    val events: SharedFlow<ConversionEvent> = _events.asSharedFlow()

    inner class LocalBinder : Binder() {
        fun getService(): ConversionForegroundService = this@ConversionForegroundService
    }

    override fun onCreate() {
        super.onCreate()
        val runtimeEnv = JavaRuntimeEnvironment(this)
        val processManager = JavaProcessManager(this, runtimeEnv)
        engine = ConversionEngine(this, runtimeEnv, processManager)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun startConversion(config: ConversionConfig) {
        if (_isRunning.value) return
        startConversionInternal(config)
    }

    private fun startConversionInternal(config: ConversionConfig) {

        _isRunning.value = true
        _progressPercent.value = 0
        _statusMessage.value = "正在初始化..."
        acquireWakeLock()

        val notification = buildNotification("正在启动转换引擎...", 0, indeterminate = true)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error", e)
        }

        conversionJob?.cancel()
        conversionJob = serviceScope.launch {
            try {
                engine?.execute(config)?.collect { event ->
                    _events.emit(event)
                    when (event) {
                        is ConversionEvent.Progress -> {
                            _progressPercent.value = event.percent
                            _statusMessage.value = event.stage
                            updateNotificationThrottled("${event.stage} (${event.percent}%)", event.percent)
                        }
                        is ConversionEvent.Success -> {
                            _progressPercent.value = 100
                            _statusMessage.value = "转换成功"
                            updateNotification("转换已圆满完成！", 100, false)
                            finishService(success = true)
                        }
                        is ConversionEvent.Failure -> {
                            _statusMessage.value = "转换失败: ${event.error}"
                            updateNotification("转换遇到错误", 0, false)
                            finishService(success = false)
                        }
                        is ConversionEvent.LogOutput -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Conversion job threw exception", e)
                _events.emit(ConversionEvent.Failure("转换异常: ${e.message}"))
                finishService(success = false)
            }
        }
    }

    fun cancelConversion() {
        engine?.cancel()
        conversionJob?.cancel()
        _statusMessage.value = "用户已取消转换"
        _isRunning.value = false
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun finishService(success: Boolean) {
        _isRunning.value = false
        releaseWakeLock()
        stopForeground(if (success) STOP_FOREGROUND_DETACH else STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "世界转换后台服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持 Minecraft 存档转换在后台持续运行"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String, progress: Int, indeterminate: Boolean): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Chunkoid - 世界存档转换中")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, indeterminate)
            .build()
    }

    private fun updateNotification(statusText: String, progress: Int, indeterminate: Boolean) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(NOTIFICATION_ID, buildNotification(statusText, progress, indeterminate))
        } catch (_: Exception) {}
    }

    private fun updateNotificationThrottled(statusText: String, progress: Int) {
        val now = System.currentTimeMillis()
        if (progress == lastNotificationPercent && now - lastNotificationAt < 500L) return
        if (now - lastNotificationAt < 250L && progress < 100) return
        lastNotificationAt = now
        lastNotificationPercent = progress
        updateNotification(statusText, progress, false)
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ChunkoidNG::ConversionWakeLock")
        }
        wakeLock?.let {
            if (!it.isHeld) it.acquire(4 * 60 * 60 * 1000L) // 4 hours limit
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }

    override fun onDestroy() {
        cancelConversion()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "chunkoid_conversion_channel"
        private const val NOTIFICATION_ID = 2001
    }
}
