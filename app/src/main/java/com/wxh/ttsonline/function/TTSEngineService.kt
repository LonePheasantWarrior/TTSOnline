package com.wxh.ttsonline.function

import android.content.Context
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import com.wxh.ttsonline.configuration.LogTag

/**
 * TTS引擎服务类，实现Android系统的TTS接口
 * 用于注册到系统的"文字转语音输出"菜单中
 */
class TTSEngineService : TextToSpeechService() {

    private lateinit var speechService: SpeechService

    override fun onCreate() {
        super.onCreate()
        speechService = SpeechService(this)
        Log.d(LogTag.SDK_INFO, "TTSEngineService onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LogTag.SDK_INFO, "TTSEngineService onDestroy")
    }

    /**
     * 处理语音合成请求
     * @param request 合成请求
     * @param callback 合成回调
     */
    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        Log.d(LogTag.SDK_INFO, "TTSEngineService onSynthesizeText: ${request.charSequenceText}")
        
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
        Log.d(LogTag.SDK_INFO, "TTSEngineService onStop")
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
        // 返回默认语言，这里返回中文简体
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
        val languageCode = "$lang-$country"
        val supportedLanguages = setOf(
            "zh-CN", // 中文简体（中国大陆）
            "zh-TW", // 中文繁体（台湾）
            "zh-HK", // 中文繁体（香港）
            "en-US", // 英文（美国）
            "en-GB", // 英文（英国）
            "en-AU", // 英文（澳大利亚）
            "ja-JP", // 日文
            "ko-KR", // 韩文
            "de-DE", // 德文
            "fr-FR", // 法文
            "ru-RU"  // 俄文
        )
        
        return when {
            supportedLanguages.contains(languageCode) -> TextToSpeech.LANG_AVAILABLE
            supportedLanguages.contains(lang) -> TextToSpeech.LANG_AVAILABLE
            else -> TextToSpeech.LANG_NOT_SUPPORTED
        }
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
        if (lang.isNullOrBlank()) {
            return TextToSpeech.LANG_NOT_SUPPORTED
        }
        
        val languageCode = if (country.isNullOrBlank()) lang else "$lang-$country"
        val supportedLanguages = setOf(
            "zh-CN", // 中文简体（中国大陆）
            "zh-TW", // 中文繁体（台湾）
            "zh-HK", // 中文繁体（香港）
            "en-US", // 英文（美国）
            "en-GB", // 英文（英国）
            "en-AU", // 英文（澳大利亚）
            "ja-JP", // 日文
            "ko-KR", // 韩文
            "de-DE", // 德文
            "fr-FR", // 法文
            "ru-RU"  // 俄文
        )
        
        return if (supportedLanguages.contains(languageCode) || supportedLanguages.contains(lang)) {
            TextToSpeech.LANG_AVAILABLE
        } else {
            TextToSpeech.LANG_NOT_SUPPORTED
        }
    }
}
