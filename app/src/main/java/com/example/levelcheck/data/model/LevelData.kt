package com.example.levelcheck.data.model

import com.google.gson.annotations.SerializedName

/**
 * 水平仪数据模型 - 用于上传到服务器
 * @property azimuth 方位角，0-360度，保留1位小数
 * @property tilt 倾斜角，0-90度，整数
 * @property timestamp 时间戳，毫秒级Unix时间戳
 */
data class LevelData(
    @SerializedName("azimuth")
    val azimuth: Double,
    
    @SerializedName("tilt")
    val tilt: Int,
    
    @SerializedName("timestamp")
    val timestamp: Long
)
