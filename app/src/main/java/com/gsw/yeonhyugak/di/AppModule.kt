package com.gsw.yeonhyugak.di

import com.gsw.yeonhyugak.BuildConfig
import com.gsw.yeonhyugak.core.data.di.RemoteHolidayUrl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @RemoteHolidayUrl
    fun remoteHolidayUrl(): String = BuildConfig.HOLIDAY_OVERRIDE_URL
}
