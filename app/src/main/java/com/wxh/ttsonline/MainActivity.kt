package com.wxh.ttsonline

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.wxh.ttsonline.configuration.TTSApplication
import com.wxh.ttsonline.function.SpeechEngine
import com.wxh.ttsonline.function.SpeechService
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {
    private val speechEngine: SpeechEngine
        get() = (applicationContext as TTSApplication).speechEngine
    private val speechService: SpeechService
        get() = (applicationContext as TTSApplication).speechService

    // 声明界面元素变量
    private lateinit var appIdInput: TextInputEditText
    private lateinit var tokenInput: TextInputEditText
    private lateinit var sceneSpinner: Spinner
    private lateinit var speakerSpinner: Spinner
    private lateinit var textInput: TextInputEditText
    private lateinit var saveSettingsButton: MaterialButton
    private lateinit var synthesizeButton: MaterialButton
    private lateinit var progressBar: View

    // 选中的场景和 speakerType
    private var selectedScene: String? = null
    private var selectedSpeakerType: String? = null
    private var speakerList: List<String> = emptyList()
    private var filteredSpeakerList: List<String> = emptyList()
    private var speakerTypeMap: Map<String, String> = emptyMap()

    // 定义SharedPreferences
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化SharedPreferences
        sharedPreferences = getSharedPreferences("TTSOnlineSettings", MODE_PRIVATE)

        // 初始化界面元素
        initViews()

        // 设置 spinner 适配器
        setupSpinner()

        // 从SharedPreferences加载设置
        loadSettings()

        // 设置按钮点击事件
        setupButtonListeners()
    }

    private fun initViews() {
        appIdInput = findViewById(R.id.app_id_input)
        tokenInput = findViewById(R.id.token_input)
        sceneSpinner = findViewById(R.id.scene_spinner)
        speakerSpinner = findViewById(R.id.speaker_spinner)
        textInput = findViewById(R.id.text_input)
        saveSettingsButton = findViewById(R.id.save_settings_button)
        synthesizeButton = findViewById(R.id.synthesize_button)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun setupSpinner() {
        // 初始化声音列表和映射
        initSpeakerData()

        // 找到默认声音项（第一列为true的项）
        val defaultSoundItem = speakerList.find { it.startsWith("true|") }

        // 如果有默认声音项，提取其场景并设置为默认场景
        if (defaultSoundItem != null) {
            val defaultScene = defaultSoundItem.split('|')[1]
            selectedScene = defaultScene
        }

        // 初始化场景 spinner
        setupSceneSpinner()

        // 初始化声音 spinner
        setupSpeakerSpinner()
    }

    private fun setupSceneSpinner() {
        // 获取场景分类数组
        val sceneCategories = resources.getStringArray(R.array.scene_categories)

        // 创建场景适配器
        val sceneAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sceneCategories)
        sceneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // 设置场景适配器
        sceneSpinner.adapter = sceneAdapter

        // 如果有默认场景，设置选中它
        if (selectedScene != null) {
            val defaultScenePosition = sceneCategories.indexOf(selectedScene)
            if (defaultScenePosition >= 0) {
                sceneSpinner.setSelection(defaultScenePosition)
            }
        }

        // 设置场景选择监听
        sceneSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedScene = parent.getItemAtPosition(position) as String
                // 根据选择的场景过滤声音列表
                filterSpeakerListByScene(selectedScene)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedScene = null
            }
        }
    }

    private fun initSpeakerData() {
        // 获取 speaker_list 数组
        speakerList = resources.getStringArray(R.array.speaker_list).toList()

        // 创建声音类型映射（显示名称 -> 实际类型）和过滤列表
        speakerTypeMap = speakerList.associate {
            val parts = it.split('|')
            val displayName = parts[2] // 第三列是显示名称
            displayName to parts[3]    // 第四列是实际类型
        }
    }

    private fun setupSpeakerSpinner() {
        // 默认显示所有声音
        filteredSpeakerList = if (selectedScene.isNullOrEmpty()) {
            // 提取所有声音的显示名称
            speakerList.map { it.split('|')[2] }
        } else {
            // 根据场景过滤
            filterSpeakerListByScene(selectedScene)
        }

        // 创建声音适配器
        val speakerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filteredSpeakerList)
        speakerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // 设置声音适配器
        speakerSpinner.adapter = speakerAdapter

        // 查找并设置默认选中项（第一列为"true"的项）
        val defaultPosition = speakerList.indexOfFirst { it.startsWith("true|") }
        if (defaultPosition >= 0) {
            val defaultDisplayName = speakerList[defaultPosition].split('|')[2]
            val defaultFilteredPosition = filteredSpeakerList.indexOf(defaultDisplayName)
            if (defaultFilteredPosition >= 0) {
                speakerSpinner.setSelection(defaultFilteredPosition)
                selectedSpeakerType = speakerList[defaultPosition].split('|')[3]
            }
        }

        // 设置声音选择监听
        speakerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedDisplayName = parent.getItemAtPosition(position) as String
                selectedSpeakerType = speakerTypeMap[selectedDisplayName]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedSpeakerType = null
            }
        }
    }

    // 保存设置到SharedPreferences
    private fun saveSettings(appId: String, token: String) {
        sharedPreferences.edit {
            putString("appId", appId)
            putString("token", token)
            putString("selectedScene", selectedScene)
            putString("selectedSpeakerType", selectedSpeakerType)
        }
    }

    // 从SharedPreferences加载设置
    private fun loadSettings() {
        val appId = sharedPreferences.getString("appId", "")
        val token = sharedPreferences.getString("token", "")
        selectedScene = sharedPreferences.getString("selectedScene", selectedScene)
        selectedSpeakerType = sharedPreferences.getString("selectedSpeakerType", selectedSpeakerType)

        // 设置输入框的值
        appIdInput.setText(appId)
        tokenInput.setText(token)

        // 如果加载了场景，需要更新声音列表
        if (selectedScene != null) {
            val sceneCategories = resources.getStringArray(R.array.scene_categories)
            val scenePosition = sceneCategories.indexOf(selectedScene)
            if (scenePosition >= 0) {
                sceneSpinner.setSelection(scenePosition)
                // 延迟更新speakerSpinner，确保场景已设置
                sceneSpinner.post {
                    filterSpeakerListByScene(selectedScene)
                }
            }
        }
    }

    private fun filterSpeakerListByScene(scene: String?): List<String> {
        filteredSpeakerList = if (scene.isNullOrEmpty()) {
            speakerList.map { it.split('|')[2] }
        } else {
            speakerList
                .filter { it.contains("|$scene|") }
                .map { it.split('|')[2] }
        }

        // 更新声音适配器
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filteredSpeakerList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        speakerSpinner.adapter = adapter

        // 如果有默认选中项且在过滤后的列表中，选中它
        if (selectedSpeakerType != null) {
            val defaultItem = speakerList.find { it.split('|').lastOrNull() == selectedSpeakerType }
            if (defaultItem != null) {
                val defaultDisplayName = defaultItem.split('|')[2]
                val position = filteredSpeakerList.indexOf(defaultDisplayName)
                if (position >= 0) {
                    speakerSpinner.setSelection(position)
                }
            }
        }

        return filteredSpeakerList
    }

    private fun setupButtonListeners() {
        // 保存设置按钮
        saveSettingsButton.setOnClickListener { 
            val appId = appIdInput.text.toString().trim()
            val token = tokenInput.text.toString().trim()

            // 验证输入
            if (appId.isEmpty() || token.isEmpty() || selectedSpeakerType.isNullOrEmpty() || selectedScene.isNullOrEmpty()) {
                Toast.makeText(this, "设置内容不完整", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 保存设置到SharedPreferences
            saveSettings(appId, token)

            // 调用 SpeechEngine.initEngine 函数
            speechEngine.initEngine(appId, token, selectedSpeakerType!!)
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        }

        // 合成按钮
        synthesizeButton.setOnClickListener {
            val text = textInput.text.toString()

            if (text.isBlank()) {
                Toast.makeText(this, "请输入要合成的文本", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 显示进度条
            progressBar.visibility = View.VISIBLE
            synthesizeButton.isEnabled = false

            // 调用 SpeechService.tts 函数
            speechService.tts(text)

            // 隐藏进度条
            progressBar.visibility = View.GONE
            synthesizeButton.isEnabled = true
        }
    }
}