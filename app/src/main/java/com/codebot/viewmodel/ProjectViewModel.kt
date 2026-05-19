package com.codebot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codebot.data.CodeBotDao
import com.codebot.data.CodeFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val dao: CodeBotDao
) : ViewModel() {
    val files = dao.getFiles()
    
    private val _selectedFile = MutableStateFlow<CodeFile?>(null)
    val selectedFile = _selectedFile.asStateFlow()

    fun selectFile(file: CodeFile?) {
        _selectedFile.value = file
    }

    fun updateFileContent(content: String) {
        _selectedFile.value?.let { file ->
            val updatedFile = file.copy(content = content)
            _selectedFile.value = updatedFile
            viewModelScope.launch {
                dao.saveFile(updatedFile)
            }
        }
    }
}
