# 水平仪数据采集与上传应用

这是一个Android应用，用于采集手机传感器数据（方位角和倾斜角），并定时上传到云服务器。

## 功能特性

- ✅ 实时显示方位角（0-360°）和倾斜角（0-90°）
- ✅ 方位角转换为八个方位显示（东、南、西、北、东南、东北、西南、西北）
- ✅ 倾斜角显示为整数
- ✅ 支持用户校准功能
- ✅ 可配置数据上传频率（1-30秒，默认5秒）
- ✅ 网络不稳定时自动重连（最多5次，每次间隔指数退避）
- ✅ 后台运行支持（ForegroundService）
- ✅ 服务器地址可配置

## 技术架构

- **语言**: Kotlin
- **架构**: MVVM
- **最低版本**: Android 10.0 (API Level 29)
- **目标版本**: Android 14 (API Level 34)

### 核心技术栈

- AndroidX (AppCompat, ConstraintLayout, Lifecycle, ViewModel, LiveData)
- Kotlin Coroutines
- Retrofit 2.9.0
- OkHttp 4.12.0
- Gson 2.10.1

## 项目结构

```
app/
├── src/main/
│   ├── java/com/example/levelcheck/
│   │   ├── ui/                          # UI层
│   │   │   ├── main/                    # 主界面
│   │   │   ├── calibration/             # 校准界面
│   │   │   └── settings/                # 设置界面
│   │   ├── service/                     # 服务层
│   │   │   └── SensorService            # 前台服务
│   │   ├── domain/                      # 业务逻辑层
│   │   │   ├── sensor/                  # 传感器处理
│   │   │   └── calibration/             # 校准管理
│   │   ├── data/                        # 数据层
│   │   │   ├── model/                   # 数据模型
│   │   │   ├── repository/              # 数据仓库
│   │   │   └── remote/                  # 网络接口
│   │   └── util/                        # 工具类
│   ├── res/                             # 资源文件
│   └── AndroidManifest.xml              # 应用配置
└── build.gradle.kts                     # 构建配置
```

## 核心模块说明

### 1. 数据模型 (data/model)
- **LevelData**: 上传数据模型
- **SensorData**: 传感器数据模型
- **CalibrationData**: 校准数据模型
- **AppConfig**: 应用配置模型
- **NetworkStatus**: 网络状态枚举

### 2. 业务逻辑 (domain)
- **SensorProcessor**: 传感器数据处理，使用`SensorManager.getRotationMatrix()`和`getOrientation()`
- **DirectionResolver**: 方位角转换为方向名称（八方位）
- **CalibrationManager**: 校准数据管理和偏移量应用

### 3. 数据仓库 (data/repository)
- **PreferenceRepository**: SharedPreferences封装
- **NetworkRepository**: 网络请求封装，包含重试机制

### 4. 前台服务 (service)
- **SensorService**: 
  - 持续采集传感器数据
  - 定时上传数据
  - 网络异常处理和重连
  - 前台通知显示

### 5. UI界面 (ui)
- **MainActivity**: 主界面，显示实时数据和状态
- **CalibrationActivity**: 校准界面
- **SettingsActivity**: 设置界面

## 关键功能实现

### 传感器数据计算

使用加速度计和磁力计计算设备方位：

1. 注册`TYPE_ACCELEROMETER`和`TYPE_MAGNETIC_FIELD`传感器
2. 使用`SensorManager.getRotationMatrix()`计算旋转矩阵
3. 使用`SensorManager.getOrientation()`获取方位数据
4. 转换为度数并应用校准偏移量

### 校准功能

1. 采集10个样本（间隔100ms）
2. 计算平均倾斜角
3. 保存为偏移量到SharedPreferences
4. 后续计算自动减去偏移量

### 网络重试机制

采用指数退避算法：
- 第1次重试：延迟2秒
- 第2次重试：延迟4秒
- 第3次重试：延迟8秒
- 第4次重试：延迟16秒
- 第5次重试：延迟32秒
- 超过5次：暂停采集，每30秒探测网络

### 后台服务

使用ForegroundService确保后台持续运行：
- 显示前台通知
- 设置`foregroundServiceType="location"`
- 返回`START_STICKY`以便系统重启服务

## 配置说明

### 首次使用配置

首次启动应用时，需要配置服务器信息：

- **服务器地址**：填写您的服务器IP地址或域名（如：`192.168.1.100` 或 `example.com`）
- **服务器端口**：默认 `8020`（根据服务器实际配置填写）
- **上传频率**：默认 `5秒`（可调整范围：1-30秒）
- **API端点**：`POST /api/level/upload`（自动配置）

> **注意**：未配置服务器地址前，应用将无法正常使用。请在设置界面完成配置后再启动服务。

### 数据格式

```json
{
  "azimuth": 45.6,
  "tilt": 3,
  "timestamp": 1635772800000
}
```

## 权限说明

- `INTERNET`: 网络数据上传
- `ACCESS_NETWORK_STATE`: 检测网络状态
- `FOREGROUND_SERVICE`: 启动前台服务
- `FOREGROUND_SERVICE_LOCATION`: Android 14+前台服务类型
- `POST_NOTIFICATIONS`: Android 13+显示通知

## 使用说明

1. **启动应用**：首次启动时请求必要权限
2. **启动服务**：点击"启动服务"按钮开始数据采集
3. **查看数据**：主界面实时显示方位和倾斜角
4. **校准**：点击"校准"按钮，将手机平放在水平面上进行校准
5. **设置**：点击"设置"按钮配置服务器地址和上传频率
6. **后台运行**：服务在后台持续运行，通知栏显示状态

## 开发说明

### 构建项目

```bash
./gradlew build
```

### 运行测试

```bash
./gradlew test
```

### 安装APK

```bash
./gradlew installDebug
```

## 注意事项

1. 应用仅支持Android 10.0+设备
2. 设备必须具备加速度计和磁力计
3. 网络请求使用HTTP协议，服务器需支持明文流量
4. 校准数据保存在本地，卸载应用会丢失
5. 服务在后台运行会消耗电量，建议合理设置上传频率

## 版本信息

- **版本号**: 1.0.0 (versionCode: 1)
- **编译SDK**: 34
- **最低SDK**: 29
- **目标SDK**: 34

## 开发者

该应用严格按照设计文档实现，采用MVVM架构，代码结构清晰，遵循Android最佳实践。
