package com.wxh.ttsonline.function

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.bytedance.speech.speechengine.SpeechEngine
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import com.wxh.ttsonline.configuration.Dictionary
import com.wxh.ttsonline.configuration.LogTag
import com.wxh.ttsonline.configuration.TTSApplication
import com.wxh.ttsonline.dto.StartEnginePayload
import com.wxh.ttsonline.dto.StartEnginePayloadBuilder

/**
 * 语音合成引擎相关功能
 */
class SpeechEngine(private val context: Context) {
    /**
     * 语音合成引擎
     */
    private var engine: SpeechEngine? = null

    /**
     * 语音合成引擎是否已初始化完毕
     */
    var isInitialized: Boolean = false

    /**
     * 获取语音合成引擎
     */
    fun getEngine(): SpeechEngine {
        if (!isInitialized) {
            throw IllegalStateException("语音合成引擎未初始化")
        }
        return engine!!
    }

    /**
     * 创建引擎实例
     */
    private fun createEngine(): Long? {
        engine = SpeechEngineGenerator.getInstance()
        if (engine == null) {
            return null
        }
        return engine!!.createEngine()
    }

    /**
     * 销毁引擎实例
     */
    fun destroy() {
        isInitialized = false
        if (engine != null) {
            engine!!.destroyEngine()
            Log.d(LogTag.SDK_INFO, "历史语音合成引擎实例已经销毁")
        }
        engine = null
    }

    /**
     * 初始化引擎实例
     * @param appId 火山引擎在线语音合成服务appId
     * @param token 火山引擎在线语音合成服务token
     * @param speakerType 音色类型
     * @param text 待合成语音的文本
     * @param pitchRatio 音色对应音高(大模型语音合成服务不支持设置自定义的音量和音高！)
     * @param volumeRatio 音色对应音量(大模型语音合成服务不支持设置自定义的音量和音高！)
     * @param speedRatio 音色对应语速
     */
    fun initEngine(
        appId: String,
        token: String,
        speakerType: String,
        text: String,
        pitchRatio: Double?,
        volumeRatio: Double?,
        speedRatio: Double?,
        isEnablePlayer: Boolean
    ) {
        if (text.isBlank()) {
            Log.w(LogTag.SPEECH_ERROR, "待处理文本内容为空,使用默认内容")
            return
        }
        destroy()
        //创建全局引擎实例
        createEngine()
        if (engine == null) {
            Log.e(LogTag.SDK_ERROR, "语音合成引擎实例创建失败")
            Toast.makeText(context, "引擎创建失败", Toast.LENGTH_SHORT).show()
            return
        }
        engine!!.setContext(context)

        //初始化引擎参数
        engine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_LOG_LEVEL_STRING, SpeechEngineDefines.LOG_LEVEL_DEBUG
        )
        engine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_UID_STRING, "default"
        )
        //引擎名称
        engine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_ENGINE_NAME_STRING, SpeechEngineDefines.TTS_ENGINE
        )
        //在线合成 TTS_WORK_MODE_ONLINE：只进行在线合成，不需要配置离线合成相关参数
        engine!!.setOptionInt(
            SpeechEngineDefines.PARAMS_KEY_TTS_WORK_MODE_INT,
            SpeechEngineDefines.TTS_WORK_MODE_ONLINE
        )
        //鉴权相关：AppId
        engine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_ID_STRING, appId)
        //鉴权相关：Token
        engine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_TOKEN_STRING, "Bearer;$token")
        //语音合成服务域名
        engine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_ADDRESS_STRING,
            Dictionary.SpeechEngine.SERVER_ADDRESS
        )
        //语音合成服务Uri
        engine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_URI_STRING, Dictionary.SpeechEngine.TTS_URL
        )
        //语音合成服务Cluster
        engine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_CLUSTER_STRING, Dictionary.SpeechEngine.CLUSTER
        )
        //在线合成使用的“发音人类型”
        engine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_VOICE_TYPE_ONLINE_STRING, speakerType
        )
        //在线合成使用的“发音人类型”
        engine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_VOICE_ONLINE_STRING, Dictionary.SpeechEngine.VOICE
        )
        //语音合成 SDK 默认使用内置的播放器播放合成的音频，如果开发者希望使用其他播放器，可以通过以下配置项禁用内置播放器
        engine!!.setOptionBoolean(
            SpeechEngineDefines.PARAMS_KEY_TTS_ENABLE_PLAYER_BOOL, isEnablePlayer
        )
        if (!isEnablePlayer) {
            //语音合成 SDK 支持返回合成出来的音频数据，可以通过监听回调MESSAGE_TYPE_TTS_AUDIO_DATA来拿到PCM格式的音频流
            //默认关闭，需要配置以下参数开启
            engine!!.setOptionInt(
                SpeechEngineDefines.PARAMS_KEY_TTS_DATA_CALLBACK_MODE_INT,
                SpeechEngineDefines.TTS_DATA_CALLBACK_MODE_ALL
            )
        }
        //语音合成 SDK 提供了两种合成场景
        //单次合成场景 TTS_SCENARIO_TYPE_NORMAL：又称单句场景，引擎每次启动，只合成、播放一段文本的；
        //连续合成场景 TTS_SCENARIO_TYPE_NOVEL：适用于听书业务，每次启动引擎后可以根据需求合成、播放多段文本；
        engine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_SCENARIO_STRING,
            SpeechEngineDefines.TTS_SCENARIO_TYPE_NORMAL
        )
        var pitchRatioLocal = 1.0
        if (pitchRatio != null) {
            pitchRatioLocal = pitchRatio
        }
        // 音色对应音高
        engine!!.setOptionDouble(
            SpeechEngineDefines.PARAMS_KEY_TTS_PITCH_RATIO_DOUBLE, pitchRatioLocal
        )
        var volumeRatioLocal = 1.0
        if (volumeRatio != null) {
            volumeRatioLocal = volumeRatio
        }
        // 音色对应音量
        engine!!.setOptionDouble(
            SpeechEngineDefines.PARAMS_KEY_TTS_VOLUME_RATIO_DOUBLE, volumeRatioLocal
        )
        var speedRatioLocal = 1.0
        if (speedRatio != null) {
            speedRatioLocal = speedRatio
        }
        // 音色对应语速
        engine!!.setOptionDouble(
            SpeechEngineDefines.PARAMS_KEY_TTS_SPEED_RATIO_DOUBLE, speedRatioLocal
        )
        //设置待合成语音的文本内容
        engine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_TTS_TEXT_STRING, text)


        //初始化引擎实例
        val initRes = engine!!.initEngine()
        if (initRes != SpeechEngineDefines.ERR_NO_ERROR) {
            Log.e(LogTag.SDK_ERROR, "语音合成引擎实例初始化失败,错误代码: $initRes")
            Toast.makeText(context, "引擎初始化失败,错误代码: $initRes", Toast.LENGTH_SHORT).show()
            return
        }
        engine!!.setListener((context.applicationContext as TTSApplication).speechService)

        Log.i(LogTag.SDK_INFO, "语音合成引擎实例初始化完成")
        isInitialized = true
    }
}