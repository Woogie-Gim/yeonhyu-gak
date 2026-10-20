package com.gsw.yeonhyugak.core.data.holiday

import android.content.Context
import android.util.AtomicFile
import com.gsw.yeonhyugak.core.data.di.IoDispatcher
import com.gsw.yeonhyugak.core.engine.holiday.HolidayBundleCodec
import com.gsw.yeonhyugak.core.engine.holiday.HolidayDataException
import com.gsw.yeonhyugak.core.engine.holiday.HolidayOverrides
import com.gsw.yeonhyugak.core.engine.holiday.RemoteHolidays
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

// 검증을 통과한 원격 파일만 저장. 없거나 깨지면 null → 번들만 사용
@Singleton
class RemoteHolidayStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {
    private val file get() = AtomicFile(File(context.filesDir, FILE))

    suspend fun read(): RemoteHolidays? = withContext(io) {
        val f = file
        if (!f.baseFile.exists()) return@withContext null
        try {
            HolidayBundleCodec.parseRemote(f.readFully().toString(Charsets.UTF_8))
        } catch (_: HolidayDataException) {
            null
        }
    }

    suspend fun write(text: String) = withContext(io) {
        val f = file
        val out = f.startWrite()
        try {
            out.write(text.toByteArray(Charsets.UTF_8))
            f.finishWrite(out)
        } catch (e: java.io.IOException) {
            f.failWrite(out)
            throw e
        }
    }

    private companion object {
        const val FILE = "holidays_override.json"
    }
}

class StoredHolidayOverrides @Inject constructor(private val store: RemoteHolidayStore) :
    HolidayOverrideSource {
    override suspend fun current(): HolidayOverrides =
        store.read()?.toOverrides() ?: HolidayOverrides.NONE
}
