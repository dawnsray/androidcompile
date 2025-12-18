package com.example.levelcheck.domain.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import com.example.levelcheck.data.model.SensorData
import com.example.levelcheck.domain.calibration.CalibrationManager
import kotlin.math.abs

/**
 * 传感器数据处理器 - 处理传感器原始数据，计算方位角和倾斜角
 */
class SensorProcessor(
    private val sensorManager: SensorManager,
    private val calibrationManager: CalibrationManager,
    private val onDataUpdate: (SensorData) -> Unit
) : SensorEventListener {
    
    private val TAG = "SensorProcessor"
    
    // 低通滤波器系数（0-1之间，越小越平滑但响应越慢）
    private val ALPHA = 0.15f
    
    // 磁力计数据需要更强的滤波（因为磁场波动通常较大）
    private val ALPHA_MAGNETOMETER = 0.1f
    
    // 传感器数据缓存（使用低通滤波后的数据）
    private val accelerometerData = FloatArray(3)
    private val magnetometerData = FloatArray(3)
    private var hasAccelerometerData = false
    private var hasMagnetometerData = false
    
    // 磁力计精度状态
    private var magnetometerAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
    
    // 旋转矩阵和方向角数组
    private val rotationMatrix = FloatArray(9)
    private val remappedRotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    
    // 传感器实例
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    
    /**
     * 检查传感器是否可用
     */
    fun isSensorAvailable(): Boolean {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        val available = accelerometer != null && magnetometer != null
        
        if (!available) {
            Log.e(TAG, "Required sensors not available")
        }
        
        return available
    }
    
    /**
     * 注册传感器监听器
     */
    fun register(): Boolean {
        if (!isSensorAvailable()) {
            return false
        }
        
        val accelRegistered = sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
        
        val magRegistered = sensorManager.registerListener(
            this,
            magnetometer,
            SensorManager.SENSOR_DELAY_UI
        )
        
        val success = accelRegistered && magRegistered
        
        if (success) {
            Log.d(TAG, "Sensors registered successfully")
        } else {
            Log.e(TAG, "Failed to register sensors")
        }
        
        return success
    }
    
    /**
     * 注销传感器监听器
     */
    fun unregister() {
        sensorManager.unregisterListener(this)
        hasAccelerometerData = false
        hasMagnetometerData = false
        Log.d(TAG, "Sensors unregistered")
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // 应用低通滤波器平滑加速度计数据
                if (hasAccelerometerData) {
                    accelerometerData[0] = lowPassFilter(event.values[0], accelerometerData[0])
                    accelerometerData[1] = lowPassFilter(event.values[1], accelerometerData[1])
                    accelerometerData[2] = lowPassFilter(event.values[2], accelerometerData[2])
                } else {
                    System.arraycopy(event.values, 0, accelerometerData, 0, 3)
                }
                hasAccelerometerData = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                // 应用更强的低通滤波器平滑磁力计数据（磁力计波动较大）
                if (hasMagnetometerData) {
                    magnetometerData[0] = lowPassFilter(event.values[0], magnetometerData[0], ALPHA_MAGNETOMETER)
                    magnetometerData[1] = lowPassFilter(event.values[1], magnetometerData[1], ALPHA_MAGNETOMETER)
                    magnetometerData[2] = lowPassFilter(event.values[2], magnetometerData[2], ALPHA_MAGNETOMETER)
                } else {
                    System.arraycopy(event.values, 0, magnetometerData, 0, 3)
                }
                hasMagnetometerData = true
            }
        }
        
        // 只有当两种传感器数据都就绪时才进行计算
        if (hasAccelerometerData && hasMagnetometerData) {
            calculateOrientation()
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 记录磁力计精度，用于判断方位角是否可靠
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetometerAccuracy = accuracy
            when (accuracy) {
                SensorManager.SENSOR_STATUS_UNRELIABLE -> 
                    Log.w(TAG, "磁力计精度低，请远离磁场干扰源并做“8字校准”")
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> 
                    Log.w(TAG, "磁力计精度较低，建议做8字校准")
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> 
                    Log.d(TAG, "磁力计精度中等")
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> 
                    Log.d(TAG, "磁力计精度高")
            }
        }
    }
    
    /**
     * 计算方位角和倾斜角
     */
    private fun calculateOrientation() {
        // 计算旋转矩阵
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerData,
            magnetometerData
        )
        
        if (!success) {
            Log.w(TAG, "Failed to calculate rotation matrix")
            return
        }
        
        // 不进行坐标系重映射，直接使用默认坐标系（适用于手机平放场景）
        // 默认坐标系: X轴=向右, Y轴=向前（顶部方向）, Z轴=垂直向上
        // 当手机平放（屏幕朝上）时：
        // - orientationAngles[0] = azimuth（方位角，绕Z轴）
        // - orientationAngles[1] = pitch（俯仰角，绕X轴，顶部抬起为正）
        // - orientationAngles[2] = roll（翻滚角，绕Y轴，右侧抬起为正）
        
        // 直接使用原始旋转矩阵计算方位数据
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        
        // 提取原始角度（弧度制）
        val azimuthRad = orientationAngles[0]  // 绕Z轴旋转（罗盘方向）
        val pitchRad = orientationAngles[1]    // 绕X轴旋转（前后倾斜，顶部抬起为正）
        val rollRad = orientationAngles[2]     // 绕Y轴旋转（左右倾斜，右侧抬起为正）
        
        // 转换为度数
        val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
        val pitchDeg = Math.toDegrees(pitchRad.toDouble()).toFloat()
        val rollDeg = Math.toDegrees(rollRad.toDouble()).toFloat()
        
        // 方位角转换为0-360度
        val azimuth = (azimuthDeg + 360f) % 360f
        
        // 应用方位角校准
        val azimuthCalibrated = calibrationManager.applyAzimuthCalibration(azimuth)
        
        // 倾斜角：使用pitch和roll的合成角度来计算总倾斜角
        // 当手机完全平放时，pitch和roll都接近0，倾斜角接近0
        val tilt = kotlin.math.sqrt(
            (pitchDeg * pitchDeg + rollDeg * rollDeg).toDouble()
        ).toFloat().coerceIn(0f, 90f)
        
        // 应用倾斜角校准偏移量
        val calibratedTilt = calibrationManager.applyTiltCalibration(tilt)
        
        // 计算倾斜方向角度（相对用户视角，手机平放场景）
        val tiltDirectionAngle = calculateTiltDirectionAngle(rollDeg, pitchDeg)
        
        // 解析绝对方向（使用校准后的方位角）
        val absoluteDirection = DirectionResolver.resolveAbsoluteDirection(azimuthCalibrated)
        
        // 解析相对方向（基于倾斜方向角度）
        val relativeDirection = DirectionResolver.resolveRelativeDirection(tiltDirectionAngle)
        
        // 计算磁场强度
        val magneticFieldStrength = kotlin.math.sqrt(
            magnetometerData[0] * magnetometerData[0] +
            magnetometerData[1] * magnetometerData[1] +
            magnetometerData[2] * magnetometerData[2]
        )
        
        // 调试日志
        Log.d(TAG, "Orientation - Roll: $rollDeg°, Pitch: $pitchDeg°, TiltDir: $tiltDirectionAngle°, Direction: $relativeDirection")
        
        // 创建传感器数据对象
        val sensorData = SensorData(
            azimuthRaw = azimuthRad,
            tiltRaw = pitchRad,
            azimuthDegrees = azimuth,
            azimuthCalibratedDegrees = azimuthCalibrated,
            tiltDegrees = tilt,
            tiltCalibratedDegrees = calibratedTilt,
            direction = relativeDirection, // 使用相对方向
            rollDegrees = rollDeg,
            pitchDegrees = pitchDeg,
            tiltDirectionAngle = tiltDirectionAngle,
            magneticFieldStrength = magneticFieldStrength,
            magnetometerAccuracy = magnetometerAccuracy,
            timestamp = System.currentTimeMillis()
        )
        
        // 回调通知数据更新
        onDataUpdate(sensorData)
    }
    
    /**
     * 计算倾斜方向角度（相对用户视角，手机平放场景）
     * 当手机平放（屏幕朝上）时的倾斜方向判断
     * @param rollDeg Roll角度（绕Y轴旋转，右侧抬起为正）
     * @param pitchDeg Pitch角度（绕X轴旋转，顶部抬起为正）
     * @return 倾斜方向角度（0-360度，0度=向前/顶部抬起，90度=向右，180度=向后/底部抬起，270度=向左）
     */
    private fun calculateTiltDirectionAngle(rollDeg: Float, pitchDeg: Float): Float {
        // 手机平放场景下的方向映射：
        // Pitch > 0: 顶部抬起 -> 向前 -> 0度
        // Roll > 0:  右侧抬起 -> 向右 -> 90度
        // Pitch < 0: 底部抬起 -> 向后 -> 180度
        // Roll < 0:  左侧抬起 -> 向左 -> 270度
        
        // 使用 atan2(x, y) 来计算方向角度
        // atan2 返回的角度范围是 -180° 到 +180°
        // 我们需要将其转换为 0° 到 360°，并且：
        // - pitchDeg > 0 (顶部抬起) 应该对应 0°
        // - rollDeg > 0 (右侧抬起) 应该对应 90°
        // - pitchDeg < 0 (底部抬起) 应该对应 180°
        // - rollDeg < 0 (左侧抬起) 应该对应 270°
        
        // 使用 atan2(rollDeg, pitchDeg):
        // - 当 pitch=10, roll=0 时: atan2(0, 10) = 0° ✓
        // - 当 pitch=0, roll=10 时: atan2(10, 0) = 90° ✓
        // - 当 pitch=-10, roll=0 时: atan2(0, -10) = 180° ✓
        // - 当 pitch=0, roll=-10 时: atan2(-10, 0) = -90° = 270° ✓
        
        val angleRad = kotlin.math.atan2(rollDeg.toDouble(), pitchDeg.toDouble())
        var angleDeg = Math.toDegrees(angleRad).toFloat()
        
        // 转换为 0-360 度范围
        angleDeg = (angleDeg + 360f) % 360f
        
        return angleDeg
    }
    
    /**
     * 获取当前方位角（用于校准）
     */
    fun getCurrentAzimuth(): Float {
        if (!hasAccelerometerData || !hasMagnetometerData) {
            return 0f
        }
        
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerData,
            magnetometerData
        )
        
        if (!success) {
            return 0f
        }
        
        // 使用原始旋转矩阵（适用于平放场景）
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val azimuthRad = orientationAngles[0]
        val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
        
        // 转换为0-360度
        return (azimuthDeg + 360f) % 360f
    }
    
    /**
     * 获取磁力计精度
     */
    fun getMagnetometerAccuracy(): Int {
        return magnetometerAccuracy
    }
    
    /**
     * 获取当前倾斜角（用于校准）
     */
    fun getCurrentTilt(): Float {
        if (!hasAccelerometerData || !hasMagnetometerData) {
            return 0f
        }
        
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerData,
            magnetometerData
        )
        
        if (!success) {
            return 0f
        }
        
        // 使用原始旋转矩阵（适用于平放场景）
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val pitchRad = orientationAngles[1]
        val rollRad = orientationAngles[2]
        val pitchDeg = Math.toDegrees(pitchRad.toDouble()).toFloat()
        val rollDeg = Math.toDegrees(rollRad.toDouble()).toFloat()
        
        // 使用pitch和roll的合成角度作为总倾斜角
        return kotlin.math.sqrt(
            (pitchDeg * pitchDeg + rollDeg * rollDeg).toDouble()
        ).toFloat().coerceIn(0f, 90f)
    }
    
    /**
     * 低通滤波器 - 平滑传感器数据
     * @param input 新的传感器读数
     * @param output 上次的滤波后数据
     * @param alpha 滤波系数
     * @return 滤波后的数据
     */
    private fun lowPassFilter(input: Float, output: Float, alpha: Float = ALPHA): Float {
        return output + alpha * (input - output)
    }
}
