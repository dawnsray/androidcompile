package com.example.levelcheck.data.model

/**
 * 应用配置数据模型
 * @property serverHost 服务器地址
 * @property serverPort 服务器端口
 * @property uploadIntervalSeconds 上传频率，秒
 */
data class AppConfig(
    val serverHost: String = "",
    val serverPort: Int = 0,
    val uploadIntervalSeconds: Int = 5
)
