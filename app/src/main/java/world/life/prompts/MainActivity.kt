package world.life.prompts

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import world.life.prompts.adapter.ResponsePagerAdapter
import world.life.prompts.adapter.HistoryAdapter
import world.life.prompts.database.AppDatabase
import world.life.prompts.database.ConversationEntity
import world.life.prompts.model.GeminiContent
import world.life.prompts.model.GeminiPart
import world.life.prompts.model.GeminiRequest
import world.life.prompts.model.PromptCategory
import world.life.prompts.network.GeminiApiService
import world.life.prompts.utils.DataManager
import world.life.prompts.utils.NotificationHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var navView: LinearLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var etUserInput: EditText
    private lateinit var rgPromptCategories: RadioGroup
    private lateinit var btnSend: Button
    
    private lateinit var dataManager: DataManager
    private lateinit var geminiApi: GeminiApiService
    private lateinit var responseAdapter: ResponsePagerAdapter
    private lateinit var database: AppDatabase
    
    private var promptCategories = mutableListOf<PromptCategory>()
    private var currentCategoryIndex = -1
    private var currentPromptIndex = 0
    private var isPolling = false
    private var countdownRunnable: Runnable? = null
    private var requestTimerRunnable: Runnable? = null
    private var remainingSeconds = 0
    private var requestElapsedSeconds = 0
    private var totalPromptsInCategory = 0
    private val handler = Handler(Looper.getMainLooper())
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val jsonContent = reader.readText()
                    reader.close()
                    
                    val categories = dataManager.parseJsonFile(jsonContent)
                    dataManager.savePromptCategories(categories)
                    promptCategories.clear()
                    promptCategories.addAll(categories)
                    updateRadioGroup()
                    
                    NotificationHelper.updateNotification(this, "导入成功", "成功导入 ${categories.size} 个分类")
                    Toast.makeText(this, "JSON文件导入成功", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    NotificationHelper.updateNotification(this, "导入失败", "文件格式错误: ${e.message}")
                    Toast.makeText(this, "文件格式错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupToolbar()
        setupDrawer()
        setupViewPager()
        
        dataManager = DataManager(this)
        database = AppDatabase.getDatabase(this)
        setupRetrofit()
        
        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermission()
        loadData()
        
        NotificationHelper.updateNotification(this, "Prompts 应用", "应用已启动，等待操作")
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
    
    private fun initViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        toolbar = findViewById(R.id.toolbar)
        navView = findViewById(R.id.nav_view)
        viewPager = findViewById(R.id.view_pager)
        
        etUserInput = findViewById(R.id.et_user_input)
        rgPromptCategories = findViewById(R.id.rg_prompt_categories)
        btnSend = findViewById(R.id.btn_send)
        
        btnSend.setOnClickListener {
            if (isPolling) {
                stopPolling()
            } else {
                startPolling()
            }
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_launcher_foreground)
    }
    
    private fun setupDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }
    
    private fun setupViewPager() {
        responseAdapter = ResponsePagerAdapter()
        viewPager.adapter = responseAdapter
    }
    
    private fun setupRetrofit() {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
            
        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        geminiApi = retrofit.create(GeminiApiService::class.java)
    }
    
    private fun loadData() {
        promptCategories.addAll(dataManager.getPromptCategories())
        updateRadioGroup()
    }
    
    private fun updateRadioGroup() {
        rgPromptCategories.removeAllViews()
        promptCategories.forEachIndexed { index, category ->
            val radioButton = RadioButton(this).apply {
                text = "${category.name} (${category.prompts.size})"
                id = index
                setTextColor(resources.getColor(android.R.color.white, theme))
                setPadding(16, 16, 16, 16)
            }
            
            // 添加间距
            val layoutParams = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 8, 0, 8)
            radioButton.layoutParams = layoutParams
            
            rgPromptCategories.addView(radioButton)
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                showSettingsBottomSheet()
                true
            }
            R.id.action_history -> {
                showHistoryDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showSettingsBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_settings, null)
        bottomSheetDialog.setContentView(view)
        
        val etApiKey = view.findViewById<TextInputEditText>(R.id.et_api_key)
        val rgModelSelection = view.findViewById<RadioGroup>(R.id.rg_model_selection)
        val rbGeminiPro = view.findViewById<RadioButton>(R.id.rb_gemini_pro)
        val rbGeminiFlash = view.findViewById<RadioButton>(R.id.rb_gemini_flash)
        val rbGeminiFlashLite = view.findViewById<RadioButton>(R.id.rb_gemini_flash_lite)
        val btnSaveSettings = view.findViewById<Button>(R.id.btn_save_settings)
        val btnImportJson = view.findViewById<Button>(R.id.btn_import_json)
        
        etApiKey.setText(dataManager.getApiKey())
        
        // 设置当前选中的模型
        when (dataManager.getSelectedModel()) {
            DataManager.MODEL_GEMINI_PRO -> rbGeminiPro.isChecked = true
            DataManager.MODEL_GEMINI_FLASH -> rbGeminiFlash.isChecked = true
            DataManager.MODEL_GEMINI_FLASH_LITE -> rbGeminiFlashLite.isChecked = true
        }
        
        btnSaveSettings.setOnClickListener {
            val apiKey = etApiKey.text.toString().trim()
            if (apiKey.isNotEmpty()) {
                dataManager.saveApiKey(apiKey)
                
                // 保存选中的模型
                val selectedModel = when (rgModelSelection.checkedRadioButtonId) {
                    R.id.rb_gemini_pro -> DataManager.MODEL_GEMINI_PRO
                    R.id.rb_gemini_flash -> DataManager.MODEL_GEMINI_FLASH
                    R.id.rb_gemini_flash_lite -> DataManager.MODEL_GEMINI_FLASH_LITE
                    else -> DataManager.MODEL_GEMINI_FLASH
                }
                dataManager.saveSelectedModel(selectedModel)
                
                NotificationHelper.updateNotification(this, "设置保存", "API Key 和模型已保存")
                Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(this, "请输入API Key", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnImportJson.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/json"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            filePickerLauncher.launch(Intent.createChooser(intent, "选择JSON文件"))
        }
        
        bottomSheetDialog.show()
    }
    
    private fun showHistoryDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_history, null)
        
        val rvHistory = view.findViewById<RecyclerView>(R.id.rv_history)
        val btnClearHistory = view.findViewById<Button>(R.id.btn_clear_history)
        val btnCloseHistory = view.findViewById<Button>(R.id.btn_close_history)
        
        val historyAdapter = HistoryAdapter()
        rvHistory.adapter = historyAdapter
        rvHistory.layoutManager = LinearLayoutManager(this)
        
        lifecycleScope.launch {
            val conversations = database.conversationDao().getAllConversations()
            historyAdapter.updateConversations(conversations)
        }
        
        val alertDialog = dialog.setView(view).create()
        
        btnClearHistory.setOnClickListener {
            lifecycleScope.launch {
                database.conversationDao().clearAllConversations()
                historyAdapter.updateConversations(emptyList())
                NotificationHelper.updateNotification(this@MainActivity, "历史清空", "所有历史记录已清空")
                Toast.makeText(this@MainActivity, "历史记录已清空", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            }
        }
        
        btnCloseHistory.setOnClickListener {
            alertDialog.dismiss()
        }
        
        alertDialog.show()
    }
    
    private fun startPolling() {
        val userInput = etUserInput.text.toString().trim()
        if (userInput.isEmpty()) {
            Toast.makeText(this, "请输入问题", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (promptCategories.isEmpty()) {
            Toast.makeText(this, "请先导入JSON文件", Toast.LENGTH_SHORT).show()
            return
        }
        
        val selectedId = rgPromptCategories.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(this, "请选择分类", Toast.LENGTH_SHORT).show()
            return
        }
        
        val apiKey = dataManager.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            Toast.makeText(this, "请先设置API Key", Toast.LENGTH_SHORT).show()
            return
        }
        
        currentCategoryIndex = selectedId
        currentPromptIndex = 0
        totalPromptsInCategory = promptCategories[currentCategoryIndex].prompts.size
        isPolling = true
        btnSend.text = "停止"
        
        responseAdapter.clear()
        drawerLayout.closeDrawer(GravityCompat.START)
        
        NotificationHelper.updateNotification(this, "开始轮询", "开始对分类 '${promptCategories[currentCategoryIndex].name}' 进行轮询")
        
        pollNextPrompt(userInput, apiKey)
    }
    
    private fun stopPolling() {
        isPolling = false
        btnSend.text = "发送"
        handler.removeCallbacksAndMessages(null)
        countdownRunnable = null
        requestTimerRunnable = null
        NotificationHelper.updateNotification(this, "停止轮询", "用户手动停止轮询")
    }
    
    private fun startCountdown(userInput: String, apiKey: String) {
        remainingSeconds = 60
        
        countdownRunnable = object : Runnable {
            override fun run() {
                if (!isPolling) return
                
                if (remainingSeconds > 0) {
                    val category = promptCategories[currentCategoryIndex]
                    NotificationHelper.updateNotification(
                        this@MainActivity, 
                        "轮询倒计时", 
                        "等待下一个请求 - ${remainingSeconds}秒 | 分类: ${category.name}"
                    )
                    remainingSeconds--
                    handler.postDelayed(this, 1000)
                } else {
                    pollNextPrompt(userInput, apiKey)
                }
            }
        }
        
        handler.post(countdownRunnable!!)
    }
    
    private fun startRequestTimer(promptName: String) {
        requestElapsedSeconds = 0
        
        requestTimerRunnable = object : Runnable {
            override fun run() {
                if (!isPolling) return
                
                val currentProgress = "${currentPromptIndex + 1}/$totalPromptsInCategory"
                NotificationHelper.updateNotification(
                    this@MainActivity,
                    "请求中($currentProgress)",
                    "$promptName - ${requestElapsedSeconds}秒"
                )
                requestElapsedSeconds++
                handler.postDelayed(this, 1000)
            }
        }
        
        handler.post(requestTimerRunnable!!)
    }
    
    private fun stopRequestTimer() {
        requestTimerRunnable?.let { handler.removeCallbacks(it) }
        requestTimerRunnable = null
    }
    
    private fun pollNextPrompt(userInput: String, apiKey: String) {
        if (!isPolling || currentCategoryIndex == -1) return
        
        if (currentCategoryIndex >= promptCategories.size) return
        
        val category = promptCategories[currentCategoryIndex]
        
        // 检查是否已完成所有prompt
        if (currentPromptIndex >= category.prompts.size) {
            // 轮询完成
            isPolling = false
            btnSend.text = "发送"
            NotificationHelper.updateNotification(this, "轮询完成", "已完成所有 ${category.prompts.size} 个prompt的轮询")
            return
        }
        
        val prompt = category.prompts[currentPromptIndex]
        val promptName = "${category.name} - Prompt ${currentPromptIndex + 1}"
        val fullPrompt = "$prompt $userInput"
        
        // 开始请求计时
        startRequestTimer(promptName)
        
        lifecycleScope.launch {
            try {
                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(fullPrompt))
                        )
                    )
                )
                
                val selectedModel = dataManager.getSelectedModel()
                val apiUrl = "v1beta/models/${selectedModel}:generateContent"
                
                val response = geminiApi.generateContent(apiUrl, apiKey, request = request)
                
                // 停止请求计时
                stopRequestTimer()
                
                if (response.isSuccessful && response.body() != null) {
                    val responseText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "无响应内容"
                    
                    responseAdapter.updateResponse(promptName, responseText)
                    NotificationHelper.updateNotification(this@MainActivity, "请求成功", "收到 $promptName 的响应")
                    
                    // 保存到数据库
                    val conversation = ConversationEntity(
                        question = userInput,
                        response = responseText,
                        promptCategory = category.name,
                        promptName = promptName
                    )
                    database.conversationDao().insertConversation(conversation)
                    
                } else {
                    val errorMsg = "API请求失败: ${response.code()}"
                    responseAdapter.updateResponse(promptName, errorMsg)
                    NotificationHelper.updateNotification(this@MainActivity, "请求失败", "$promptName: ${response.code()}")
                    
                    // 保存错误信息到数据库
                    val conversation = ConversationEntity(
                        question = userInput,
                        response = errorMsg,
                        promptCategory = category.name,
                        promptName = promptName
                    )
                    database.conversationDao().insertConversation(conversation)
                }
            } catch (e: Exception) {
                // 停止请求计时
                stopRequestTimer()
                
                val errorMsg = "网络错误: ${e.message}"
                responseAdapter.updateResponse(promptName, errorMsg)
                NotificationHelper.updateNotification(this@MainActivity, "网络错误", "$promptName: ${e.message}")
                
                // 保存网络错误到数据库
                val conversation = ConversationEntity(
                    question = userInput,
                    response = errorMsg,
                    promptCategory = category.name,
                    promptName = promptName
                )
                database.conversationDao().insertConversation(conversation)
            }
            
            currentPromptIndex++
            
            if (isPolling && currentPromptIndex < category.prompts.size) {
                // 继续下一个prompt，开始1分钟倒计时
                startCountdown(userInput, apiKey)
            } else if (isPolling) {
                // 所有prompt已完成
                isPolling = false
                btnSend.text = "发送"
                NotificationHelper.updateNotification(this@MainActivity, "轮询完成", "已完成所有 ${category.prompts.size} 个prompt的轮询")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        NotificationHelper.clearNotification(this)
    }
    
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}