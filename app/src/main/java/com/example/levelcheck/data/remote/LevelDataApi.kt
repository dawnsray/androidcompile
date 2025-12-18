package com.example.levelcheck.data.remote

import com.example.levelcheck.data.model.LevelData
import com.example.levelcheck.data.model.SensorDataUpload
import com.example.levelcheck.util.Constants
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 水平仪数据API接口
 */
interface LevelDataApi {
    
    /**
     * 上传水平仪数据（旧接口，暂时保留）
     * @param data 水平仪数据
     * @return 响应结果
     */
    @POST(Constants.API_ENDPOINT)
    suspend fun uploadLevelData(@Body data: LevelData): Response<Unit>
    
    /**
     * 上传传感器数据到 /api/sensors 接口
     * @param data 传感器数据
     * @return 响应结果
     */
    @POST("/api/sensors")
    suspend fun uploadSensorData(@Body data: SensorDataUpload): Response<Unit>
}
