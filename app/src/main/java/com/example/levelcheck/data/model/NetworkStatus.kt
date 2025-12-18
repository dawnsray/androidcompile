package com.example.levelcheck.data.model

/**
 * 网络状态枚举
 */
enum class NetworkStatus {
    /** 网络正常，上传成功 */
    CONNECTED,
    
    /** 连接中，正在重试 */
    CONNECTING,
    
    /** 网络异常，已暂停采集 */
    DISCONNECTED,
    
    /** 未知状态 */
    UNKNOWN
}
