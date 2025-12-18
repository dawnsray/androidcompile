package com.example.levelcheck.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.levelcheck.data.model.AppConfig
import com.example.levelcheck.data.repository.PreferenceRepository
import com.example.levelcheck.util.Constants

/**
 * 设置界面ViewModel
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val preferenceRepository = PreferenceRepository(application)
    
    // 配置数据
    private val _serverHost = MutableLiveData<String>()
    val serverHost: LiveData<String> = _serverHost
    
    private val _serverPort = MutableLiveData<Int>()
    val serverPort: LiveData<Int> = _serverPort
    
    private val _uploadInterval = MutableLiveData<Int>()
    val uploadInterval: LiveData<Int> = _uploadInterval
    
    // 校准信息
    private val _calibrationTime = MutableLiveData<Long>()
    val calibrationTime: LiveData<Long> = _calibrationTime
    
    // 验证结果
    private val _validationError = MutableLiveData<String?>()
    val validationError: LiveData<String?> = _validationError
    
    init {
        loadConfig()
    }
    
    /**
     * 加载配置
     */
    fun loadConfig() {
        val config = preferenceRepository.getAppConfig()
        _serverHost.value = config.serverHost
        _serverPort.value = config.serverPort
        _uploadInterval.value = config.uploadIntervalSeconds
        
        val calibData = preferenceRepository.getCalibrationData()
        _calibrationTime.value = calibData.calibrationTime
    }
    
    /**
     * 验证并保存配置
     * @return 是否保存成功
     */
    fun validateAndSaveConfig(host: String, port: String, interval: Int): Boolean {
        // 验证服务器地址
        if (host.isBlank()) {
            _validationError.value = "服务器地址不能为空"
            return false
        }
        
        // 验证端口
        val portNum = port.toIntOrNull()
        if (portNum == null || portNum !in Constants.MIN_PORT..Constants.MAX_PORT) {
            _validationError.value = "端口必须在 ${Constants.MIN_PORT} - ${Constants.MAX_PORT} 范围内"
            return false
        }
        
        // 验证上传频率
        if (interval !in Constants.MIN_UPLOAD_INTERVAL..Constants.MAX_UPLOAD_INTERVAL) {
            _validationError.value = "上传频率必须在 ${Constants.MIN_UPLOAD_INTERVAL} - ${Constants.MAX_UPLOAD_INTERVAL} 秒范围内"
            return false
        }
        
        // 保存配置
        val config = AppConfig(
            serverHost = host.trim(),
            serverPort = portNum,
            uploadIntervalSeconds = interval
        )
        preferenceRepository.saveAppConfig(config)
        
        _validationError.value = null
        return true
    }
    
    /**
     * 清除验证错误
     */
    fun clearValidationError() {
        _validationError.value = null
    }
}
