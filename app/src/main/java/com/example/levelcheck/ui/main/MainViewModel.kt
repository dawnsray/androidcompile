package com.example.levelcheck.ui.main

import android.app.Application
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.levelcheck.data.model.NetworkStatus
import com.example.levelcheck.data.repository.PreferenceRepository
import com.example.levelcheck.domain.calibration.CalibrationManager

/**
 * 主界面ViewModel
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val preferenceRepository = PreferenceRepository(application)
    private val calibrationManager = CalibrationManager(preferenceRepository)
    
    // 传感器数据
    private val _currentDirection = MutableLiveData<String>()
    val currentDirection: LiveData<String> = _currentDirection
    
    private val _currentAzimuth = MutableLiveData<Float>()
    val currentAzimuth: LiveData<Float> = _currentAzimuth
    
    private val _currentTilt = MutableLiveData<Int>()
    val currentTilt: LiveData<Int> = _currentTilt
    
    private val _currentTiltDirectionAngle = MutableLiveData<Float>()
    val currentTiltDirectionAngle: LiveData<Float> = _currentTiltDirectionAngle
    
    // 服务状态
    private val _serviceRunning = MutableLiveData<Boolean>()
    val serviceRunning: LiveData<Boolean> = _serviceRunning
    
    // 网络状态
    private val _networkStatus = MutableLiveData<NetworkStatus>()
    val networkStatus: LiveData<NetworkStatus> = _networkStatus
    
    private val _lastUploadTime = MutableLiveData<Long>()
    val lastUploadTime: LiveData<Long> = _lastUploadTime
    
    // 校准信息
    private val _calibrationTime = MutableLiveData<Long>()
    val calibrationTime: LiveData<Long> = _calibrationTime
    
    // 磁力计状态
    private val _magnetometerAccuracy = MutableLiveData<Int>()
    val magnetometerAccuracy: LiveData<Int> = _magnetometerAccuracy
    
    private val _magneticFieldStrength = MutableLiveData<Float>()
    val magneticFieldStrength: LiveData<Float> = _magneticFieldStrength
    
    init {
        _serviceRunning.value = false
        _networkStatus.value = NetworkStatus.UNKNOWN
        _currentDirection.value = "未知"
        _currentAzimuth.value = 0f
        _currentTilt.value = 0
        _currentTiltDirectionAngle.value = 0f
        _lastUploadTime.value = 0L
        _magnetometerAccuracy.value = SensorManager.SENSOR_STATUS_UNRELIABLE
        _magneticFieldStrength.value = 0f
        
        loadCalibrationInfo()
    }
    
    /**
     * 更新传感器数据
     */
    fun updateSensorData(direction: String, azimuth: Float, tilt: Int, tiltDirectionAngle: Float = 0f) {
        _currentDirection.value = direction
        _currentAzimuth.value = azimuth
        _currentTilt.value = tilt
        _currentTiltDirectionAngle.value = tiltDirectionAngle
    }
    
    /**
     * 更新服务状态
     */
    fun updateServiceStatus(running: Boolean) {
        _serviceRunning.value = running
    }
    
    /**
     * 更新网络状态
     */
    fun updateNetworkStatus(status: NetworkStatus, lastUploadTime: Long) {
        _networkStatus.value = status
        _lastUploadTime.value = lastUploadTime
    }
    
    /**
     * 更新磁力计状态
     */
    fun updateMagnetometerStatus(accuracy: Int, fieldStrength: Float) {
        _magnetometerAccuracy.value = accuracy
        _magneticFieldStrength.value = fieldStrength
    }
    
    /**
     * 加载校准信息
     */
    fun loadCalibrationInfo() {
        val calibData = calibrationManager.getCalibrationData()
        _calibrationTime.value = calibData.calibrationTime
    }
}
