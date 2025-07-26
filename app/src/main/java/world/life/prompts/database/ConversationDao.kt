package world.life.prompts.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ConversationDao {
    
    @Insert
    suspend fun insertConversation(conversation: ConversationEntity)
    
    @Query("SELECT * FROM conversation_history ORDER BY timestamp DESC")
    suspend fun getAllConversations(): List<ConversationEntity>
    
    @Query("SELECT * FROM conversation_history WHERE promptCategory = :category ORDER BY timestamp DESC")
    suspend fun getConversationsByCategory(category: String): List<ConversationEntity>
    
    @Query("DELETE FROM conversation_history")
    suspend fun clearAllConversations()
}