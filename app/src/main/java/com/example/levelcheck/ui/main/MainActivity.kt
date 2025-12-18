package com.example.levelcheck.ui.main

import android.Manifest
import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.levelcheck.R
import com.example.levelcheck.data.model.NetworkStatus
import com.example.levelcheck.databinding.ActivityMainBinding
import com.example.levelcheck.service.SensorService
import com.example.levelcheck.ui.calibration.CalibrationActivity
import com.example.levelcheck.ui.calibration.MagnetometerCalibrationActivity
import com.example.levelcheck.ui.settings.SettingsActivity
import com.example.levelcheck.util.Constants
import com.example.levelcheck.util.toTimeString

/**
 * 主界面Activity
 */
class MainActivity : AppCompatActivity() {
    
    private val TAG = "MainActivity"
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var localBroadcastManager: LocalBroadcastManager
    
    // 权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkSensorAvailability()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    // Android 14+ 前台服务权限请求启动器
    private val requestForegroundServiceLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 无论是否授权，都尝试启动服务
        startSensorService()
    }
    
    // 广播接收器
    private val sensorDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            
            when (intent.action) {
                Constants.ACTION_SENSOR_DATA_UPDATE -> {
                    val direction = intent.getStringExtra(Constants.EXTRA_DIRECTION) ?: "未知"
                    val azimuth = intent.getFloatExtra(Constants.EXTRA_AZIMUTH, 0f)
                    val tilt = intent.getIntExtra(Constants.EXTRA_TILT, 0)
                    val tiltDirectionAngle = intent.getFloatExtra(Constants.EXTRA_TILT_DIRECTION_ANGLE, 0f)
                    
                    Log.d(TAG, "Received sensor data: $direction Tilt:$tilt° TiltDirection:$tiltDirectionAngle°")
                    viewModel.updateSensorData(direction, azimuth, tilt, tiltDirectionAngle)
                }
                Constants.ACTION_NETWORK_STATUS_UPDATE -> {
                    val statusName = intent.getStringExtra(Constants.EXTRA_NETWORK_STATUS) ?: "UNKNOWN"
                    val status = NetworkStatus.valueOf(statusName)
                    val lastUploadTime = intent.getLongExtra(Constants.EXTRA_LAST_UPLOAD_TIME, 0L)
                    
                    Log.d(TAG, "Received network status: $statusName")
                    viewModel.updateNetworkStatus(status, lastUploadTime)
                }
                Constants.ACTION_MAGNETOMETER_STATUS_UPDATE -> {
                    val accuracy = intent.getIntExtra(Constants.EXTRA_MAGNETOMETER_ACCURACY, 0)
                    val fieldStrength = intent.getFloatExtra(Constants.EXTRA_MAGNETIC_FIELD_STRENGTH, 0f)
                    
                    Log.d(TAG, "Received magnetometer status: accuracy=$accuracy, strength=$fieldStrength")
                    viewModel.updateMagnetometerStatus(accuracy, fieldStrength)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        
        setupViews()
        observeViewModel()
        checkPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        
        // 注册广播接收器
        val filter = IntentFilter().apply {
            addAction(Constants.ACTION_SENSOR_DATA_UPDATE)
            addAction(Constants.ACTION_NETWORK_STATUS_UPDATE)
            addAction(Constants.ACTION_MAGNETOMETER_STATUS_UPDATE)
        }
        
        // 使用 LocalBroadcastManager 注册
        localBroadcastManager.registerReceiver(sensorDataReceiver, filter)
        Log.d(TAG, "Broadcast receiver registered")
        
        // 更新服务状态
        updateServiceStatus()
        
        // 刷新校准信息
        viewModel.loadCalibrationInfo()
    }
    
    override fun onPause() {
        super.onPause()
        // 注销广播接收器
        try {
            localBroadcastManager.unregisterReceiver(sensorDataReceiver)
            Log.d(TAG, "Broadcast receiver unregistered")
        } catch (e: Exception) {
            // Receiver may not be registered
        }
    }
    
    /**
     * 设置视图
     */
    private fun setupViews() {
        // 服务控制按钮
        binding.btnToggleService.setOnClickListener {
            toggleService()
        }
        
        // 校准按钮
        binding.btnCalibration.setOnClickListener {
            startActivity(Intent(this, CalibrationActivity::class.java))
        }
        
        // 设置按钮
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
    
    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModel() {
        // 方位方向
        viewModel.currentDirection.observe(this) { direction ->
            binding.tvDirection.text = direction
        }
        
        // 倾斜角
        viewModel.currentTilt.observe(this) { tilt ->
            binding.tvTilt.text = "$tilt°"
            // 更新可视化指示器
            binding.levelIndicator.updateTilt(tilt)
        }
        
        // 倾斜方向角度（相对用户视角）
        viewModel.currentTiltDirectionAngle.observe(this) { tiltDirectionAngle ->
            // 更新可视化指示器
            binding.levelIndicator.updateTiltDirection(tiltDirectionAngle)
        }
        
        // 服务状态
        viewModel.serviceRunning.observe(this) { running ->
            updateServiceButton(running)
            binding.tvServiceStatus.text = if (running) "运行中" else "已停止"
            binding.tvServiceStatus.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (running) android.R.color.holo_green_dark else android.R.color.darker_gray
                )
            )
        }
        
        // 网络状态
        viewModel.networkStatus.observe(this) { status ->
            val (statusText, colorRes) = when (status) {
                NetworkStatus.CONNECTED -> "正常" to android.R.color.holo_green_dark
                NetworkStatus.CONNECTING -> "连接中" to android.R.color.holo_orange_dark
                NetworkStatus.DISCONNECTED -> "异常" to android.R.color.holo_red_dark
                NetworkStatus.UNKNOWN -> "未知" to android.R.color.darker_gray
            }
            
            binding.tvNetworkStatus.text = statusText
            binding.tvNetworkStatus.setTextColor(ContextCompat.getColor(this, colorRes))
        }
        
        // 上次上传时间
        viewModel.lastUploadTime.observe(this) { time ->
            binding.tvLastUpload.text = if (time > 0) {
                time.toTimeString("HH:mm:ss")
            } else {
                "--"
            }
        }
        
        // 磁力计精度状态
        viewModel.magnetometerAccuracy.observe(this) { accuracy ->
            updateMagnetometerStatus(accuracy, viewModel.magneticFieldStrength.value ?: 0f)
        }
        
        viewModel.magneticFieldStrength.observe(this) { fieldStrength ->
            updateMagnetometerStatus(viewModel.magnetometerAccuracy.value ?: 0, fieldStrength)
        }
    }
    
    /**
     * 检查权限
     */
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要请求通知权限
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        
        checkSensorAvailability()
    }
    
    /**
     * 检查传感器可用性
     */
    private fun checkSensorAvailability() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD)
        
        if (accelerometer == null || magnetometer == null) {
            AlertDialog.Builder(this)
                .setTitle("错误")
                .setMessage(getString(R.string.sensor_not_available))
                .setPositiveButton("确定") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }
    
    /**
     * 显示权限拒绝对话框
     */
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required_title))
            .setMessage(getString(R.string.permission_required_message))
            .setPositiveButton(getString(R.string.permission_go_to_settings)) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(getString(R.string.permission_exit)) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 打开应用设置页面
     */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
    
    /**
     * 切换服务状态
     */
    private fun toggleService() {
        if (isServiceRunning()) {
            stopSensorService()
        } else {
            // Android 14+ 需要请求前台服务权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestForegroundServiceLauncher.launch(
                        Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC
                    )
                    return
                }
            }
            startSensorService()
        }
    }
    
    /**
     * 启动传感器服务
     */
    private fun startSensorService() {
        val intent = Intent(this, SensorService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        viewModel.updateServiceStatus(true)
        Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 停止传感器服务
     */
    private fun stopSensorService() {
        val intent = Intent(this, SensorService::class.java).apply {
            action = Constants.ACTION_STOP_SERVICE
        }
        startService(intent)
        
        viewModel.updateServiceStatus(false)
        Toast.makeText(this, "服务已停止", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 检查服务是否正在运行
     */
    private fun isServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        @Suppress("DEPRECATION")
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (SensorService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
    
    /**
     * 更新服务状态
     */
    private fun updateServiceStatus() {
        val running = isServiceRunning()
        viewModel.updateServiceStatus(running)
    }
    
    /**
     * 更新服务按钮状态
     */
    private fun updateServiceButton(running: Boolean) {
        binding.btnToggleService.text = if (running) "停止服务" else "启动服务"
        binding.btnToggleService.backgroundTintList = ContextCompat.getColorStateList(
            this,
            if (running) android.R.color.holo_red_dark else android.R.color.holo_green_dark
        )
    }
    
    /**
     * 更新磁力计状态显示
     */
    private fun updateMagnetometerStatus(accuracy: Int, fieldStrength: Float) {
        val (statusText, colorRes, showWarning) = when (accuracy) {
            SensorManager.SENSOR_STATUS_UNRELIABLE -> 
                Triple("磁场异常", android.R.color.holo_red_dark, true)
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> 
                Triple("磁场较弱", android.R.color.holo_orange_dark, true)
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> 
                Triple("磁场正常", android.R.color.holo_green_light, false)
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> 
                Triple("磁场良好", android.R.color.holo_green_dark, false)
            else -> 
                Triple("未知", android.R.color.darker_gray, false)
        }
        
        binding.tvMagnetometerStatus.text = statusText
        binding.tvMagnetometerStatus.setTextColor(ContextCompat.getColor(this, colorRes))
        
        // 显示磁场强度
        binding.tvMagneticFieldStrength.text = String.format("%.1f μT", fieldStrength)
        
        // 如果磁场异常，显示警告提示
        if (showWarning && viewModel.serviceRunning.value == true) {
            binding.tvMagnetometerWarning.visibility = android.view.View.VISIBLE
            binding.tvMagnetometerWarning.text = "⚠️ 磁场干扰严重，请远离电子设备或进行8字校准"
            
            // 设置点击事件，打开校准页面
            binding.tvMagnetometerWarning.setOnClickListener {
                startActivity(Intent(this, MagnetometerCalibrationActivity::class.java))
            }
        } else {
            binding.tvMagnetometerWarning.visibility = android.view.View.GONE
        }
    }
}
