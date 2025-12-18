package com.example.levelcheck.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.levelcheck.data.model.AppConfig
import com.example.levelcheck.data.model.CalibrationData
import com.example.levelcheck.util.Constants

/**
 * 配置数据仓库 - 封装SharedPreferences操作
 */
class PreferenceRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
    
    /**
     * 获取应用配置
     */
    fun getAppConfig(): AppConfig {
        return AppConfig(
            serverHost = sharedPreferences.getString(
                Constants.KEY_SERVER_HOST,
                Constants.DEFAULT_SERVER_HOST
            ) ?: Constants.DEFAULT_SERVER_HOST,
            serverPort = sharedPreferences.getInt(
                Constants.KEY_SERVER_PORT,
                Constants.DEFAULT_SERVER_PORT
            ),
            uploadIntervalSeconds = sharedPreferences.getInt(
                Constants.KEY_UPLOAD_INTERVAL,
                Constants.DEFAULT_UPLOAD_INTERVAL
            )
        )
    }
    
    /**
     * 保存应用配置
     */
    fun saveAppConfig(config: AppConfig) {
        sharedPreferences.edit().apply {
            putString(Constants.KEY_SERVER_HOST, config.serverHost)
            putInt(Constants.KEY_SERVER_PORT, config.serverPort)
            putInt(Constants.KEY_UPLOAD_INTERVAL, config.uploadIntervalSeconds)
            apply()
        }
    }
    
    /**
     * 获取服务器地址
     */
    fun getServerHost(): String {
        return sharedPreferences.getString(
            Constants.KEY_SERVER_HOST,
            Constants.DEFAULT_SERVER_HOST
        ) ?: Constants.DEFAULT_SERVER_HOST
    }
    
    /**
     * 获取服务器端口
     */
    fun getServerPort(): Int {
        return sharedPreferences.getInt(
            Constants.KEY_SERVER_PORT,
            Constants.DEFAULT_SERVER_PORT
        )
    }
    
    /**
     * 获取上传频率
     */
    fun getUploadInterval(): Int {
        return sharedPreferences.getInt(
            Constants.KEY_UPLOAD_INTERVAL,
            Constants.DEFAULT_UPLOAD_INTERVAL
        )
    }
    
    /**
     * 保存上传频率
     */
    fun saveUploadInterval(intervalSeconds: Int) {
        sharedPreferences.edit().putInt(Constants.KEY_UPLOAD_INTERVAL, intervalSeconds).apply()
    }
    
    /**
     * 获取校准数据
     */
    fun getCalibrationData(): CalibrationData {
        val tiltOffset = sharedPreferences.getFloat(
            Constants.KEY_CALIBRATION_OFFSET,
            Constants.DEFAULT_CALIBRATION_OFFSET
        )
        val azimuthOffset = sharedPreferences.getFloat(
            Constants.KEY_AZIMUTH_CALIBRATION_OFFSET,
            0f
        )
        val time = sharedPreferences.getLong(Constants.KEY_CALIBRATION_TIME, 0L)
        
        return CalibrationData(
            tiltOffset = tiltOffset,
            azimuthOffset = azimuthOffset,
            calibrationTime = time,
            isCalibrated = time > 0
        )
    }
    
    /**
     * 保存倾斜角校准数据
     */
    fun saveCalibrationData(tiltOffset: Float) {
        sharedPreferences.edit().apply {
            putFloat(Constants.KEY_CALIBRATION_OFFSET, tiltOffset)
            putLong(Constants.KEY_CALIBRATION_TIME, System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * 保存方位角校准数据
     */
    fun saveAzimuthCalibrationData(azimuthOffset: Float) {
        sharedPreferences.edit().apply {
            putFloat(Constants.KEY_AZIMUTH_CALIBRATION_OFFSET, azimuthOffset)
            putLong(Constants.KEY_CALIBRATION_TIME, System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * 清除校准数据
     */
    fun clearCalibrationData() {
        sharedPreferences.edit().apply {
            putFloat(Constants.KEY_CALIBRATION_OFFSET, 0f)
            putFloat(Constants.KEY_AZIMUTH_CALIBRATION_OFFSET, 0f)
            putLong(Constants.KEY_CALIBRATION_TIME, 0L)
            apply()
        }
    }
}
