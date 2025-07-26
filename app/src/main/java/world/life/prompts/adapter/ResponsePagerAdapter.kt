package world.life.prompts.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import world.life.prompts.R

class ResponsePagerAdapter : RecyclerView.Adapter<ResponsePagerAdapter.ResponseViewHolder>() {
    
    private val responses = mutableListOf<Pair<String, String>>()
    
    fun updateResponse(promptName: String, response: String) {
        val index = responses.indexOfFirst { it.first == promptName }
        if (index != -1) {
            responses[index] = Pair(promptName, response)
            notifyItemChanged(index)
        } else {
            responses.add(Pair(promptName, response))
            notifyItemInserted(responses.size - 1)
        }
    }
    
    fun clear() {
        val size = responses.size
        responses.clear()
        notifyItemRangeRemoved(0, size)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResponseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_response_page, parent, false)
        return ResponseViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ResponseViewHolder, position: Int) {
        val (promptName, response) = responses[position]
        holder.bind(promptName, response)
    }
    
    override fun getItemCount(): Int = responses.size
    
    class ResponseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.tv_prompt_title)
        private val responseText: TextView = itemView.findViewById(R.id.tv_response)
        
        fun bind(promptName: String, response: String) {
            titleText.text = promptName
            responseText.text = response
        }
    }
}