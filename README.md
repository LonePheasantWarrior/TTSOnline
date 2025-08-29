# TTSOnline
> An Android TTS application based on an online AI synthesis large model implemented using "Doubao Voice" of Volcengine.
> 基于来自字节跳动火山引擎大模型语音合成服务的在线安卓TTS应用

## Related Documents
[豆包语音-SDK接入文档-离在线语音合成SDK-Android](https://www.volcengine.com/docs/6561/79832)

## Current Status
目前软件完成了主要UI的编码,可以实现输入来自火山引擎语音合成服务的`Access Token`和`Secret Key`来访问语音合成服务输出演示声音  
但是在正式工作环境下却无法播放内容,`com.wxh.ttsonline.function.SpeechService.ttsCallback`函数功能工作不正常

## HELP!
希望有兴趣的大佬可以帮一把,让我的第一款安卓应用能够正常工作,不胜感激!

## ETC
一直在搞Java服务端开发,这是第一次用Kotlin来写工程,同样也是第一次写Android应用  
中间遇到不少坑,比如无法将软件注册为系统TTS应用(“文字转语音输出”设置页中没有出现本软件),对`android.speech.tts.TextToSpeechService`不熟悉,后来才知道`android.speech.tts.TextToSpeechService.onSynthesizeText`接口的实现逻辑**必须在一个线程中**等等 