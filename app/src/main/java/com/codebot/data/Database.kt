package com.codebot.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_history")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
@Entity(
    tableName = "files",
    indices = [Index(value = ["name", "path"], unique = true)]
)
data class CodeFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val content: String,
    val extension: String,
    val path: String = "/"
)

@Dao
interface CodeBotDao {
    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    fun getChatHistory(): Flow<List<ChatMessage>>

    @Insert
    suspend fun insertMessage(message: ChatMessage)

    @Query("SELECT * FROM files")
    fun getFiles(): Flow<List<CodeFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFile(file: CodeFile)

    @Query("DELETE FROM chat_history")
    suspend fun clearChat()

    @Delete
    suspend fun deleteFile(file: CodeFile)
}

@Database(entities = [ChatMessage::class, CodeFile::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): CodeBotDao
}
