package com.example.levelcheck.data.model

/**
 * 校准数据模型
 * @property tiltOffset 倾斜角偏移量，度数
 * @property azimuthOffset 方位角偏移量，度数
 * @property calibrationTime 校准时间戳
 * @property isCalibrated 是否已校准
 */
data class CalibrationData(
    val tiltOffset: Float = 0f,
    val azimuthOffset: Float = 0f,
    val calibrationTime: Long = 0L,
    val isCalibrated: Boolean = false
)
