package com.jinkim.hyundaiAndGretel.di

import com.jinkim.hyundaiAndGretel.gleo.GleoActionProcessor
import com.jinkim.hyundaiAndGretel.service.RoadNoteSubmissionClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object GleoModule {
    @Provides
    fun provideGleoProcessor(client: RoadNoteSubmissionClient) = GleoActionProcessor(client::submitReport)
}
