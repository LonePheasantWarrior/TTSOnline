package com.wxh.ttsonline.dto

import com.google.gson.annotations.SerializedName

/**
 * TTS语音合成请求参数DTO
 */
data class StartEnginePayload(@SerializedName("req_params") val reqParams: ReqParams) {

    /**
     * 请求参数
     */
    data class ReqParams(@SerializedName("speaker") val speaker: String, @SerializedName("audio_params") val audioParams: AudioParams) {
        /**
         * 音频参数
         * @param loudnessRate 语速，取值范围[-50,100]，100代表2.0倍速，-50代表0.5倍数
         * @param speechRate 音量，取值范围[-50,100]，100代表2.0倍音量，-50代表0.5倍音量（mix音色暂不支持）
         */
        data class AudioParams(@SerializedName("loudness_rate") val loudnessRate: Int, @SerializedName("speech_rate") val speechRate: Int)
    }
}