package com.wxh.ttsonline.function

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.util.Log
import android.widget.Toast
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.google.gson.Gson
import com.wxh.ttsonline.configuration.Dictionary
import com.wxh.ttsonline.configuration.LogTag
import com.wxh.ttsonline.configuration.TTSApplication
import com.wxh.ttsonline.dto.StartEnginePayload
import com.wxh.ttsonline.dto.TaskEnginePayload

class SpeechService(private val context: Context) :
    com.bytedance.speech.speechengine.SpeechEngine.SpeechListener {
    private val gson = Gson()

    private val speechEngine: SpeechEngine
        get() = (context.applicationContext as TTSApplication).speechEngine

    /**
     * 系统回调,用于向系统的TTS服务传递语音合成结果.全局唯一(系统TTS作业同一时间仅会存在一项)
     */
    private var callbackForSys: SynthesisCallback? = null

    /**
     * 音频播放器，用于播放语音合成结果
     */
    private var audioTrack: AudioTrack? = null

    /**
     * 音频播放器是否已初始化
     */
    private var isAudioTrackInitialized: Boolean = false

    /**
     * 当前语音合成服务状态
     */
    var currentState: Int = Dictionary.SpeechServiceState.PENDING

    /**
     * 最近一次语音合成服务错误信息
     */
    var lastErrorMsg: String = ""

    /**
     * 当前语音合成服务运行场景
     */
    var currentScene: String = Dictionary.SpeechServiceScene.REGULAR

    /**
     * 语音合成指定文本内容
     */
    fun tts(text: CharSequence?) {
        currentScene = Dictionary.SpeechServiceScene.DEMO
        currentState = Dictionary.SpeechServiceState.PROCESSING

        if (text.isNullOrBlank()) {
            currentState = Dictionary.SpeechServiceState.PENDING
            lastErrorMsg = "待合成文本为空"
            Log.e(LogTag.SPEECH_ERROR, lastErrorMsg)
            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            return
        }

        ttsInner(text, null, null)
    }

    /**
     * 执行来自系统的语音合成请求
     */
    fun tts(request: SynthesisRequest?, callback: SynthesisCallback?) {
        currentScene = Dictionary.SpeechServiceScene.REGULAR
        currentState = Dictionary.SpeechServiceState.PROCESSING

        if (callback == null) {
            currentState = Dictionary.SpeechServiceState.ERROR
            lastErrorMsg = "SynthesisCallback不能为空"
            throw IllegalArgumentException(lastErrorMsg)
        }
        callbackForSys = callback

        if (request == null || request.charSequenceText.isNullOrBlank()) {
            callback.done()
            currentState = Dictionary.SpeechServiceState.PENDING
            callbackForSys!!.done()
            return
        }

        val res = ttsInner(request.charSequenceText, request.pitch, request.speechRate)
        if (!res) {
            callbackForSys!!.done()
        }
    }

    /**
     * 语音合成
     */
    fun ttsInner(text: CharSequence?, loudnessRate: Int?, speechRate: Int?): Boolean {
        currentScene = Dictionary.SpeechServiceScene.REGULAR
        currentState = Dictionary.SpeechServiceState.PROCESSING

        if (text.isNullOrBlank()) {
            currentState = Dictionary.SpeechServiceState.PENDING
            return true
        }

        //初始化语音合成作业环境
        val defaultStartEnginePayload = speechEngine.getStartEnginePayload()
        var localLoudnessRate = defaultStartEnginePayload.reqParams.audioParams.loudnessRate
        var localSpeechRate = defaultStartEnginePayload.reqParams.audioParams.speechRate
        if (loudnessRate != null) {
            localLoudnessRate = degreeValueConvert(loudnessRate)
        }
        if (speechRate != null) {
            localSpeechRate = degreeValueConvert(speechRate)
        }
        val localStartEnginePayload: StartEnginePayload = defaultStartEnginePayload.copy(
            reqParams = defaultStartEnginePayload.reqParams.copy(
                audioParams = defaultStartEnginePayload.reqParams.audioParams.copy(
                    loudnessRate = localLoudnessRate, speechRate = localSpeechRate
                )
            )
        )
        var resCode = speechEngine.getEngine()
            .sendDirective(SpeechEngineDefines.DIRECTIVE_SYNC_STOP_ENGINE, "")
        if (resCode != SpeechEngineDefines.ERR_NO_ERROR) {
            lastErrorMsg = String.format("停止历史语音引擎发生错误: $resCode")
            Log.e(LogTag.SDK_ERROR, lastErrorMsg)
            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            currentState = Dictionary.SpeechServiceState.ERROR
            return false
        }
        resCode = speechEngine.getEngine().sendDirective(
            SpeechEngineDefines.DIRECTIVE_START_ENGINE, gson.toJson(localStartEnginePayload)
        )
        if (resCode != SpeechEngineDefines.ERR_NO_ERROR) {
            lastErrorMsg = String.format("启动语音引擎发生错误: $resCode")
            Log.e(LogTag.SDK_ERROR, lastErrorMsg)
            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            currentState = Dictionary.SpeechServiceState.ERROR
            return false
        }
        resCode = speechEngine.getEngine()
            .sendDirective(SpeechEngineDefines.DIRECTIVE_EVENT_START_SESSION, "")
        if (resCode != SpeechEngineDefines.ERR_NO_ERROR) {
            lastErrorMsg = String.format("启动语音合成会话发生错误: $resCode")
            Log.e(LogTag.SDK_ERROR, lastErrorMsg)
            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            currentState = Dictionary.SpeechServiceState.ERROR
            return false
        }

        //开始执行作业
        val taskPayload = TaskEnginePayload(TaskEnginePayload.ReqParams(text))
        resCode = speechEngine.getEngine().sendDirective(
            SpeechEngineDefines.DIRECTIVE_EVENT_TASK_REQUEST, gson.toJson(taskPayload)
        )
        if (resCode != SpeechEngineDefines.ERR_NO_ERROR) {
            lastErrorMsg = String.format("语音合成失败: $resCode")
            Log.e(LogTag.SDK_ERROR, lastErrorMsg)
            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            currentState = Dictionary.SpeechServiceState.ERROR
            return false
        }

        return true
    }

    /**
     * 停止当前语音合成作业
     */
    fun stopTTS() {
        if (callbackForSys != null) {
            callbackForSys = null
        }
        ttsStop()
    }

    /**
     * 语音合成阶段性完毕,向系统发起回调以响应当前合成结果
     * @param data 合成结果(ByteArray)
     * @param len 消息数据长度
     */
    fun ttsCallbackForSys(data: ByteArray?, len: Int) {
        if (callbackForSys == null) {
            lastErrorMsg = "SynthesisCallback为空"
            Log.e(LogTag.SPEECH_ERROR, lastErrorMsg)
            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            currentState = Dictionary.SpeechServiceState.ERROR
            return
        }
        if (len <= 0) {
            lastErrorMsg = String.format("远程语音合成服务响应了空内容, len: $len")
            Log.e(LogTag.SPEECH_INFO, lastErrorMsg)
            Toast.makeText(context, "合成内容为空", Toast.LENGTH_SHORT).show()
            currentState = Dictionary.SpeechServiceState.ERROR
        } else {
            callbackForSys!!.audioAvailable(data, 0, len)
            currentState = Dictionary.SpeechServiceState.PROCESSING_COMPLETED
        }
        callbackForSys!!.done()
        callbackForSys == null

        ttsStop()
    }

    /**
     * 语音合成阶段性完毕,调用扬声器发出朗读声音
     * @param data 合成结果(ByteArray)
     * @param len 消息数据长度
     */
    fun ttsCallback(data: ByteArray?, len: Int) {
        if (data == null || len <= 0) {
            lastErrorMsg = String.format("语音合成数据为空或长度无效, len: $len")
            Log.e(LogTag.SPEECH_ERROR, lastErrorMsg)
            Toast.makeText(context, "合成内容为空", Toast.LENGTH_SHORT).show()
            currentState = Dictionary.SpeechServiceState.ERROR
            return
        }

        try {
            // 初始化音频播放器（如果未初始化）
            if (!isAudioTrackInitialized) {
                initAudioTrack()
            }

            // 播放音频数据
            if (audioTrack != null && audioTrack!!.playState == AudioTrack.PLAYSTATE_PLAYING) {
                val writtenBytes = audioTrack!!.write(data, 0, len)
                if (writtenBytes != len) {
                    lastErrorMsg = String.format("音频数据写入不完整: 期望 $len, 实际 $writtenBytes")
                    Log.w(LogTag.SPEECH_INFO, lastErrorMsg)
                    Toast.makeText(context, "音频数据写入不完整", Toast.LENGTH_SHORT).show()
                }
            } else {
                lastErrorMsg = String.format("音频播放器未就绪或未播放状态")
                Log.e(LogTag.SPEECH_ERROR, lastErrorMsg)
                Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
                currentState = Dictionary.SpeechServiceState.ERROR
                return
            }

            currentState = Dictionary.SpeechServiceState.PROCESSING_COMPLETED
            Log.d(LogTag.SPEECH_INFO, "音频播放成功, 数据长度: $len")
        } catch (e: Exception) {
            lastErrorMsg = "音频播放失败: ${e.message}"
            Log.e(LogTag.SPEECH_ERROR, lastErrorMsg, e)
            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            currentState = Dictionary.SpeechServiceState.ERROR
        }

        ttsStop()
    }

    /**
     * 终止语音合成作业
     */
    fun ttsStop() {
        if (Dictionary.SpeechServiceScene.DEMO == currentScene) {
            // 释放音频播放器资源
            releaseAudioTrack()
            currentState = Dictionary.SpeechServiceState.PENDING
            return
        }

        var localState = currentState

        var resCode: Int?
        if (Dictionary.SpeechServiceState.PROCESSING == currentState) {
            resCode = speechEngine.getEngine()
                .sendDirective(SpeechEngineDefines.DIRECTIVE_EVENT_CANCLE_SESSION, "")
            if (resCode != SpeechEngineDefines.ERR_NO_ERROR) {
                lastErrorMsg = String.format("语音合成会话取消失败: $resCode")
                Log.e(LogTag.SDK_ERROR, lastErrorMsg)
                Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
                localState = Dictionary.SpeechServiceState.ERROR
            }
        }

        resCode = speechEngine.getEngine()
            .sendDirective(SpeechEngineDefines.DIRECTIVE_EVENT_FINISH_SESSION, "")
        if (resCode != SpeechEngineDefines.ERR_NO_ERROR) {
            lastErrorMsg = String.format("语音合成会话终止失败: $resCode")
            Log.e(LogTag.SDK_ERROR, lastErrorMsg)
            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            localState = Dictionary.SpeechServiceState.ERROR
        }

        currentState = if (localState != Dictionary.SpeechServiceState.ERROR) {
            Dictionary.SpeechServiceState.PENDING
        } else {
            localState
        }
    }

    /**
     * 语音引擎的回调
     * @param type 消息类型，在SpeechEngineDefines.java中定义
     * @param data 消息数据，应根据消息类型进行处理
     * @param len 消息数据长度
     */
    override fun onSpeechMessage(type: Int, data: ByteArray?, len: Int) {
        var stdData = ""
        if (data != null) {
            stdData = String(data)
        }
        when (type) {
            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_START -> Log.d(
                LogTag.SDK_INFO, "Callback: 引擎启动成功: data: $stdData"
            )

            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_STOP -> Log.d(
                LogTag.SDK_INFO, "Callback: 引擎关闭: data: $stdData"
            )

            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_ERROR -> Log.d(
                LogTag.SDK_ERROR, "Callback: 错误信息: $stdData"
            )

            SpeechEngineDefines.MESSAGE_TYPE_EVENT_TTS_SENTENCE_START -> Log.d(
                LogTag.SDK_INFO, "Callback: TTS_SENTENCE_START: $stdData"
            )

            SpeechEngineDefines.MESSAGE_TYPE_EVENT_TTS_SENTENCE_END -> Log.d(
                LogTag.SDK_INFO, "Callback: TTS_SENTENCE_END: $stdData"
            )

            SpeechEngineDefines.MESSAGE_TYPE_EVENT_TTS_RESPONSE -> Log.d(
                LogTag.SDK_INFO, "Callback: TTS_RESPONSE: data len ${stdData.length}"
            )

            SpeechEngineDefines.MESSAGE_TYPE_EVENT_TTS_ENDED -> Log.d(
                LogTag.SDK_INFO, "Callback: TTSEnded: $stdData"
            )

            SpeechEngineDefines.MESSAGE_TYPE_PLAYER_AUDIO_DATA -> Unit

            SpeechEngineDefines.MESSAGE_TYPE_PLAYER_START_PLAY_AUDIO -> Log.d(
                LogTag.SDK_INFO, "Callback: 播放开始: $stdData"
            )

            SpeechEngineDefines.MESSAGE_TYPE_PLAYER_FINISH_PLAY_AUDIO -> Log.d(
                LogTag.SDK_INFO, "Callback: 播放结束: $stdData"
            )

            else -> Unit
        }
    }

    /**
     * 初始化音频播放器
     */
    private fun initAudioTrack() {
        try {
            // 音频格式配置
            val sampleRate = 16000 // 采样率
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO // 单声道
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT // 16位PCM编码
            val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            // 创建音频属性
            val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()

            // 创建AudioTrack
            audioTrack = AudioTrack.Builder().setAudioAttributes(audioAttributes).setAudioFormat(
                    AudioFormat.Builder().setEncoding(audioFormat).setSampleRate(sampleRate)
                        .setChannelMask(channelConfig).build()
                ).setBufferSizeInBytes(bufferSize).setTransferMode(AudioTrack.MODE_STREAM).build()

            // 开始播放
            audioTrack!!.play()
            isAudioTrackInitialized = true

            Log.d(LogTag.SPEECH_INFO, "音频播放器初始化成功")

        } catch (e: Exception) {
            lastErrorMsg = "音频播放器初始化失败: ${e.message}"
            Log.e(LogTag.COMMON_ERROR, lastErrorMsg, e)
            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            isAudioTrackInitialized = false
            throw e
        }
    }

    /**
     * 释放音频播放器资源
     */
    private fun releaseAudioTrack() {
        try {
            if (audioTrack != null) {
                if (audioTrack!!.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack!!.stop()
                }
                audioTrack!!.release()
                audioTrack = null
            }
            isAudioTrackInitialized = false
            Log.d(LogTag.SPEECH_INFO, "音频播放器资源已释放")
        } catch (e: Exception) {
            lastErrorMsg = "释放音频播放器资源失败"
            Log.e(LogTag.SPEECH_ERROR, lastErrorMsg, e)
            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            currentState = Dictionary.SpeechServiceState.ERROR
        }
    }

    companion object {
        /**
         * 将安卓TTS场景下的程度数值参数转换为火山引擎语音合成服务的程度值
         * 安卓TTS程度值范围: 0 到 200, 默认为100
         * 语音合成服务程度值范围: -50 到 100, 默认0
         */
        fun degreeValueConvert(androidTTSDegreeValue: Int): Int {
            val clamped = androidTTSDegreeValue.coerceIn(0, 200)
            return ((clamped - 100) * 150 / 200)
        }
    }
}