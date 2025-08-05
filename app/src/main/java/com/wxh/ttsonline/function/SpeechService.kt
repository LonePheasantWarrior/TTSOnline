package com.wxh.ttsonline.function

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.util.Log
import android.widget.Toast
import com.bytedance.speech.speechengine.SpeechEngineDefines
import com.wxh.ttsonline.configuration.Dictionary
import com.wxh.ttsonline.configuration.LogTag
import com.wxh.ttsonline.configuration.TTSApplication
import java.io.ByteArrayOutputStream

class SpeechService(private val context: Context) :
    com.bytedance.speech.speechengine.SpeechEngine.SpeechListener {

    private val speechEngine: SpeechEngine
        get() = (context.applicationContext as TTSApplication).speechEngine
    private val audioPlayer: AudioPlayer
        get() = (context.applicationContext as TTSApplication).audioPlayer

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
     * @param text 待合成语音的文本内容
     * @param pitchForSys 音高(来自系统的TTS参数)
     * @param speechRateForSys 语速(来自系统的TTS参数)
     */
    fun ttsInner(text: CharSequence?, pitchForSys: Int?, speechRateForSys: Int?): Boolean {
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
            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            return false
        }

        val pitchRate: Double = degreeValueConvert(pitchForSys)
        val speechRate: Double = degreeValueConvert(speechRateForSys)

        speechEngine.destroy()
        speechEngine.initEngine(appId, token, selectedSpeakerType, text as String, pitchRate, null, speechRate)

        if (!speechEngine.isInitialized) {
            lastErrorMsg = String.format("语音合成引擎未初始化")
            Log.e(LogTag.SDK_ERROR, lastErrorMsg)
            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            currentState = Dictionary.SpeechServiceState.ERROR
            return false
        }

        //关闭引擎
        //先调用同步停止，避免SDK内部异步线程带来的问题
        //取消请求，关闭引擎。单次合成场景下正常结束不需要调用，引擎内部会自动结束；连续合成场景下当不再使用合成功能时应主动调用
        var resCode = speechEngine.getEngine()
            .sendDirective(SpeechEngineDefines.DIRECTIVE_SYNC_STOP_ENGINE, "")
        if (resCode != SpeechEngineDefines.ERR_NO_ERROR) {
            lastErrorMsg = String.format("停止历史语音引擎发生错误: $resCode")
            Log.e(LogTag.SDK_ERROR, lastErrorMsg)
            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            currentState = Dictionary.SpeechServiceState.ERROR
            return false
        }
        //建连指令用于在发送合成请求之前建立网络连接，可以在语音交互场景下减少在线合成的端到端延时。该指令需要在启动引擎之前调用
        resCode = speechEngine.getEngine()
            .sendDirective(SpeechEngineDefines.DIRECTIVE_CREATE_CONNECTION, "")
        if (resCode != SpeechEngineDefines.ERR_NO_ERROR) {
            lastErrorMsg = String.format("语音引擎网络连接错误: $resCode")
            Log.e(LogTag.SDK_ERROR, lastErrorMsg)
            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            currentState = Dictionary.SpeechServiceState.ERROR
            return false
        }
        //启动引擎
        //单次合成场景：启动一次合成，合成、播放完后引擎停止，再次合成需要重新启动引擎
        //连续合成场景：启动引擎，触发合成需要单独调用合成指令
        resCode =
            speechEngine.getEngine().sendDirective(SpeechEngineDefines.DIRECTIVE_START_ENGINE, "")
        if (resCode != SpeechEngineDefines.ERR_NO_ERROR) {
            lastErrorMsg = String.format("启动语音引擎发生错误: $resCode")
            Log.e(LogTag.SDK_ERROR, lastErrorMsg)
            Toast.makeText(context, lastErrorMsg, Toast.LENGTH_SHORT).show()
            currentState = Dictionary.SpeechServiceState.ERROR
            return false
        }
        //开始执行作业
        resCode = speechEngine.getEngine().sendDirective(
            SpeechEngineDefines.DIRECTIVE_SYNTHESIS, ""
        )
        if (resCode != SpeechEngineDefines.ERR_NO_ERROR) {
            lastErrorMsg = String.format("发起语音合成请求失败: $resCode")
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
     */
    fun ttsCallback(data: ByteArray?) {
        if (data == null || data.isEmpty()) {
            Log.w(LogTag.SPEECH_INFO, "接收到空的音频数据，跳过播放")
            return
        }
        audioPlayer.play(data)
        ttsStop()
    }

    /**
     * 终止语音合成作业
     */
    fun ttsStop() {
        if (Dictionary.SpeechServiceScene.DEMO == currentScene) {
            // 释放音频播放器资源
            audioPlayer.stop()
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
        when (type) {
            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_START -> Log.d(
                LogTag.SDK_INFO, "SpeechMessage: 引擎启动成功"
            )

            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_STOP -> Log.d(
                LogTag.SDK_INFO, "SpeechMessage: 引擎已停止"
            )

            SpeechEngineDefines.MESSAGE_TYPE_ENGINE_ERROR -> Log.e(
                LogTag.SDK_ERROR, "SpeechMessage: 引擎发生错误"
            )

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
                Log.d(LogTag.SDK_INFO, "SpeechMessage: 收到音频数据")
                if (Dictionary.SpeechServiceScene.DEMO == currentScene) {
                    ttsCallback(data)
                } else if (Dictionary.SpeechServiceScene.REGULAR == currentScene) {
                    ttsCallbackForSys(data, data!!.size)
                } else {
                    throw RuntimeException("不受支持的语音合成工作场景")
                }
            }

            else -> {
                val warnMsg = String.format("未处理的语音合成回调: $type")
                Log.i(LogTag.SDK_INFO, warnMsg)
            }
        }
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