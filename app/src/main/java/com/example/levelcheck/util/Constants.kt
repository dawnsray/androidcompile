package com.example.levelcheck.util

/**
 * 应用常量定义
 */
object Constants {
    // SharedPreferences 文件名
    const val PREF_NAME = "level_check_preferences"
    
    // 配置键名
    const val KEY_SERVER_HOST = "server_host"
    const val KEY_SERVER_PORT = "server_port"
    const val KEY_UPLOAD_INTERVAL = "upload_interval_seconds"
    const val KEY_CALIBRATION_OFFSET = "calibration_tilt_offset"
    const val KEY_AZIMUTH_CALIBRATION_OFFSET = "calibration_azimuth_offset"
    const val KEY_CALIBRATION_TIME = "calibration_timestamp"
    
    // 默认配置值
    const val DEFAULT_SERVER_HOST = ""
    const val DEFAULT_SERVER_PORT = 0
    const val DEFAULT_UPLOAD_INTERVAL = 5
    const val DEFAULT_CALIBRATION_OFFSET = 0f
    
    // 首次启动标记
    const val KEY_FIRST_LAUNCH_COMPLETED = "first_launch_completed"
    const val INTENT_EXTRA_FORCE_CONFIG_MODE = "force_config_mode"
    
    // 上传频率范围
    const val MIN_UPLOAD_INTERVAL = 1
    const val MAX_UPLOAD_INTERVAL = 30
    
    // 端口范围
    const val MIN_PORT = 1
    const val MAX_PORT = 65535
    
    // 网络重试配置
    const val MAX_RETRY_COUNT = 5
    const val RETRY_BASE_DELAY_SECONDS = 2L
    const val NETWORK_MONITOR_INTERVAL_SECONDS = 30L
    
    // 网络超时配置
    const val CONNECT_TIMEOUT_SECONDS = 10L
    const val READ_TIMEOUT_SECONDS = 10L
    const val WRITE_TIMEOUT_SECONDS = 10L
    
    // API端点
    const val API_ENDPOINT = "/api/level/upload"
    
    // 传感器采集配置
    const val SENSOR_DELAY = android.hardware.SensorManager.SENSOR_DELAY_UI
    const val CALIBRATION_SAMPLE_COUNT = 10
    const val CALIBRATION_SAMPLE_INTERVAL_MS = 100L
    const val MAX_CALIBRATION_OFFSET = 45f
    
    // UI更新频率
    const val UI_UPDATE_INTERVAL_MS = 500L
    const val NOTIFICATION_UPDATE_INTERVAL_MS = 1000L
    const val NOTIFICATION_TILT_CHANGE_THRESHOLD = 1
    
    // 前台服务通知
    const val NOTIFICATION_CHANNEL_ID = "sensor_service_channel"
    const val NOTIFICATION_CHANNEL_NAME = "水平仪数据采集"
    const val NOTIFICATION_ID = 1001
    
    // 广播Action
    const val ACTION_SENSOR_DATA_UPDATE = "com.example.levelcheck.SENSOR_DATA_UPDATE"
    const val ACTION_NETWORK_STATUS_UPDATE = "com.example.levelcheck.NETWORK_STATUS_UPDATE"
    const val ACTION_MAGNETOMETER_STATUS_UPDATE = "com.example.levelcheck.MAGNETOMETER_STATUS_UPDATE"
    const val ACTION_STOP_SERVICE = "com.example.levelcheck.STOP_SERVICE"
    const val ACTION_UPDATE_CONFIG = "com.example.levelcheck.UPDATE_CONFIG"
    
    // Intent Extra键名
    const val EXTRA_AZIMUTH = "extra_azimuth"
    const val EXTRA_TILT = "extra_tilt"
    const val EXTRA_DIRECTION = "extra_direction"
    const val EXTRA_TILT_DIRECTION_ANGLE = "extra_tilt_direction_angle"
    const val EXTRA_NETWORK_STATUS = "extra_network_status"
    const val EXTRA_LAST_UPLOAD_TIME = "extra_last_upload_time"
    const val EXTRA_MAGNETOMETER_ACCURACY = "extra_magnetometer_accuracy"
    const val EXTRA_MAGNETIC_FIELD_STRENGTH = "extra_magnetic_field_strength"
}
