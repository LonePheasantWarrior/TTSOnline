package com.wxh.ttsonline.function

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.wxh.ttsonline.configuration.LogTag

/**
 * 音频播放器
 */
class AudioPlayer {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    /**
     * 获取兼容的声道配置
     */
    private fun getCompatibleChannelConfig(channelConfig: Int): Int {
        // 检查设备是否支持指定的声道配置
        val audioManager = android.content.Context.AUDIO_SERVICE as? android.media.AudioManager
        return if (audioManager != null) {
            try {
                // 尝试使用原始声道配置
                val minBufferSize = AudioTrack.getMinBufferSize(44100, channelConfig, AudioFormat.ENCODING_PCM_16BIT)
                if (minBufferSize > 0) {
                    channelConfig
                } else {
                    // 如果不支持，使用默认声道配置
                    Log.w(LogTag.SPEECH_INFO, "设备不支持声道配置$channelConfig，使用默认配置")
                    AudioFormat.CHANNEL_OUT_DEFAULT
                }
            } catch (e: Exception) {
                Log.w(LogTag.SPEECH_INFO, "检查声道配置时出错: ${e.message}", e)
                AudioFormat.CHANNEL_OUT_DEFAULT
            }
        } else {
            channelConfig
        }
    }

    /**
     * 播放音频数据
     * @param audioData 要播放的音频数据
     * @param sampleRate 采样率，默认44100Hz
     * @param channelConfig 声道配置，默认单声道
     * @param audioFormat 音频格式，默认16位PCM
     */
    fun play(
        audioData: ByteArray,
        sampleRate: Int = 44100,
        channelConfig: Int = AudioFormat.CHANNEL_OUT_MONO,
        audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
    ) {
        // 检查音频数据长度
        if (audioData.size < 1024) {
            Log.w(LogTag.SPEECH_INFO, "音频数据可能太短(${audioData.size}字节)，可能导致播放问题")
        }
        // 停止当前播放（如果正在播放）
        stop()

        try {
            // 获取兼容的声道配置
            val compatibleChannelConfig = getCompatibleChannelConfig(channelConfig)
            Log.d(LogTag.SPEECH_INFO, "使用声道配置: $compatibleChannelConfig")

            // 计算最小缓冲区大小
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, compatibleChannelConfig, audioFormat)
            // 创建AudioTrack实例
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val trackAudioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(compatibleChannelConfig)
                .setEncoding(audioFormat)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(trackAudioFormat)
                .setBufferSizeInBytes(Math.max(minBufferSize, audioData.size))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            // 写入音频数据
            audioTrack?.write(audioData, 0, audioData.size)
            isPlaying = true

            // 设置播放完成监听器
            audioTrack?.setPlaybackPositionUpdateListener(object :
                AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack?) {
                    // 播放完成，自动停止
                    Log.d(LogTag.SPEECH_INFO, "音频播放完成，自动停止")
                    stop()
                }

                override fun onPeriodicNotification(track: AudioTrack?) {
                    // 不需要处理
                }
            })

            // 设置标记位置为音频数据末尾
            audioTrack?.setNotificationMarkerPosition(audioData.size / 2) // 假设16位PCM，每个样本2字节

            // 开始播放
            audioTrack?.play()
            Log.d(LogTag.SPEECH_INFO, "开始播放音频，长度: ${audioData.size}字节")
            Log.d(LogTag.SPEECH_INFO, "音频配置: 采样率=${sampleRate}Hz, 声道=${channelConfig}, 格式=${audioFormat}")

        } catch (e: Exception) {
            Log.e(LogTag.SPEECH_INFO, "播放音频失败: ${e.message}", e)
            stop()
        }
    }

    /**
     * 停止播放并释放资源
     */
    fun stop() {
        if (isPlaying && audioTrack != null) {
            try {
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
                isPlaying = false
                Log.d(LogTag.SPEECH_INFO, "音频播放已停止，资源已释放")
            } catch (e: Exception) {
                Log.e(LogTag.SPEECH_INFO, "停止音频播放失败: ${e.message}", e)
            }
        }
    }
}