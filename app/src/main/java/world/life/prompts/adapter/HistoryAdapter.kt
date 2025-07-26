package world.life.prompts.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import world.life.prompts.DetailActivity
import world.life.prompts.R
import world.life.prompts.database.ConversationEntity
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    
    private val conversations = mutableListOf<ConversationEntity>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    
    fun updateConversations(newConversations: List<ConversationEntity>) {
        conversations.clear()
        conversations.addAll(newConversations)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(conversations[position])
    }
    
    override fun getItemCount(): Int = conversations.size
    
    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_category)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        private val tvQuestion: TextView = itemView.findViewById(R.id.tv_question)
        private val tvResponse: TextView = itemView.findViewById(R.id.tv_response)
        
        fun bind(conversation: ConversationEntity) {
            tvCategory.text = "${conversation.promptCategory} - ${conversation.promptName}"
            tvTimestamp.text = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(conversation.timestamp))
            tvQuestion.text = "问题: ${conversation.question}"
            tvResponse.text = "回答: ${conversation.response}"
            
            // 添加点击事件
            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, DetailActivity::class.java).apply {
                    putExtra(DetailActivity.EXTRA_QUESTION, conversation.question)
                    putExtra(DetailActivity.EXTRA_RESPONSE, conversation.response)
                    putExtra(DetailActivity.EXTRA_PROMPT_NAME, "${conversation.promptCategory} - ${conversation.promptName}")
                    putExtra(DetailActivity.EXTRA_TIMESTAMP, conversation.timestamp)
                }
                context.startActivity(intent)
            }
        }
    }
}