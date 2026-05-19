package com.codebot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebot.data.ChatMessage
import com.codebot.data.CodeBotDao
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val dao: CodeBotDao,
    private val generativeModel: GenerativeModel
) : ViewModel() {

    val messages = dao.getChatHistory()

    fun sendMessage(text: String) {
        viewModelScope.launch {
            dao.insertMessage(ChatMessage(role = "user", content = text))
            
            try {
                val response = generativeModel.generateContent(text)
                val responseText = response.text ?: "No response"
                dao.insertMessage(ChatMessage(role = "model", content = responseText))
            } catch (e: Exception) {
                dao.insertMessage(ChatMessage(role = "model", content = "Error: ${e.message}"))
            }
        }
    }
}
