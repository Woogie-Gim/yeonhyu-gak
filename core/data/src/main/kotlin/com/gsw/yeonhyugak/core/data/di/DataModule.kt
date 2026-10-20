package com.gsw.yeonhyugak.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.gsw.yeonhyugak.core.data.db.YhDatabase
import com.gsw.yeonhyugak.core.data.holiday.HolidayOverrideSource
import com.gsw.yeonhyugak.core.data.holiday.StoredHolidayOverrides
import com.gsw.yeonhyugak.core.engine.planner.Recommender
import com.gsw.yeonhyugak.core.engine.planner.VacationPlanner
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

// 원격 공휴일 JSON 주소. :app이 BuildConfig로 공급
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteHolidayUrl

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): YhDatabase =
        Room.databaseBuilder(context, YhDatabase::class.java, YhDatabase.NAME).build()

    @Provides
    @Singleton
    fun preferences(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("user_prefs") }

    @Provides
    fun clock(): Clock = Clock.systemDefaultZone()

    // 엔진은 순수 Kotlin이라 여기서 Clock을 넣어 만든다
    @Provides
    fun recommender(clock: Clock): Recommender = Recommender(VacationPlanner(clock))

    @Provides
    @IoDispatcher
    fun io(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
interface HolidayModule {
    // 저장된 원격 파일. 없으면 번들만
    @Binds
    fun overrides(impl: StoredHolidayOverrides): HolidayOverrideSource
}
