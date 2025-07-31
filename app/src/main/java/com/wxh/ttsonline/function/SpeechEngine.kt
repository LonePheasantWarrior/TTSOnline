package com.wxh.ttsonline.function

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.bytedance.speech.speechengine.SpeechEngine
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.bytedance.speech.speechengine.SpeechEngineGenerator
import com.google.gson.Gson
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
    private var isInitialized: Boolean = false

    /**
     * 语音合成参数
     */
    private var startEnginePayload: StartEnginePayload? = null

    private var gson = Gson()

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
     * 获取语音合成参数
     */
    fun getStartEnginePayload(): StartEnginePayload {
        if (!isInitialized) {
            throw IllegalStateException("语音合成引擎未初始化")
        }
        return startEnginePayload!!
    }

    /**
     * 创建引擎实例
     */
    private fun createEngine(): SpeechEngine? {
        destroy()

        engine = SpeechEngineGenerator.getInstance()
        if (engine == null) {
            return null
        }
        engine!!.createEngine()
        return engine as SpeechEngine
    }

    /**
     * 销毁引擎实例
     */
    fun destroy() {
        isInitialized = false
        if (engine != null) {
            engine!!.destroyEngine()
        }
        engine = null
    }

    /**
     * 初始化引擎实例
     */
    fun initEngine(appId: String, token: String, speakerType: String?) {
        //创建全局引擎实例
        createEngine()
        if (engine == null) {
            Log.e(LogTag.SDK_ERROR, "语音合成引擎实例创建失败")
            Toast.makeText(context, "引擎创建失败", Toast.LENGTH_SHORT).show()
            return
        }
        startEnginePayload = if (speakerType != null) {
            StartEnginePayloadBuilder().setSpeaker(speakerType).build();
        }else {
            StartEnginePayloadBuilder().build();
        }

        //初始化引擎参数
        //引擎名称
        engine!!.setOptionString(
            Dictionary.SpeechEngine.ENGINE_NAME, SpeechEngineDefines.BITTS_ENGINE
        )
        //鉴权相关：Appid
        engine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_ID_STRING, appId)
        //鉴权相关：Token
        engine!!.setOptionString(SpeechEngineDefines.PARAMS_KEY_APP_TOKEN_STRING, token)
        //语音合成服务域名
        engine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_ADDRESS_STRING,
            Dictionary.SpeechEngine.SERVER_ADDRESS
        )
        //语音合成服务Uri
        engine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_TTS_URI_STRING, Dictionary.SpeechEngine.API_PATH
        )
        //语音合成服务Resource id
        engine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_RESOURCE_ID_STRING, Dictionary.SpeechEngine.RES_ID
        )
        //语音合成服务StartSession时传入的音频相关参数，json字符串格式
        engine!!.setOptionString(
            SpeechEngineDefines.PARAMS_KEY_START_ENGINE_PAYLOAD_STRING, gson.toJson(startEnginePayload)
        )

        //初始化引擎实例
        val initRes = engine!!.initEngine()
        if (initRes != SpeechEngineDefines.ERR_NO_ERROR) {
            Log.e(LogTag.SDK_ERROR, "语音合成引擎实例初始化失败,错误代码: $initRes")
            Toast.makeText(context, "引擎初始化失败,错误代码: $initRes", Toast.LENGTH_SHORT).show()
            return
        }
        engine!!.setContext(context)
        engine!!.setListener((context.applicationContext as TTSApplication).speechService)

        Log.i(LogTag.SDK_INFO, "语音合成引擎实例初始化完成")
        isInitialized = true
    }
}