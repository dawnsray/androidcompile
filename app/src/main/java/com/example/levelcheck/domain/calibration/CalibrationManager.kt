package com.example.levelcheck.domain.calibration

import com.example.levelcheck.data.model.CalibrationData
import com.example.levelcheck.data.repository.PreferenceRepository
import com.example.levelcheck.util.Constants
import kotlin.math.abs

/**
 * 校准管理器 - 管理校准数据和应用校准偏移量
 */
class CalibrationManager(
    private val preferenceRepository: PreferenceRepository
) {
    
    /**
     * 获取当前校准数据
     */
    fun getCalibrationData(): CalibrationData {
        return preferenceRepository.getCalibrationData()
    }
    
    /**
     * 应用倾斜角校准偏移量
     * @param rawTilt 原始倾斜角
     * @return 校准后的倾斜角
     */
    fun applyTiltCalibration(rawTilt: Float): Float {
        val calibData = getCalibrationData()
        
        // 如果校准数据无效（超出合理范围），则不应用校准
        if (abs(calibData.tiltOffset) > Constants.MAX_CALIBRATION_OFFSET) {
            return rawTilt
        }
        
        // 应用校准偏移量
        val calibrated = rawTilt - calibData.tiltOffset
        
        // 确保结果在0-90度范围内
        return abs(calibrated).coerceIn(0f, 90f)
    }
    
    /**
     * 应用方位角校准偏移量
     * @param rawAzimuth 原始方位角（0-360）
     * @return 校准后的方位角（0-360）
     */
    fun applyAzimuthCalibration(rawAzimuth: Float): Float {
        val calibData = getCalibrationData()
        
        // 应用校准偏移量并规范化到0-360度
        var calibrated = rawAzimuth - calibData.azimuthOffset
        
        // 处理负值和超过360度的情况
        while (calibrated < 0f) {
            calibrated += 360f
        }
        while (calibrated >= 360f) {
            calibrated -= 360f
        }
        
        return calibrated
    }
    
    /**
     * 应用校准（为了向后兼容，保留applyCalibration方法）
     * @param rawTilt 原始倾斜角
     * @return 校准后的倾斜角
     */
    fun applyCalibration(rawTilt: Float): Float {
        return applyTiltCalibration(rawTilt)
    }
    
    /**
     * 执行倾斜角校准 - 保存当前倾斜角作为偏移量
     * @param samples 采集的样本数据列表
     * @return 是否校准成功
     */
    fun performTiltCalibration(samples: List<Float>): Boolean {
        if (samples.isEmpty()) {
            return false
        }
        
        // 计算平均倾斜角
        val averageTilt = samples.average().toFloat()
        
        // 计算标准差，检查设备是否静止
        val variance = samples.map { (it - averageTilt) * (it - averageTilt) }.average()
        val stdDev = kotlin.math.sqrt(variance).toFloat()
        
        // 如果标准差过大，说明设备在移动，校准失败
        if (stdDev > 2f) {
            return false
        }
        
        // 保存校准偏移量
        preferenceRepository.saveCalibrationData(averageTilt)
        
        return true
    }
    
    /**
     * 执行方位角校准 - 保存当前方位角作为偏移量
     * @param samples 采集的样本数据列表（0-360度）
     * @return 是否校准成功
     */
    fun performAzimuthCalibration(samples: List<Float>): Boolean {
        if (samples.isEmpty()) {
            return false
        }
        
        // 计算平均方位角（处理环形角度）
        val averageAzimuth = calculateCircularMean(samples)
        
        // 计算环形标准差，检查设备是否静止
        val circularStdDev = calculateCircularStdDev(samples, averageAzimuth)
        
        // 如果标准差过大，说明设备在移动或磁场不稳定
        if (circularStdDev > 5f) {
            return false
        }
        
        // 保存校准偏移量
        preferenceRepository.saveAzimuthCalibrationData(averageAzimuth)
        
        return true
    }
    
    /**
     * 执行校准（为了向后兼容，保留performCalibration方法）
     * @param samples 采集的样本数据列表
     * @return 是否校准成功
     */
    fun performCalibration(samples: List<Float>): Boolean {
        return performTiltCalibration(samples)
    }
    
    /**
     * 清除校准数据
     */
    fun clearCalibration() {
        preferenceRepository.clearCalibrationData()
    }
    
    /**
     * 计算环形平均值（用于角度计算）
     */
    private fun calculateCircularMean(angles: List<Float>): Float {
        var sinSum = 0.0
        var cosSum = 0.0
        
        angles.forEach { angle ->
            val rad = Math.toRadians(angle.toDouble())
            sinSum += kotlin.math.sin(rad)
            cosSum += kotlin.math.cos(rad)
        }
        
        val meanRad = kotlin.math.atan2(sinSum / angles.size, cosSum / angles.size)
        var meanDeg = Math.toDegrees(meanRad).toFloat()
        
        // 规范化到0-360度
        if (meanDeg < 0) {
            meanDeg += 360f
        }
        
        return meanDeg
    }
    
    /**
     * 计算环形标准差（用于角度计算）
     */
    private fun calculateCircularStdDev(angles: List<Float>, mean: Float): Float {
        var sumSquaredDiff = 0.0
        
        angles.forEach { angle ->
            // 计算角度差异（处理跨越0度的情况）
            var diff = angle - mean
            if (diff > 180f) diff -= 360f
            if (diff < -180f) diff += 360f
            sumSquaredDiff += diff * diff
        }
        
        return kotlin.math.sqrt(sumSquaredDiff / angles.size).toFloat()
    }
}
