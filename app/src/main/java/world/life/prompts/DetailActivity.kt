package world.life.prompts

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class DetailActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_QUESTION = "extra_question"
        const val EXTRA_RESPONSE = "extra_response"
        const val EXTRA_PROMPT_NAME = "extra_prompt_name"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        
        setupToolbar()
        loadData()
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "详情"
    }
    
    private fun loadData() {
        val question = intent.getStringExtra(EXTRA_QUESTION) ?: ""
        val response = intent.getStringExtra(EXTRA_RESPONSE) ?: ""
        val promptName = intent.getStringExtra(EXTRA_PROMPT_NAME) ?: ""
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, 0)
        
        findViewById<TextView>(R.id.tv_prompt_name).text = promptName
        findViewById<TextView>(R.id.tv_timestamp).text = formatTimestamp(timestamp)
        findViewById<TextView>(R.id.tv_question_content).text = question
        findViewById<TextView>(R.id.tv_response_content).text = response
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        return if (timestamp > 0) {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        } else {
            "未知时间"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}