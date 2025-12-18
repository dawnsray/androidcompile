package com.example.levelcheck.domain.sensor

/**
 * 方向解析器 - 将方位角转换为八个方位名称，或将倾斜方向角转换为相对用户的方向
 */
object DirectionResolver {
    
    /**
     * 根据方位角解析绝对方向名称（东南西北）
     * @param azimuth 方位角（0-360度）
     * @return 方位名称
     */
    fun resolveAbsoluteDirection(azimuth: Float): String {
        return when {
            azimuth >= 337.5f || azimuth < 22.5f -> "北"
            azimuth >= 22.5f && azimuth < 67.5f -> "东北"
            azimuth >= 67.5f && azimuth < 112.5f -> "东"
            azimuth >= 112.5f && azimuth < 157.5f -> "东南"
            azimuth >= 157.5f && azimuth < 202.5f -> "南"
            azimuth >= 202.5f && azimuth < 247.5f -> "西南"
            azimuth >= 247.5f && azimuth < 292.5f -> "西"
            azimuth >= 292.5f && azimuth < 337.5f -> "西北"
            else -> "未知"
        }
    }
    
    /**
     * 根据倾斜方向角度解析相对用户的方向
     * @param tiltDirectionAngle 倾斜方向角度（0度=向前，90度=向右，180度=向后，270度=向左）
     * @return 相对方向名称
     */
    fun resolveRelativeDirection(tiltDirectionAngle: Float): String {
        return when {
            tiltDirectionAngle >= 337.5f || tiltDirectionAngle < 22.5f -> "向前"
            tiltDirectionAngle >= 22.5f && tiltDirectionAngle < 67.5f -> "向右前"
            tiltDirectionAngle >= 67.5f && tiltDirectionAngle < 112.5f -> "向右"
            tiltDirectionAngle >= 112.5f && tiltDirectionAngle < 157.5f -> "向右后"
            tiltDirectionAngle >= 157.5f && tiltDirectionAngle < 202.5f -> "向后"
            tiltDirectionAngle >= 202.5f && tiltDirectionAngle < 247.5f -> "向左后"
            tiltDirectionAngle >= 247.5f && tiltDirectionAngle < 292.5f -> "向左"
            tiltDirectionAngle >= 292.5f && tiltDirectionAngle < 337.5f -> "向左前"
            else -> "水平"
        }
    }
    
    /**
     * 保留兼容方法，默认返回绝对方向
     */
    fun resolve(azimuth: Float): String {
        return resolveAbsoluteDirection(azimuth)
    }
}
