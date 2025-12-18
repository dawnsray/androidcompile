package com.example.levelcheck.ui.calibration

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.levelcheck.databinding.ActivityMagnetometerCalibrationBinding

/**
 * 磁力计校准引导Activity - 8字校准法
 */
class MagnetometerCalibrationActivity : AppCompatActivity(), SensorEventListener {
    
    private lateinit var binding: ActivityMagnetometerCalibrationBinding
    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null
    
    private var isCalibrating = false
    private var calibrationProgress = 0
    private val handler = Handler(Looper.getMainLooper())
    
    // 磁场强度范围记录
    private var minFieldStrength = Float.MAX_VALUE
    private var maxFieldStrength = Float.MIN_VALUE
    private var currentAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMagnetometerCalibrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        if (magnetometer == null) {
            Toast.makeText(this, "设备不支持磁力计", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupViews()
    }
    
    override fun onResume() {
        super.onResume()
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
    
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
    
    private fun setupViews() {
        binding.btnStartCalibration.setOnClickListener {
            if (!isCalibrating) {
                startCalibration()
            }
        }
        
        binding.btnSkip.setOnClickListener {
            finish()
        }
        
        binding.btnDone.setOnClickListener {
            finish()
        }
        
        updateUI()
    }
    
    private fun startCalibration() {
        isCalibrating = true
        calibrationProgress = 0
        minFieldStrength = Float.MAX_VALUE
        maxFieldStrength = Float.MIN_VALUE
        
        binding.btnStartCalibration.isEnabled = false
        binding.btnSkip.isEnabled = false
        binding.tvStatus.text = "请按8字轨迹缓慢移动设备..."
        
        // 30秒后自动结束
        handler.postDelayed({
            finishCalibration()
        }, 30000)
        
        updateUI()
    }
    
    private fun finishCalibration() {
        isCalibrating = false
        
        val calibrationQuality = when {
            currentAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "优秀"
            currentAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "良好"
            currentAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "一般"
            else -> "较差"
        }
        
        binding.tvStatus.text = "校准完成！磁场精度: $calibrationQuality"
        binding.btnDone.isEnabled = true
        binding.progressBar.progress = 100
        
        Toast.makeText(this, "磁力计校准完成", Toast.LENGTH_SHORT).show()
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            
            val fieldStrength = kotlin.math.sqrt(x * x + y * y + z * z)
            
            binding.tvFieldStrength.text = String.format("磁场强度: %.1f μT", fieldStrength)
            
            if (isCalibrating) {
                // 更新最小和最大磁场强度
                minFieldStrength = kotlin.math.min(minFieldStrength, fieldStrength)
                maxFieldStrength = kotlin.math.max(maxFieldStrength, fieldStrength)
                
                // 计算进度（基于磁场强度的变化范围）
                val fieldRange = maxFieldStrength - minFieldStrength
                calibrationProgress = (fieldRange * 2).toInt().coerceIn(0, 100)
                
                binding.progressBar.progress = calibrationProgress
                binding.tvProgress.text = "$calibrationProgress%"
                
                // 如果精度达到高或中等，且进度超过70%，可以提前结束
                if (calibrationProgress >= 70 && 
                    (currentAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH ||
                     currentAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)) {
                    handler.removeCallbacksAndMessages(null)
                    finishCalibration()
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            currentAccuracy = accuracy
            
            val accuracyText = when (accuracy) {
                SensorManager.SENSOR_STATUS_UNRELIABLE -> "不可靠"
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "低"
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "中"
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "高"
                else -> "未知"
            }
            
            binding.tvAccuracy.text = "精度: $accuracyText"
            
            val colorRes = when (accuracy) {
                SensorManager.SENSOR_STATUS_UNRELIABLE -> android.R.color.holo_red_dark
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> android.R.color.holo_orange_dark
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> android.R.color.holo_green_light
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> android.R.color.holo_green_dark
                else -> android.R.color.darker_gray
            }
            
            binding.tvAccuracy.setTextColor(getColor(colorRes))
        }
    }
    
    private fun updateUI() {
        binding.progressBar.progress = calibrationProgress
        binding.tvProgress.text = "$calibrationProgress%"
        binding.btnDone.isEnabled = !isCalibrating && calibrationProgress > 0
    }
}
