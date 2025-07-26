package world.life.prompts.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_history")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val question: String,
    val response: String,
    val promptCategory: String,
    val promptName: String,
    val timestamp: Long = System.currentTimeMillis()
)