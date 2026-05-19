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
@InstallIn(SingletonComponent)
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
        // The secrets-gradle-plugin provides this from local.properties or env vars
        val apiKey = try {
            com.codebot.BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            "YOUR_API_KEY_HERE"
        }
        
        return GenerativeModel(
            modelName = "gemini-1.5-flash",
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
