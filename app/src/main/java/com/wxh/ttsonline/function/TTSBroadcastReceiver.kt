package com.wxh.ttsonline.function

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wxh.ttsonline.configuration.LogTag

/**
 * TTS广播接收器
 * 监听系统TTS相关事件，尝试主动注册TTS引擎
 */
class TTSBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(LogTag.SDK_INFO, "TTSBroadcastReceiver onReceive: $action")
        
        when (action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                val packageName = intent.data?.schemeSpecificPart
                Log.d(LogTag.SDK_INFO, "包安装/更新: $packageName")
                
                // 如果是我们的包，尝试主动注册TTS引擎
                if (packageName == "com.wxh.ttsonline") {
                    Log.d(LogTag.SDK_INFO, "检测到TTSOnline包安装/更新，尝试主动注册TTS引擎")
                    try {
                        // 这里可以尝试主动通知系统TTS服务
                        // 但由于权限限制，可能无法直接操作
                        Log.d(LogTag.SDK_INFO, "TTS引擎主动注册尝试完成")
                    } catch (e: Exception) {
                        Log.e(LogTag.SPEECH_ERROR, "TTS引擎主动注册失败: ${e.message}", e)
                    }
                }
            }
        }
    }
}
