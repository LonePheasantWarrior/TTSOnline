package com.wxh.ttsonline.dto

import com.google.gson.annotations.SerializedName

/**
 * TTS语音合成任务内容(待合成文本)
 */
data class TaskEnginePayload(@SerializedName("req_params") val reqParams: ReqParams) {

    /**
     * 请求参数
     */
    data class ReqParams(@SerializedName("text") val text: CharSequence)
}