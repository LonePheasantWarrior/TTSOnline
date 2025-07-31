package com.wxh.ttsonline.dto

/**
 * StartEnginePayloadDTO的构建器类
 * 提供便捷的方法来设置TTS语音合成请求参数
 */
class StartEnginePayloadBuilder {
    private var speaker: String = ""
    private var loudnessRate: Int = 0
    private var speechRate: Int = 0

    /**
     * 设置说话人(默认“清新女声”)
     */
    fun setSpeaker(speaker: String): StartEnginePayloadBuilder {
        this.speaker = speaker
        return this
    }

    /**
     * 设置音量(默认50)
     */
    fun setLoudnessRate(loudnessRate: Int): StartEnginePayloadBuilder {
        this.loudnessRate = loudnessRate
        return this
    }

    /**
     * 设置朗读速度(默认50)
     */
    fun setSpeechRate(speechRate: Int): StartEnginePayloadBuilder {
        this.speechRate = speechRate
        return this
    }

    /**
     * 构建StartEnginePayloadDTO对象
     */
    fun build(): StartEnginePayload {
        return StartEnginePayload(
            reqParams = StartEnginePayload.ReqParams(
                speaker = speaker,
                audioParams = StartEnginePayload.ReqParams.AudioParams(
                    loudnessRate = loudnessRate, speechRate = speechRate
                )
            )
        )
    }
} 