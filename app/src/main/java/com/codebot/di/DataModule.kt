package com.codebot.di

import android.content.Context
import androidx.room.Room
import com.codebot.data.AppDatabase
import com.codebot.data.CodeBotDao
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "codebot_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideDao(db: AppDatabase): CodeBotDao = db.dao()

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        val apiKey = com.codebot.BuildConfig.GEMINI_API_KEY
        
        return GenerativeModel(
            modelName = "gemini-1.5-flash-latest",
            apiKey = apiKey,
            systemInstruction = content {
                text("You are an automated coding assistant for a mobile app. " +
                     "When you provide code snippets, ALWAYS start the first line of the code block with a comment indicating the file path and name, like this: // @file: path/to/filename.ext " +
                     "This allows the app to automatically save the file to the correct location. " +
                     "If it's a web project, prioritize index.html, styles.css, and script.js.")
            }
        )
    }
}
