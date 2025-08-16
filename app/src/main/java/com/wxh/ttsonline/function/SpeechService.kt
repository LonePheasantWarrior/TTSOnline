package com.wxh.ttsonline.function

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Handler
import android.os.Looper
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.util.Log
import android.widget.Toast
import com.bytedance.speech.da
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.wxh.ttsonline.configuration.Dictionary
import com.wxh.ttsonline.configuration.LogTag
import com.wxh.ttsonline.configuration.TTSApplication

class SpeechService(private val context: Context) :
    com.bytedance.speech.speechengine.SpeechEngine.SpeechListener {

    private val mainHandler = Handler(Looper.getMainLooper())

    val defaultText =
        "愿中国青年都摆脱冷气，只是向上走，不必听自暴自弃者流的话。能做事的做事，能发声的发声。有一分热，发一分光。就令萤火一般，也可以在黑暗里发一点光，不必等候炬火。此后如竟没有炬火：我便是唯一的光。"
    private val speechEngine: SpeechEngine
        get() = (context.applicationContext as TTSApplication).speechEngine

    /**
     * 系统回调,用于向系统的TTS服务传递语音合成结果.全局唯一(系统TTS作业同一时间仅会存在一项)
     */
    private var callbackForSys: SynthesisCallback? = null

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

        if (text.isNullOrBlank()) {
            currentState = Dictionary.SpeechServiceState.PENDING
            lastErrorMsg = "待合成文本为空,使用默认文本"
            Log.e(LogTag.SPEECH_ERROR, lastErrorMsg)
            mainHandler.post {
                Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            }
            ttsInner(defaultText, null, null)
        } else {
            if (text.length > 80) {
                lastErrorMsg = "待合成文本超长（80字符）"
                Log.w(LogTag.SPEECH_ERROR, lastErrorMsg)
                mainHandler.post {
                    Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
                }
            }
            ttsInner(text, null, null)
        }
    }

    /**
     * 执行来自系统的语音合成请求
     */
    fun tts(request: SynthesisRequest?, callback: SynthesisCallback?) {
        currentScene = Dictionary.SpeechServiceScene.REGULAR

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
        ttsInner(request.charSequenceText, request.pitch, request.speechRate)

        // 等待处理结果(currentState),得到结果后根据状态执行相应动作
        // 如果currentState为Dictionary.SpeechServiceState.PROCESSING,则持续等待(0.5秒检查一次)
        // 如果currentState为非Dictionary.SpeechServiceState.PROCESSING,则执行callbackForSys!!.done()结束本次TTS作业
        waitForProcessingResult()
    }

    /**
     * 等待语音合成处理结果（同步阻塞）
     */
    private fun waitForProcessingResult() {
        val checkInterval = 500L // 0.5秒检查一次
        val maxWaitTime = 30000L // 最大等待30秒，避免无限等待
        val startTime = System.currentTimeMillis()
        
        Log.d(LogTag.SDK_INFO, "开始等待语音合成处理结果，当前状态: $currentState")
        
        // 同步等待处理结果
        while (true) {
            // 检查是否超时
            if (System.currentTimeMillis() - startTime > maxWaitTime) {
                Log.w(LogTag.SPEECH_ERROR, "等待语音合成结果超时(${maxWaitTime}ms)")
                callbackForSys?.done()
                callbackForSys = null
                currentState = Dictionary.SpeechServiceState.PENDING
                break
            }
            
            when (currentState) {
                Dictionary.SpeechServiceState.PROCESSING -> {
                    // 仍在处理中，继续等待
                    try {
                        Thread.sleep(checkInterval)
                    } catch (e: InterruptedException) {
                        Log.w(LogTag.SPEECH_ERROR, "等待语音合成结果时被中断")
                        callbackForSys?.done()
                        callbackForSys = null
                        currentState = Dictionary.SpeechServiceState.PENDING
                        break
                    }
                }
                Dictionary.SpeechServiceState.PROCESSING_COMPLETED -> {
                    // 处理完成，调用done()结束本次TTS作业
                    Log.d(LogTag.SDK_INFO, "语音合成处理完成")
                    callbackForSys?.done()
                    callbackForSys = null
                    currentState = Dictionary.SpeechServiceState.PENDING
                    break
                }
                Dictionary.SpeechServiceState.ERROR -> {
                    // 发生错误，调用done()结束本次TTS作业
                    Log.e(LogTag.SPEECH_ERROR, "语音合成发生错误: $lastErrorMsg")
                    callbackForSys?.done()
                    callbackForSys = null
                    currentState = Dictionary.SpeechServiceState.PENDING
                    break
                }
                Dictionary.SpeechServiceState.PENDING -> {
                    // 已挂起，调用done()结束本次TTS作业
                    Log.d(LogTag.SDK_INFO, "语音合成状态为挂起")
                    callbackForSys?.done()
                    callbackForSys = null
                    break
                }
                else -> {
                    // 未知状态，调用done()结束本次TTS作业
                    Log.w(LogTag.SPEECH_ERROR, "未知的语音合成状态: $currentState")
                    callbackForSys?.done()
                    callbackForSys = null
                    currentState = Dictionary.SpeechServiceState.PENDING
                    break
                }
            }
        }
        
        Log.d(LogTag.SPEECH_INFO, "语音合成等待结束，最终状态(0-挂起,1-处理器中,2-已完成,3-错误): $currentState")
    }

    /**
     * 语音合成
     * @param text 待合成语音的文本内容
     * @param pitchForSys 音高(来自系统的TTS参数)
     * @param speechRateForSys 语速(来自系统的TTS参数)
     */
    fun ttsInner(text: CharSequence?, pitchForSys: Int?, speechRateForSys: Int?): Boolean {
        if (currentState == Dictionary.SpeechServiceState.PROCESSING) {
            lastErrorMsg = String.format("语音合成引擎正忙")
            Log.e(LogTag.COMMON_ERROR, lastErrorMsg)
            mainHandler.post {
                Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            }
            return false
        }

        currentState = Dictionary.SpeechServiceState.PROCESSING

        if (text.isNullOrBlank()) {
            currentState = Dictionary.SpeechServiceState.PENDING
            return true
        }

        val preferences = context.getSharedPreferences("TTSOnlineSettings", MODE_PRIVATE)
        val appId = preferences.getString("appId", null)
        val token = preferences.getString(Dictionary.PreferenceKey.TOKEN, null)
        val selectedSpeakerType =
            preferences.getString(Dictionary.PreferenceKey.SELECTED_SPEAKER_TYPE, null)
        if (appId.isNullOrBlank() || token.isNullOrBlank() || selectedSpeakerType.isNullOrBlank()) {
            lastErrorMsg = String.format("语音合成引擎配置不完整")
            Log.e(LogTag.COMMON_ERROR, lastErrorMsg)
            mainHandler.post {
                Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            }
            return false
        }

        val pitchRate: Double = degreeValueConvert(pitchForSys)
        val speechRate: Double = degreeValueConvert(speechRateForSys)

        speechEngine.destroy()
        speechEngine.initEngine(
            appId,
            token,
            selectedSpeakerType,
            text as String,
            pitchRate,
            null,
            speechRate,
            currentScene == Dictionary.SpeechServiceScene.DEMO,
            this
        )

        if (!speechEngine.isInitialized) {
            lastErrorMsg = String.format("语音合成引擎未初始化")
            Log.e(LogTag.SDK_ERROR, lastErrorMsg)
            mainHandler.post {
                Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            }
            currentState = Dictionary.SpeechServiceState.ERROR
            return false
        }

        //关闭引擎
        //先调用同步停止，避免SDK内部异步线程带来的问题
        var resCode = speechEngine.getEngine()
            .sendDirective(SpeechEngineDefines.DIRECTIVE_SYNC_STOP_ENGINE, "")
        if (resCode != SpeechEngineDefines.ERR_NO_ERROR) {
            lastErrorMsg = String.format("停止历史语音引擎发生错误: $resCode")
            Log.e(LogTag.SDK_ERROR, lastErrorMsg)
            mainHandler.post {
                Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            }
            currentState = Dictionary.SpeechServiceState.ERROR
            return false
        }
        //启动引擎
        //单次合成场景：启动一次合成，合成、播放完后引擎停止，再次合成需要重新启动引擎
        //连续合成场景：启动引擎，触发合成需要单独调用合成指令
        resCode = speechEngine.getEngine()
            .sendDirective(SpeechEngineDefines.DIRECTIVE_START_ENGINE, "")
        if (resCode != SpeechEngineDefines.ERR_NO_ERROR) {
            lastErrorMsg = String.format("启动语音引擎发生错误: $resCode")
            Log.e(LogTag.SDK_ERROR, lastErrorMsg)
            mainHandler.post {
                Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            }
            currentState = Dictionary.SpeechServiceState.ERROR
            return false
        }
        return true
    }

    /**
     * 语音合成阶段性完毕,向系统发起回调以响应当前合成结果
     * @param data 合成结果(ByteArray)
     * @param dataLength 消息数据长度
     * @param isFinal 是否播放完毕
     */
    fun ttsCallback(data: ByteArray?, dataLength: Int, isFinal: Boolean) {
        if (callbackForSys == null) {
            lastErrorMsg = "SynthesisCallback为空"
            Log.e(LogTag.SPEECH_ERROR, lastErrorMsg)
            mainHandler.post {
                Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            }
            currentState = Dictionary.SpeechServiceState.ERROR
            return
        }

        if (data != null && data.isNotEmpty() && dataLength > 0) {
            Log.d(LogTag.SPEECH_INFO, "待播放媒体数据实际长度: ${data.size}, 标记长度: $dataLength")
            
            // Android TTS系统对单次传递的音频数据大小有限制，需要分块处理
            val maxChunkSize = 8192 // 8KB限制，避免缓冲区过大错误
            var offset = 0
            
            while (offset < dataLength) {
                val chunkSize = minOf(maxChunkSize, dataLength - offset)
                try {
                    callbackForSys!!.audioAvailable(data, offset, chunkSize)
                    offset += chunkSize
                } catch (e: IllegalArgumentException) {
                    Log.e(LogTag.SPEECH_ERROR, "audioAvailable调用失败: ${e.message}")
                    lastErrorMsg = "音频数据处理失败: ${e.message}"
                    mainHandler.post {
                        Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
                    }
                    currentState = Dictionary.SpeechServiceState.ERROR
                    return
                }
            }
        }

        if (isFinal) {
            Log.d(LogTag.SPEECH_INFO, "本次TTS作业完成")
            currentState = Dictionary.SpeechServiceState.PROCESSING_COMPLETED
        }
    }

    /**
     * 语音引擎的回调
     * @param type 消息类型，在SpeechEngineDefines.java中定义
     * @param data 消息数据，应根据消息类型进行处理
     * @param len 消息数据长度
     */
    override fun onSpeechMessage(type: Int, data: ByteArray?, len: Int) {
        when (type) {
            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_START -> {
                Log.d(LogTag.SDK_INFO, "SpeechMessage: 引擎启动成功")

                //发起语音合成请求
                val resCode = speechEngine.getEngine().sendDirective(
                    SpeechEngineDefines.DIRECTIVE_SYNTHESIS, ""
                )
                if (resCode != SpeechEngineDefines.ERR_NO_ERROR) {
                    if (SpeechEngineDefines.ERR_SYNTHESIS_IS_BUSY == resCode) {
                        Log.i(LogTag.SDK_INFO, "忽略 [${SpeechEngineDefines.ERR_SYNTHESIS_IS_BUSY}] 错误，视为处理中...")
                    } else {
                        lastErrorMsg = String.format("发起语音合成请求失败: $resCode")
                        Log.e(LogTag.SDK_ERROR, lastErrorMsg)
                        mainHandler.post {
                            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
                        }
                        currentState = Dictionary.SpeechServiceState.ERROR
                    }
                }
            }

            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_STOP -> Log.d(
                LogTag.SDK_INFO, "SpeechMessage: 引擎已停止"
            )

            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_ERROR -> {
                var errorData: String? = ""
                if (data != null && data.isNotEmpty()) {
                    errorData = String(data)
                }
                Log.e(
                    LogTag.SDK_ERROR, "SpeechMessage: 引擎发生错误: $errorData"
                )
            }

            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_LOG -> {
                if (data != null) {
                    val dataStr = String(data)
                    Log.d(LogTag.SDK_INFO, "SpeechMessage: 引擎日志: $dataStr")
                }
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_SYNTHESIS_BEGIN -> {
                Log.d(LogTag.SDK_INFO, "SpeechMessage: 语音合成已开始")
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_SYNTHESIS_END -> Log.d(
                LogTag.SDK_INFO, "SpeechMessage: 语音合成已结束"
            )

            SpeechEngineDefines.MESSAGE_TYPE_TTS_AUDIO_DATA -> {
                Log.d(LogTag.SDK_INFO, "SpeechMessage: 收到音频数据,回调数据长度: $len")
                if (currentScene == Dictionary.SpeechServiceScene.REGULAR) {
                    if (data != null && data.isNotEmpty() && len > 0) {
                        ttsCallback(data, len, false)
                    }
                }
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_AUDIO_DATA_END -> {
                Log.d(LogTag.SDK_INFO, "SpeechMessage: 音频数据接收完毕")
                if (currentScene == Dictionary.SpeechServiceScene.REGULAR) {
                    ttsCallback(null, 0, true)
                }
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_PLAYBACK_PROGRESS -> {
                var dataStr = "未知"
                if (data != null && data.isNotEmpty()) {
                    dataStr = String(data)
                }
                Log.d(LogTag.SDK_INFO, "SpeechMessage: 播放进度: $dataStr")
            }

            SpeechEngineDefines.MESSAGE_TYPE_TTS_START_PLAYING -> Log.d(
                LogTag.SDK_INFO, "SpeechMessage: 音频播放开始"
            )

            SpeechEngineDefines.MESSAGE_TYPE_TTS_FINISH_PLAYING -> {
                Log.d(LogTag.SDK_INFO, "SpeechMessage: 音频播放完毕")
            }

            else -> {
                val warnMsg = String.format("未处理的语音合成回调: $type")
                Log.i(LogTag.SDK_INFO, warnMsg)
            }
        }
    }

    fun ttsStop() {
        speechEngine.destroy()
        callbackForSys = null
        currentState = Dictionary.SpeechServiceState.PENDING
    }

    companion object {
        /**
         * 将安卓TTS场景下的程度数值参数转换为火山引擎语音合成服务的程度值
         * 安卓TTS程度值范围: 0 到 200, 默认为100
         * 语音合成服务程度值范围: 0.0 到 2.0, 默认1.0
         */
        fun degreeValueConvert(androidTTSDegreeValue: Int?): Double {
            if (androidTTSDegreeValue == null) {
                return 1.0
            }
            // 确保输入值在有效范围内
            val clampedValue = androidTTSDegreeValue.coerceIn(0, 200)
            // 进行线性转换：安卓TTS值 / 100.0 = 语音服务值
            return clampedValue / 100.0
        }
    }
}