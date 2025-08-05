package com.wxh.ttsonline.function

import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import com.wxh.ttsonline.configuration.Dictionary
import com.wxh.ttsonline.configuration.TTSApplication

/**
 * 语音合成服务
 */
class TTSService : TextToSpeechService() {
    private val speechService: SpeechService
        get() = (applicationContext as TTSApplication).speechService

    override fun onGetLanguage(): Array<out String?>? {
        return Dictionary.SpeechEngine.SUPPORTED_LANGUAGES.map { it.first }.distinct().toTypedArray()
    }

    override fun onIsLanguageAvailable(
        lang: String?, country: String?, variant: String?
    ): Int {
        if (lang == null) return TextToSpeech.LANG_NOT_SUPPORTED
        val match = Dictionary.SpeechEngine.SUPPORTED_LANGUAGES.find {
            it.first.equals(lang, ignoreCase = true) &&
            (country == null || it.second.equals(country, ignoreCase = true)) &&
            (variant == null)
        }
        return when {
            match != null && country != null && variant != null -> TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
            match != null && country != null -> TextToSpeech.LANG_COUNTRY_AVAILABLE
            match != null -> TextToSpeech.LANG_AVAILABLE
            else -> TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onLoadLanguage(
        lang: String?, country: String?, variant: String?
    ): Int {
        return onIsLanguageAvailable(lang, country, variant)
    }

    override fun onStop() {
        speechService.ttsStop()
    }

    override fun onSynthesizeText(
        request: SynthesisRequest?, callback: SynthesisCallback?
    ) {
        speechService.tts(request, callback)
    }

    // 注意：某些方法在较新的Android版本中可能不可用
    // 我们只实现必要的方法来确保TTS引擎能够正常工作
    override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String? {
        // 返回默认语音名称
        return "zh-CN"
    }

    override fun onGetVoices(): MutableList<android.speech.tts.Voice>? {
        // 返回支持的语音列表
        val voices = mutableListOf<android.speech.tts.Voice>()
        
        Dictionary.SpeechEngine.SUPPORTED_LANGUAGES.forEach { (lang, country, _) ->
            val locale = java.util.Locale.Builder()
                .setLanguage(lang)
                .setRegion(country)
                .build()
            val voice = android.speech.tts.Voice(
                "TTSOnline_${lang}_${country}",
                locale,
                android.speech.tts.Voice.QUALITY_NORMAL,
                android.speech.tts.Voice.LATENCY_NORMAL,
                false, // requiresNetwork
                setOf() // features
            )
            voices.add(voice)
        }
        return voices
    }



}