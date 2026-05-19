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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val dao: CodeBotDao,
    private val generativeModel: GenerativeModel
) : ViewModel() {

    val messages = dao.getChatHistory()
    private val files = dao.getFiles()

    fun sendMessage(text: String) {
        viewModelScope.launch {
            dao.insertMessage(ChatMessage(role = "user", content = text))
            
            try {
                // Fetch current files to provide as context
                val currentFiles = files.first()
                val projectContext = if (currentFiles.isEmpty()) {
                    "The project is currently empty."
                } else {
                    "Current project files:\n" + currentFiles.joinToString("\n") { file ->
                        "- ${file.path}${file.name} (${file.extension})"
                    }
                }

                val fullPrompt = "Project Context:\n$projectContext\n\nUser Request: $text"
                
                val response = generativeModel.generateContent(fullPrompt)
                val responseText = response.text ?: "No response"
                dao.insertMessage(ChatMessage(role = "model", content = responseText))
                
                // Automatic Extraction
                extractAndSaveFiles(responseText)
            } catch (e: Exception) {
                dao.insertMessage(ChatMessage(role = "model", content = "Error: ${e.message}"))
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            dao.clearChat()
        }
    }

    private fun extractAndSaveFiles(text: String) {
        val codeBlockRegex = Regex("```(\\w+)\\n([\\s\\S]*?)```")
        codeBlockRegex.findAll(text).forEach { match ->
            val lang = match.groupValues[1]
            val content = match.groupValues[2]
            
            // Advanced filename extraction
            // 1. Look for @file: path/name.ext 
            // 2. Look for // Path: path/name.ext
            // 3. Look for "File: name.ext" in surrounding text
            
            var fileName = "snippet_${System.currentTimeMillis()}.$lang"
            
            // Check for annotations in the first 3 lines of code
            val lines = content.lineSequence().take(3).toList()
            for (line in lines) {
                val cleanLine = line.trim()
                if (cleanLine.contains("/") || cleanLine.contains(".")) {
                    val potential = cleanLine
                        .replace("//", "")
                        .replace("/*", "")
                        .replace("*", "")
                        .replace("@file:", "")
                        .replace("Path:", "")
                        .trim()
                    
                    if (potential.contains(".") && !potential.contains(" ")) {
                        fileName = potential
                        break
                    }
                }
            }

            if (lang == "html" && fileName.startsWith("snippet")) fileName = "index.html"

            viewModelScope.launch {
                dao.saveFile(com.codebot.data.CodeFile(
                    name = fileName.substringAfterLast("/"),
                    content = content,
                    extension = fileName.substringAfterLast(".", lang),
                    path = if (fileName.contains("/")) {
                         val p = fileName.substringBeforeLast("/")
                         if (p.startsWith("/")) p else "/$p"
                    } else "/"
                ))
            }
        }
    }
}
