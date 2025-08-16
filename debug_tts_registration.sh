#!/bin/bash

echo "=== TTS引擎注册调试脚本 ==="
echo ""

# 检查设备连接
echo "1. 检查设备连接..."
if ! adb devices | grep -q "device$"; then
    echo "错误: 没有找到已连接的Android设备"
    exit 1
fi
echo "✓ 设备已连接"
echo ""

# 检查应用安装状态
echo "2. 检查应用安装状态..."
if adb shell pm list packages | grep -q "com.wxh.ttsonline"; then
    echo "✓ 应用已安装"
else
    echo "错误: 应用未安装"
    exit 1
fi
echo ""

# 检查TTS服务注册
echo "3. 检查TTS服务注册..."
echo "已注册的服务："
adb shell pm list services | grep -i tts
echo ""

# 检查应用权限
echo "4. 检查应用权限..."
echo "应用权限列表："
adb shell dumpsys package com.wxh.ttsonline | grep -A 20 "requested permissions"
echo ""

# 检查TTS引擎列表
echo "5. 检查系统TTS引擎..."
echo "系统TTS引擎："
adb shell dumpsys texttospeech | grep -A 10 "Available engines"
echo ""

# 检查应用组件
echo "6. 检查应用组件..."
echo "应用组件列表："
adb shell dumpsys package com.wxh.ttsonline | grep -A 10 "Activity Resolver Table"
echo ""

# 检查TTS服务状态
echo "7. 检查TTS服务状态..."
echo "TTS服务状态："
adb shell dumpsys texttospeech | grep -A 5 "com.wxh.ttsonline"
echo ""

# 尝试启动TTS服务
echo "8. 尝试启动TTS服务..."
adb shell am startservice -n com.wxh.ttsonline/.function.TTSEngineService
echo ""

# 检查服务进程
echo "9. 检查服务进程..."
echo "TTS服务进程："
adb shell ps | grep -i tts
echo ""

# 检查日志
echo "10. 检查TTS相关日志..."
echo "最近的TTS日志："
adb logcat -d | grep -E "(TTSEngineService|SpeechService|TTSOnline|TTS)" | tail -20
echo ""

echo "=== 调试完成 ==="
echo ""
echo "如果TTS引擎仍未显示，可能的原因："
echo "1. 需要系统签名权限"
echo "2. 需要root权限"
echo "3. 设备不支持第三方TTS引擎"
echo "4. 需要特定的Android版本"
echo ""
echo "建议尝试："
echo "1. 重启设备"
echo "2. 清除系统TTS缓存"
echo "3. 检查设备TTS设置"
echo "4. 查看完整日志：adb logcat | grep -E '(TTSEngineService|SpeechService|TTSOnline)'"
