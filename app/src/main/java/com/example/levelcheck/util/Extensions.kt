package com.example.levelcheck.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 扩展函数和工具方法
 */

/**
 * Float扩展：保留指定小数位数
 */
fun Float.round(decimals: Int): Float {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return (this * multiplier).roundToInt() / multiplier.toFloat()
}

/**
 * Double扩展：保留指定小数位数
 */
fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return (this * multiplier).roundToInt() / multiplier
}

/**
 * Long扩展：时间戳转换为格式化字符串
 */
fun Long.toTimeString(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    return if (this > 0) {
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
    } else {
        "未校准"
    }
}

/**
 * 计算指数退避延迟时间（秒）
 */
fun calculateBackoffDelay(retryCount: Int, baseDelay: Long = Constants.RETRY_BASE_DELAY_SECONDS): Long {
    return baseDelay * (1 shl retryCount) // 2^retryCount * baseDelay
}
