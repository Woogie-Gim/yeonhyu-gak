package com.gsw.yeonhyugak.core.data.holiday

import com.gsw.yeonhyugak.core.data.di.IoDispatcher
import com.gsw.yeonhyugak.core.data.di.RemoteHolidayUrl
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

// 원격 공휴일 JSON 원문. 200 이외는 IOException
class RemoteHolidayFetcher @Inject constructor(
    @param:RemoteHolidayUrl private val url: String,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun fetch(): String = withContext(io) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.useCaches = false
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
    }
}
