package com.example.levelcheck.data.model

/**
 * 传感器数据模型 - 内部使用
 * @property azimuthRaw 原始方位角，弧度制
 * @property tiltRaw 原始倾斜角，弧度制
 * @property azimuthDegrees 方位角，度数
 * @property azimuthCalibratedDegrees 校准后的方位角，度数
 * @property tiltDegrees 倾斜角，度数（应用校准前）
 * @property tiltCalibratedDegrees 校准后的倾斜角，度数
 * @property direction 方位名称
 * @property rollDegrees Roll角度（设备左右倾斜），度数
 * @property pitchDegrees Pitch角度（设备前后倾斜），度数
 * @property tiltDirectionAngle 倾斜方向角度（相对用户视角，0度=向前，90度=向右，180度=向后，270度=向左）
 * @property magneticFieldStrength 磁场强度（微特斯拉）
 * @property magnetometerAccuracy 磁力计精度（0-3）
 * @property timestamp 采集时间戳
 */
data class SensorData(
    val azimuthRaw: Float = 0f,
    val tiltRaw: Float = 0f,
    val azimuthDegrees: Float = 0f,
    val azimuthCalibratedDegrees: Float = 0f,
    val tiltDegrees: Float = 0f,
    val tiltCalibratedDegrees: Float = 0f,
    val direction: String = "",
    val rollDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
    val tiltDirectionAngle: Float = 0f,
    val magneticFieldStrength: Float = 0f,
    val magnetometerAccuracy: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
