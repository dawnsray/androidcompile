package com.example.levelcheck.data.repository

import android.util.Log
import com.example.levelcheck.data.model.LevelData
import com.example.levelcheck.data.model.NetworkStatus
import com.example.levelcheck.data.model.SensorDataUpload
import com.example.levelcheck.data.remote.LevelDataApi
import com.example.levelcheck.util.Constants
import com.example.levelcheck.util.calculateBackoffDelay
import com.google.gson.GsonBuilder
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络数据仓库 - 封装网络请求逻辑
 */
class NetworkRepository(
    private val preferenceRepository: PreferenceRepository
) {
    
    private val TAG = "NetworkRepository"
    
    private var retrofit: Retrofit? = null
    private var api: LevelDataApi? = null
    private var retryCount = 0
    
    /**
     * 初始化Retrofit实例
     */
    private fun initRetrofit() {
        val config = preferenceRepository.getAppConfig()
        val baseUrl = "http://${config.serverHost}:${config.serverPort}"
        
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(Constants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
        
        val gson = GsonBuilder()
            .setLenient()
            .create()
        
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        
        api = retrofit?.create(LevelDataApi::class.java)
        
        Log.d(TAG, "Retrofit initialized with base URL: $baseUrl")
    }
    
    /**
     * 获取API实例，如果未初始化则先初始化
     */
    private fun getApi(): LevelDataApi {
        if (api == null) {
            initRetrofit()
        }
        return api!!
    }
    
    /**
     * 重新初始化Retrofit（配置变更时调用）
     */
    fun reinitialize() {
        Log.d(TAG, "Reinitializing Retrofit with new configuration")
        retrofit = null
        api = null
        initRetrofit()
    }
    
    /**
     * 上传水平仪数据（带重试机制）
     * 使用新的 /api/sensors 接口
     * @param data 水平仪数据
     * @return 上传结果状态
     */
    suspend fun uploadData(data: LevelData): NetworkStatus {
        try {
            // 将 LevelData 转换为 SensorDataUpload 格式
            // 传感器数据：将 tilt 值作为单个传感器角度上传
            val sensorData = SensorDataUpload(
                sensors = listOf(data.tilt.toDouble())
            )
            
            val response = getApi().uploadSensorData(sensorData)
            
            if (response.isSuccessful) {
                // 上传成功，重置重试计数器
                retryCount = 0
                Log.d(TAG, "Sensor data uploaded successfully: tilt=${data.tilt}°")
                return NetworkStatus.CONNECTED
            } else {
                // 服务器返回错误状态码
                Log.w(TAG, "Upload failed with code: ${response.code()}")
                
                // HTTP 400错误不重试
                if (response.code() == 400) {
                    Log.e(TAG, "Bad request, skipping retry")
                    retryCount = 0
                    return NetworkStatus.CONNECTED // 继续下次上传
                }
                
                // 其他错误触发重试
                return handleUploadFailure()
            }
        } catch (e: java.net.UnknownHostException) {
            // 服务器地址无法解析，可能未配置或网络不可用
            Log.w(TAG, "Server address cannot be resolved, treating as disconnected")
            retryCount = 0
            return NetworkStatus.DISCONNECTED
        } catch (e: java.net.ConnectException) {
            // 无法连接到服务器
            Log.w(TAG, "Cannot connect to server: ${e.message}")
            return handleUploadFailure()
        } catch (e: java.net.SocketTimeoutException) {
            // 连接超时
            Log.w(TAG, "Connection timeout: ${e.message}")
            return handleUploadFailure()
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception: ${e.message}", e)
            return handleUploadFailure()
        }
    }
    
    /**
     * 处理上传失败
     */
    private suspend fun handleUploadFailure(): NetworkStatus {
        retryCount++
        
        if (retryCount <= Constants.MAX_RETRY_COUNT) {
            // 计算退避延迟
            val delaySeconds = calculateBackoffDelay(retryCount)
            Log.d(TAG, "Retry $retryCount/${Constants.MAX_RETRY_COUNT}, waiting ${delaySeconds}s")
            
            delay(delaySeconds * 1000)
            return NetworkStatus.CONNECTING
        } else {
            // 超过最大重试次数，暂停采集
            Log.e(TAG, "Max retry count reached, pausing data collection")
            return NetworkStatus.DISCONNECTED
        }
    }
    
    /**
     * 发送网络探测请求（恢复检测用）
     * @return 是否成功连接
     */
    suspend fun probeNetwork(): Boolean {
        return try {
            val testData = SensorDataUpload(
                sensors = listOf(0.0)  // 测试数据
            )
            val response = getApi().uploadSensorData(testData)
            val success = response.isSuccessful
            
            if (success) {
                // 网络恢复，重置重试计数器
                retryCount = 0
                Log.d(TAG, "Network probe successful, network recovered")
            } else {
                Log.d(TAG, "Network probe failed with code: ${response.code()}")
            }
            
            success
        } catch (e: Exception) {
            Log.d(TAG, "Network probe exception: ${e.message}")
            false
        }
    }
    
    /**
     * 重置重试计数器
     */
    fun resetRetryCount() {
        retryCount = 0
    }
    
    /**
     * 获取当前重试次数
     */
    fun getRetryCount(): Int = retryCount
}
