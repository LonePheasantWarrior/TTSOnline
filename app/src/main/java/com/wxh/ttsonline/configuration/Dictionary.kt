package com.wxh.ttsonline.configuration

/**
 * 全局字典类
 */
class Dictionary {
    /**
     * 配置项
     */
    object PreferenceKey {
        const val APP_ID = "appId"
        const val TOKEN = "token"
        const val SELECTED_SCENE = "selectedScene"
        const val SELECTED_SPEAKER_TYPE = "selectedSpeakerType"
    }


    /**
     * 语音合成服务状态
     */
    object SpeechServiceState {
        /**
         * 挂起
         */
        const val PENDING = 0

        /**
         * 语音合成中
         */
        const val PROCESSING = 1

        /**
         * 语音已合成
         */
        const val PROCESSING_COMPLETED = 2

        /**
         * 发生错误
         */
        const val ERROR = 3
    }

    /**
     * 语音合成服务运行场景
     */
    object SpeechServiceScene {
        /**
         * 演示场景
         */
        const val DEMO = "demo"

        /**
         * 常规场景
         */
        const val REGULAR = "regular"
    }

    /**
     * 火山方舟-语音合成引擎相关
     */
    object SpeechEngine {
        /**
         * 语音合成引擎名称
         */
        const val ENGINE_NAME = "TTSOnline"

        /**
         * 语音合成服务访问地址
         */
        const val SERVER_ADDRESS = "wss://openspeech.bytedance.com"

        /**
         * 语音合成服务接口地址
         */
        const val TTS_URL = "/api/v1/tts/ws_binary"

        /**
         * 语音合成服务Cluster ID
         */
        const val CLUSTER = "volcano_tts"

        /**
         * 语音合成服务资源ID
         */
        const val RES_ID = "volc.service_type.10029"

        /**
         * 发音人（非语音克隆场景使用other）
         */
        const val VOICE = "other"

        /**
         * 支持的语言列表（语言代码, 国家代码, 变体）
         * 例如: Pair("zh", "CN") 表示简体中文
         */
        val SUPPORTED_LANGUAGES = listOf(
            Triple("zh", "CN", null),    // 汉语（简体中文）
            Triple("zh", "TW", null),    // 汉语（繁体中文-台湾）
            Triple("zh", "HK", null),    // 汉语（繁体中文-香港）
        )
    }
}