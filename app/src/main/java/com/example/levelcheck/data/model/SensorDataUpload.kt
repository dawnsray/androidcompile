package com.example.levelcheck.data.model

import com.google.gson.annotations.SerializedName

/**
 * 传感器数据上传模型 - 匹配服务器 /api/sensors 接口格式
 * @property sensors 传感器角度数组（单位：度，范围-180到180）
 */
data class SensorDataUpload(
    @SerializedName("sensors")
    val sensors: List<Double>
)
