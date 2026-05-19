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
                
                // Automatic Extraction
                extractAndSaveFiles(responseText)
            } catch (e: Exception) {
                dao.insertMessage(ChatMessage(role = "model", content = "Error: ${e.message}"))
            }
        }
    }

    private fun extractAndSaveFiles(text: String) {
        val codeBlockRegex = Regex("```(\\w+)\\n([\\s\\S]*?)```")
        codeBlockRegex.findAll(text).forEach { match ->
            val lang = match.groupValues[1]
            val content = match.groupValues[2]
            
            // Try to find a filename hint in the previous few lines or within the code
            // Look for patterns like "File: filename.ext" or "// filename.ext"
            var fileName = "snippet_${System.currentTimeMillis()}.$lang"
            
            if (lang == "html" || content.contains("<!DOCTYPE html>")) fileName = "index.html"
            else if (lang == "javascript" || lang == "js") fileName = "script.js"
            else if (lang == "css") fileName = "styles.css"
            
            // Check for file hint in the first line of content
            val firstLine = content.lineSequence().firstOrNull() ?: ""
            if (firstLine.contains("/") || firstLine.contains(".")) {
                val potentialName = firstLine.replace("//", "").replace("/*", "").replace("*", "").trim()
                if (potentialName.contains(".")) {
                    fileName = potentialName
                }
            }

            viewModelScope.launch {
                dao.saveFile(com.codebot.data.CodeFile(
                    name = fileName.substringAfterLast("/"),
                    content = content,
                    extension = fileName.substringAfterLast(".", lang),
                    path = if (fileName.contains("/")) "/${fileName.substringBeforeLast("/")}/" else "/"
                ))
            }
        }
    }
}
