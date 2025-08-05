package com.wxh.ttsonline.configuration

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import com.wxh.ttsonline.function.SpeechEngine
import com.wxh.ttsonline.function.SpeechService

/**
 * 应用初始化相关
 */
class TTSApplication : Application() {
    lateinit var speechEngine: SpeechEngine private set
    lateinit var speechService: SpeechService private set

    override fun onCreate() {
        super.onCreate()
        // 初始化火山引擎语音合成环境
        // 在Application创建时调用，确保整个应用生命周期内只执行一次
        SpeechEngineGenerator.PrepareEnvironment(applicationContext, this)
        Log.d(LogTag.SDK_INFO, "火山引擎语音合成环境初始化完成")

        // 向工程上下文注册基础组建
        speechEngine = SpeechEngine(this)
        Log.d(LogTag.COMMON_INFO, "本地引擎已初始化")
        speechService = SpeechService(this)
        Log.d(LogTag.COMMON_INFO, "本地引擎服务已初始化")
    }
}