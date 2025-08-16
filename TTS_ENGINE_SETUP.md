# TTS引擎注册说明

## 概述

本项目已经实现了完整的TTS引擎注册功能，让系统能够识别并使用你的TTS引擎。

## 实现的功能

### 1. TTS引擎服务类
- `TTSEngineService.kt` - 实现了Android系统的TTS接口
- 继承自`TextToSpeechService`，符合Android 15的最新标准
- 集成了你现有的`SpeechService`功能

### 2. 系统注册配置
- 在`AndroidManifest.xml`中注册了TTS服务
- 创建了`tts_engine.xml`配置文件，定义了引擎属性和支持的语言
- 添加了必要的权限声明

### 3. 支持的语言
- 中文（简体、繁体、大陆、台湾、香港）
- 英文（美国、英国、澳大利亚）
- 日文、韩文、德文、法文、俄文

## 测试步骤

### 1. 编译并安装应用
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. 检查TTS引擎注册
1. 打开系统设置
2. 进入"辅助功能" → "文字转语音输出"
3. 在"首选引擎"中应该能看到"TTSOnline"选项
4. 选择你的引擎作为默认TTS引擎

### 3. 测试TTS功能
1. 在系统设置中点击"播放"按钮测试TTS
2. 使用其他支持TTS的应用（如阅读器）测试
3. 检查日志输出，确认TTS服务正常工作

## 技术细节

### 权限要求
- `BIND_TTS_SERVICE` - 绑定TTS服务的权限
- `WAKE_LOCK` - 保持设备唤醒的权限
- `INTERNET` - 网络访问权限（用于AI语音合成）

### 服务配置
- 服务名称：`com.wxh.ttsonline.function.TTSEngineService`
- 导出状态：`true`（允许系统绑定）
- 权限：`android.permission.BIND_TTS_SERVICE`

### 语言代码格式
- 使用标准的ISO语言-国家代码格式
- 例如：`zh-CN`（中文-中国）、`en-US`（英文-美国）

## 故障排除

### 1. TTS引擎未显示
- 检查应用是否正确安装
- 确认`AndroidManifest.xml`中的服务注册
- 查看logcat日志中的错误信息

### 2. TTS功能异常
- 检查网络连接（AI语音合成需要网络）
- 确认应用配置（App ID、Token等）
- 查看`SpeechService`的日志输出

### 3. 权限问题
- 确认应用已获得必要权限
- 检查Android版本兼容性（最低支持API 30）

## 注意事项

1. **首次使用**：用户需要在系统设置中手动选择你的TTS引擎
2. **网络依赖**：由于使用AI模型，需要稳定的网络连接
3. **性能考虑**：AI语音合成可能需要一些时间，建议添加加载提示
4. **错误处理**：确保在网络异常或配置错误时有适当的用户提示

## 后续优化建议

1. 添加TTS引擎设置界面，让用户可以直接在应用中配置
2. 实现离线语音合成功能，减少网络依赖
3. 添加语音质量设置选项
4. 实现语音缓存机制，提高响应速度
