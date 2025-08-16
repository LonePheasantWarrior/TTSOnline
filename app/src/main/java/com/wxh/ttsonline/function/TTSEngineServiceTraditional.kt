package com.wxh.ttsonline.function

import android.content.Context
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import com.wxh.ttsonline.configuration.LogTag

/**
 * TTS引擎服务类 - 传统接口版本
 * 尝试使用更兼容的TTS引擎注册方式
 */
class TTSEngineServiceTraditional : TextToSpeechService() {

    private lateinit var speechService: SpeechService

    override fun onCreate() {
        super.onCreate()
        speechService = SpeechService(this)
        Log.d(LogTag.SDK_INFO, "TTSEngineServiceTraditional onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LogTag.SDK_INFO, "TTSEngineServiceTraditional onDestroy")
    }

    /**
     * 处理语音合成请求
     * @param request 合成请求
     * @param callback 合成回调
     */
    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        Log.d(LogTag.SDK_INFO, "TTSEngineServiceTraditional onSynthesizeText: ${request.charSequenceText}")
        
        try {
            // 调用你的SpeechService来处理TTS请求
            speechService.tts(request, callback)
        } catch (e: Exception) {
            Log.e(LogTag.SPEECH_ERROR, "TTS合成失败: ${e.message}", e)
            callback.error(TextToSpeech.ERROR_SERVICE)
        }
    }

    /**
     * 停止语音合成
     */
    override fun onStop() {
        Log.d(LogTag.SDK_INFO, "TTSEngineServiceTraditional onStop")
        try {
            speechService.ttsStop()
        } catch (e: Exception) {
            Log.e(LogTag.SPEECH_ERROR, "停止TTS失败: ${e.message}", e)
        }
    }

    /**
     * 获取默认语言
     * @return 默认语言代码
     */
    override fun onGetLanguage(): Array<String> {
        return arrayOf("zh-CN")
    }

    /**
     * 检查是否支持指定的语言
     * @param lang 语言代码
     * @param country 国家代码
     * @param variant 变体代码
     * @return 是否支持
     */
    override fun onIsLanguageAvailable(lang: String, country: String, variant: String): Int {
        // 支持所有语言
        return TextToSpeech.LANG_AVAILABLE
    }

    /**
     * 加载指定的语言
     * @param lang 语言代码
     * @param country 国家代码
     * @param variant 变体代码
     * @return 加载结果状态
     */
    override fun onLoadLanguage(
        lang: String?,
        country: String?,
        variant: String?
    ): Int {
        // 支持所有语言
        return TextToSpeech.LANG_AVAILABLE
    }
}
