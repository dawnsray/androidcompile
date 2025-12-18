package com.example.levelcheck.ui.calibration

import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.levelcheck.data.repository.PreferenceRepository
import com.example.levelcheck.databinding.ActivityCalibrationBinding
import com.example.levelcheck.domain.calibration.CalibrationManager
import com.example.levelcheck.domain.sensor.SensorProcessor
import com.example.levelcheck.util.Constants

/**
 * 校准界面Activity
 */
class CalibrationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCalibrationBinding
    private lateinit var calibrationManager: CalibrationManager
    private lateinit var sensorProcessor: SensorProcessor
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isCalibrating = false
    private val calibrationSamples = mutableListOf<Float>()
    
    private val updateTiltRunnable = object : Runnable {
        override fun run() {
            if (!isCalibrating) {
                val currentTilt = sensorProcessor.getCurrentTilt()
                binding.tvCurrentTilt.text = String.format("%.1f°", currentTilt)
                mainHandler.postDelayed(this, 100)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityCalibrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化组件
        val preferenceRepository = PreferenceRepository(this)
        calibrationManager = CalibrationManager(preferenceRepository)
        
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorProcessor = SensorProcessor(sensorManager, calibrationManager) { }
        
        setupViews()
    }
    
    override fun onResume() {
        super.onResume()
        
        // 注册传感器
        sensorProcessor.register()
        
        // 开始更新实时倾斜角
        mainHandler.post(updateTiltRunnable)
    }
    
    override fun onPause() {
        super.onPause()
        
        // 注销传感器
        sensorProcessor.unregister()
        
        // 停止更新
        mainHandler.removeCallbacks(updateTiltRunnable)
    }
    
    /**
     * 设置视图
     */
    private fun setupViews() {
        // 开始校准按钮
        binding.btnStartCalibration.setOnClickListener {
            startCalibration()
        }
        
        // 取消按钮
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
    
    /**
     * 开始校准流程
     */
    private fun startCalibration() {
        if (isCalibrating) {
            return
        }
        
        isCalibrating = true
        calibrationSamples.clear()
        
        // 禁用按钮
        binding.btnStartCalibration.isEnabled = false
        binding.btnStartCalibration.text = "校准中..."
        binding.btnCancel.isEnabled = false
        
        // 停止实时倾斜角更新
        mainHandler.removeCallbacks(updateTiltRunnable)
        
        // 开始采集样本
        collectSamples()
    }
    
    /**
     * 采集样本数据
     */
    private fun collectSamples() {
        var sampleCount = 0
        
        val sampleRunnable = object : Runnable {
            override fun run() {
                if (sampleCount < Constants.CALIBRATION_SAMPLE_COUNT) {
                    // 采集当前倾斜角
                    val tilt = sensorProcessor.getCurrentTilt()
                    calibrationSamples.add(tilt)
                    sampleCount++
                    
                    // 更新状态提示
                    binding.tvCalibrationStatus.text = 
                        "正在采集数据 $sampleCount/${Constants.CALIBRATION_SAMPLE_COUNT}"
                    
                    // 延迟后采集下一个样本
                    mainHandler.postDelayed(this, Constants.CALIBRATION_SAMPLE_INTERVAL_MS)
                } else {
                    // 采集完成，执行校准
                    performCalibration()
                }
            }
        }
        
        mainHandler.post(sampleRunnable)
    }
    
    /**
     * 执行校准
     */
    private fun performCalibration() {
        val success = calibrationManager.performCalibration(calibrationSamples)
        
        if (success) {
            // 校准成功
            binding.tvCalibrationStatus.text = "校准成功！"
            Toast.makeText(this, "校准成功", Toast.LENGTH_SHORT).show()
            
            // 延迟1秒后返回主界面
            mainHandler.postDelayed({
                finish()
            }, 1000)
        } else {
            // 校准失败
            binding.tvCalibrationStatus.text = "校准失败，请保持设备静止"
            Toast.makeText(this, "校准失败，请保持设备静止", Toast.LENGTH_LONG).show()
            
            // 恢复按钮状态
            isCalibrating = false
            binding.btnStartCalibration.isEnabled = true
            binding.btnStartCalibration.text = "开始校准"
            binding.btnCancel.isEnabled = true
            
            // 恢复实时倾斜角更新
            mainHandler.post(updateTiltRunnable)
        }
    }
}
