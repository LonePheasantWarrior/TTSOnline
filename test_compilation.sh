#!/bin/bash

echo "=== TTS引擎编译测试脚本 ==="
echo ""

# 清理之前的构建
echo "1. 清理之前的构建..."
./gradlew clean
if [ $? -ne 0 ]; then
    echo "错误: 清理构建失败"
    exit 1
fi
echo "✓ 清理完成"
echo ""

# 编译应用
echo "2. 编译应用..."
./gradlew assembleDebug
if [ $? -ne 0 ]; then
    echo "错误: 应用编译失败"
    echo "请检查代码中的语法错误"
    exit 1
fi
echo "✓ 编译成功"
echo ""

# 检查APK文件
echo "3. 检查生成的APK文件..."
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✓ APK文件生成成功"
    ls -lh app/build/outputs/apk/debug/app-debug.apk
else
    echo "错误: APK文件未生成"
    exit 1
fi
echo ""

# 检查TTS服务类
echo "4. 检查TTS服务类..."
if grep -q "class TTSEngineService" app/src/main/java/com/wxh/ttsonline/function/TTSEngineService.kt; then
    echo "✓ TTSEngineService类存在"
else
    echo "错误: TTSEngineService类未找到"
    exit 1
fi

# 检查AndroidManifest.xml
echo "5. 检查AndroidManifest.xml..."
if grep -q "TTSEngineService" app/src/main/AndroidManifest.xml; then
    echo "✓ TTS服务已在AndroidManifest.xml中注册"
else
    echo "错误: TTS服务未在AndroidManifest.xml中注册"
    exit 1
fi

# 检查tts_engine.xml
echo "6. 检查tts_engine.xml..."
if [ -f "app/src/main/res/xml/tts_engine.xml" ]; then
    echo "✓ tts_engine.xml配置文件存在"
else
    echo "错误: tts_engine.xml配置文件未找到"
    exit 1
fi

echo ""
echo "=== 编译测试完成 ==="
echo ""
echo "所有检查都通过了！现在可以安装应用进行测试："
echo ""
echo "1. 安装应用："
echo "   adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "2. 启动应用："
echo "   adb shell am start -n com.wxh.ttsonline/.MainActivity"
echo ""
echo "3. 检查TTS引擎注册："
echo "   设置 → 辅助功能 → 文字转语音输出"
echo ""
echo "4. 查看日志："
echo "   adb logcat | grep -E '(TTSEngineService|SpeechService|TTSOnline)'"
