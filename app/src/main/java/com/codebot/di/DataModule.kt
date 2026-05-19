package com.codebot.di

import android.content.Context
import androidx.room.Room
import com.codebot.data.AppDatabase
import com.codebot.data.CodeBotDao
import com.google.ai.client.generativeai.GenerativeModel
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
        ).build()
    }

    @Provides
    fun provideDao(db: AppDatabase): CodeBotDao = db.dao()

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        // In a real app, you'd get this from BuildConfig or a secure store
        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "YOUR_API_KEY_HERE"
        )
    }
}
