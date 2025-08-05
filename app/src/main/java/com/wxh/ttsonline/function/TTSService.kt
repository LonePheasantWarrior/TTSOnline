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

}