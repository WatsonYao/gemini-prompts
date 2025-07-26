package world.life.prompts.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import world.life.prompts.model.PromptCategory

class DataManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("prompts_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_PROMPT_CATEGORIES = "prompt_categories"
        private const val KEY_SELECTED_MODEL = "selected_model"
        const val MODEL_GEMINI_PRO = "gemini-2.5-pro"
        const val MODEL_GEMINI_FLASH = "gemini-2.5-flash"
        const val MODEL_GEMINI_FLASH_LITE = "gemini-2.5-flash-lite"
    }
    
    fun saveApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }
    
    fun getApiKey(): String? {
        return prefs.getString(KEY_API_KEY, null)
    }
    
    fun saveSelectedModel(model: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL, model).apply()
    }
    
    fun getSelectedModel(): String {
        return prefs.getString(KEY_SELECTED_MODEL, MODEL_GEMINI_FLASH) ?: MODEL_GEMINI_FLASH
    }
    
    fun savePromptCategories(categories: List<PromptCategory>) {
        val json = gson.toJson(categories)
        prefs.edit().putString(KEY_PROMPT_CATEGORIES, json).apply()
    }
    
    fun getPromptCategories(): List<PromptCategory> {
        val json = prefs.getString(KEY_PROMPT_CATEGORIES, null) ?: return emptyList()
        val type = object : TypeToken<List<PromptCategory>>() {}.type
        return gson.fromJson(json, type)
    }
    
    fun parseJsonFile(jsonContent: String): List<PromptCategory> {
        val type = object : TypeToken<List<Map<String, List<String>>>>() {}.type
        val rawData: List<Map<String, List<String>>> = gson.fromJson(jsonContent, type)
        
        return rawData.flatMap { categoryMap ->
            categoryMap.map { (name, prompts) ->
                PromptCategory(name, prompts)
            }
        }
    }
}