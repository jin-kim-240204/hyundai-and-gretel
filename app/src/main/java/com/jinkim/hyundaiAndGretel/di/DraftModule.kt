package com.jinkim.hyundaiAndGretel.di

import android.content.Context
import com.jinkim.hyundaiAndGretel.roadnote.DraftStore
import com.jinkim.hyundaiAndGretel.roadnote.LocalDraftStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DraftModule {
    /** Application Context로 Android 암호화 저장소를 만들어 필요한 객체에 제공한다. */
    @Provides
    fun provideDraftStore(@ApplicationContext context: Context): DraftStore = LocalDraftStore(context)
}
