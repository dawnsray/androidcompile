package com.example.levelcheck.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.levelcheck.data.model.LevelData
import com.example.levelcheck.data.model.NetworkStatus
import com.example.levelcheck.data.model.SensorData
import com.example.levelcheck.data.repository.NetworkRepository
import com.example.levelcheck.data.repository.PreferenceRepository
import com.example.levelcheck.domain.calibration.CalibrationManager
import com.example.levelcheck.domain.sensor.SensorProcessor
import com.example.levelcheck.util.Constants
import com.example.levelcheck.util.round
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 传感器前台服务 - 负责后台持续采集传感器数据并上传
 */
class SensorService : Service() {
    
    private val TAG = "SensorService"
    
    // 协程作用域
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // 依赖组件
    private lateinit var preferenceRepository: PreferenceRepository
    private lateinit var networkRepository: NetworkRepository
    private lateinit var calibrationManager: CalibrationManager
    private lateinit var sensorProcessor: SensorProcessor
    private lateinit var notificationManager: NotificationManager
    private lateinit var localBroadcastManager: LocalBroadcastManager
    
    // 数据状态
    private var currentSensorData: SensorData? = null
    private var currentNetworkStatus: NetworkStatus = NetworkStatus.UNKNOWN
    private var lastUploadTime: Long = 0L
    private var isPaused = false
    
    // Handler用于定时任务
    private val mainHandler = Handler(Looper.getMainLooper())
    private var uploadRunnable: Runnable? = null
    private var networkMonitorRunnable: Runnable? = null
    private var uiUpdateRunnable: Runnable? = null
    
    // 通知更新控制
    private var lastNotificationUpdate = 0L
    private var lastNotificationTilt = 0
    private var lastNotificationDirection = ""
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        
        // 初始化依赖
        preferenceRepository = PreferenceRepository(this)
        networkRepository = NetworkRepository(preferenceRepository)
        calibrationManager = CalibrationManager(preferenceRepository)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorProcessor = SensorProcessor(sensorManager, calibrationManager) { sensorData ->
            onSensorDataUpdate(sensorData)
        }
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 创建通知渠道
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        
        when (intent?.action) {
            Constants.ACTION_STOP_SERVICE -> {
                stopService()
                return START_NOT_STICKY
            }
            Constants.ACTION_UPDATE_CONFIG -> {
                updateConfig()
                return START_STICKY
            }
        }
        
        // 启动前台服务
        startForeground(Constants.NOTIFICATION_ID, createNotification())
        
        // 注册传感器
        if (sensorProcessor.register()) {
            // 立即广播初始状态
            broadcastInitialStatus()
            startDataCollection()
        } else {
            Log.e(TAG, "Failed to register sensors, stopping service")
            stopSelf()
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        
        // 停止所有定时任务
        stopAllTasks()
        
        // 注销传感器
        sensorProcessor.unregister()
        
        // 取消协程
        serviceScope.cancel()
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示水平仪数据采集状态"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建通知
     */
    private fun createNotification(): Notification {
        // 停止服务的Intent
        val stopIntent = Intent(this, SensorService::class.java).apply {
            action = Constants.ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 通知内容
        val contentText = buildNotificationContent()
        
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("水平仪数据采集中")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_delete,
                "停止",
                stopPendingIntent
            )
            .build()
    }
    
    /**
     * 构建通知内容文本
     */
    private fun buildNotificationContent(): String {
        val data = currentSensorData
        val statusText = when (currentNetworkStatus) {
            NetworkStatus.CONNECTED -> "正常"
            NetworkStatus.CONNECTING -> "连接中"
            NetworkStatus.DISCONNECTED -> "异常"
            NetworkStatus.UNKNOWN -> "未知"
        }
        
        return if (data != null) {
            "${data.direction} ${data.tiltCalibratedDegrees.toInt()}° | 网络:$statusText"
        } else {
            "正在初始化... | 网络:$statusText"
        }
    }
    
    /**
     * 更新通知
     */
    private fun updateNotification() {
        val now = System.currentTimeMillis()
        val data = currentSensorData ?: return
        
        // 限制更新频率
        val timeSinceLastUpdate = now - lastNotificationUpdate
        val tiltChanged = abs(data.tiltCalibratedDegrees.toInt() - lastNotificationTilt) >= 
            Constants.NOTIFICATION_TILT_CHANGE_THRESHOLD
        val directionChanged = data.direction != lastNotificationDirection
        
        if (timeSinceLastUpdate >= Constants.NOTIFICATION_UPDATE_INTERVAL_MS ||
            tiltChanged || directionChanged) {
            
            val notification = createNotification()
            notificationManager.notify(Constants.NOTIFICATION_ID, notification)
            
            lastNotificationUpdate = now
            lastNotificationTilt = data.tiltCalibratedDegrees.toInt()
            lastNotificationDirection = data.direction
        }
    }
    
    /**
     * 启动数据采集
     */
    private fun startDataCollection() {
        Log.d(TAG, "Starting data collection")
        isPaused = false
        scheduleNextUpload()
        startUiUpdateBroadcast()
    }
    
    /**
     * 调度下次上传
     */
    private fun scheduleNextUpload() {
        if (isPaused) {
            return
        }
        
        uploadRunnable?.let { mainHandler.removeCallbacks(it) }
        
        val intervalMs = preferenceRepository.getUploadInterval() * 1000L
        
        uploadRunnable = Runnable {
            performUpload()
        }
        
        mainHandler.postDelayed(uploadRunnable!!, intervalMs)
    }
    
    /**
     * 执行数据上传
     */
    private fun performUpload() {
        val data = currentSensorData ?: run {
            scheduleNextUpload()
            return
        }
        
        val levelData = LevelData(
            azimuth = data.azimuthDegrees.toDouble().round(1),
            tilt = data.tiltCalibratedDegrees.toInt(),
            timestamp = System.currentTimeMillis()
        )
        
        serviceScope.launch(Dispatchers.IO) {
            val status = networkRepository.uploadData(levelData)
            
            launch(Dispatchers.Main) {
                handleUploadResult(status)
            }
        }
    }
    
    /**
     * 处理上传结果
     */
    private fun handleUploadResult(status: NetworkStatus) {
        currentNetworkStatus = status
        
        when (status) {
            NetworkStatus.CONNECTED -> {
                // 上传成功
                lastUploadTime = System.currentTimeMillis()
                updateNotification()
                broadcastNetworkStatus()
                scheduleNextUpload()
            }
            NetworkStatus.CONNECTING -> {
                // 正在重试
                updateNotification()
                broadcastNetworkStatus()
                performUpload() // 继续重试
            }
            NetworkStatus.DISCONNECTED -> {
                // 网络异常，暂停采集
                pauseDataCollection()
                updateNotification()
                broadcastNetworkStatus()
                startNetworkMonitoring()
            }
            NetworkStatus.UNKNOWN -> {
                scheduleNextUpload()
            }
        }
    }
    
    /**
     * 暂停数据采集
     */
    private fun pauseDataCollection() {
        Log.d(TAG, "Pausing data collection")
        isPaused = true
        uploadRunnable?.let { mainHandler.removeCallbacks(it) }
    }
    
    /**
     * 启动网络监控
     */
    private fun startNetworkMonitoring() {
        Log.d(TAG, "Starting network monitoring")
        
        networkMonitorRunnable?.let { mainHandler.removeCallbacks(it) }
        
        networkMonitorRunnable = Runnable {
            checkNetworkRecovery()
        }
        
        mainHandler.postDelayed(
            networkMonitorRunnable!!,
            Constants.NETWORK_MONITOR_INTERVAL_SECONDS * 1000
        )
    }
    
    /**
     * 检查网络恢复
     */
    private fun checkNetworkRecovery() {
        serviceScope.launch(Dispatchers.IO) {
            val recovered = networkRepository.probeNetwork()
            
            launch(Dispatchers.Main) {
                if (recovered) {
                    resumeDataCollection()
                } else {
                    // 继续监控
                    startNetworkMonitoring()
                }
            }
        }
    }
    
    /**
     * 恢复数据采集
     */
    private fun resumeDataCollection() {
        Log.d(TAG, "Resuming data collection")
        networkMonitorRunnable?.let { mainHandler.removeCallbacks(it) }
        currentNetworkStatus = NetworkStatus.CONNECTED
        updateNotification()
        broadcastNetworkStatus()
        startDataCollection()
    }
    
    /**
     * 传感器数据更新回调
     */
    private fun onSensorDataUpdate(sensorData: SensorData) {
        currentSensorData = sensorData
    }
    
    /**
     * 启动UI更新广播
     */
    private fun startUiUpdateBroadcast() {
        uiUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
        
        uiUpdateRunnable = Runnable {
            broadcastSensorData()
            updateNotification()
            mainHandler.postDelayed(uiUpdateRunnable!!, Constants.UI_UPDATE_INTERVAL_MS)
        }
        
        mainHandler.post(uiUpdateRunnable!!)
    }
    
    /**
     * 广播传感器数据
     */
    private fun broadcastSensorData() {
        val data = currentSensorData ?: return
        
        val intent = Intent(Constants.ACTION_SENSOR_DATA_UPDATE).apply {
            putExtra(Constants.EXTRA_DIRECTION, data.direction)
            putExtra(Constants.EXTRA_AZIMUTH, data.azimuthCalibratedDegrees)
            putExtra(Constants.EXTRA_TILT, data.tiltCalibratedDegrees.toInt())
            putExtra(Constants.EXTRA_TILT_DIRECTION_ANGLE, data.tiltDirectionAngle)
        }
        
        // 使用 LocalBroadcastManager 发送广播
        localBroadcastManager.sendBroadcast(intent)
        
        // 广播磁力计状态
        broadcastMagnetometerStatus(data)
        
        Log.d(TAG, "Broadcast sensor data: ${data.direction} Tilt:${data.tiltCalibratedDegrees.toInt()}° TiltDir:${data.tiltDirectionAngle.toInt()}° MagAcc:${data.magnetometerAccuracy}")
    }
    
    /**
     * 广播磁力计状态
     */
    private fun broadcastMagnetometerStatus(data: SensorData) {
        val intent = Intent(Constants.ACTION_MAGNETOMETER_STATUS_UPDATE).apply {
            putExtra(Constants.EXTRA_MAGNETOMETER_ACCURACY, data.magnetometerAccuracy)
            putExtra(Constants.EXTRA_MAGNETIC_FIELD_STRENGTH, data.magneticFieldStrength)
        }
        
        // 使用 LocalBroadcastManager 发送广播
        localBroadcastManager.sendBroadcast(intent)
    }
    
    /**
     * 广播网络状态
     */
    private fun broadcastNetworkStatus() {
        val intent = Intent(Constants.ACTION_NETWORK_STATUS_UPDATE).apply {
            putExtra(Constants.EXTRA_NETWORK_STATUS, currentNetworkStatus.name)
            putExtra(Constants.EXTRA_LAST_UPLOAD_TIME, lastUploadTime)
        }
        
        // 使用 LocalBroadcastManager 发送广播
        localBroadcastManager.sendBroadcast(intent)
        Log.d(TAG, "Broadcast network status: ${currentNetworkStatus.name}")
    }
    
    /**
     * 广播初始状态
     */
    private fun broadcastInitialStatus() {
        // 广播初始网络状态
        broadcastNetworkStatus()
        
        // 如果已有传感器数据，也广播出去
        currentSensorData?.let {
            broadcastSensorData()
        }
    }
    
    /**
     * 更新配置
     */
    private fun updateConfig() {
        Log.d(TAG, "Updating configuration")
        networkRepository.reinitialize()
        
        // 重新调度上传任务
        if (!isPaused) {
            uploadRunnable?.let { mainHandler.removeCallbacks(it) }
            scheduleNextUpload()
        }
    }
    
    /**
     * 停止所有任务
     */
    private fun stopAllTasks() {
        uploadRunnable?.let { mainHandler.removeCallbacks(it) }
        networkMonitorRunnable?.let { mainHandler.removeCallbacks(it) }
        uiUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
    }
    
    /**
     * 停止服务
     */
    private fun stopService() {
        Log.d(TAG, "Stopping service")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}
