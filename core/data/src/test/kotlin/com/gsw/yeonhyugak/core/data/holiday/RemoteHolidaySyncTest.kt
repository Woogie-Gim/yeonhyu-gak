package com.gsw.yeonhyugak.core.data.holiday

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.data.prefs.AlertPrefsRepository
import com.gsw.yeonhyugak.core.engine.holiday.HolidaySource
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 원격 JSON: 정상 · 스키마 오류 · 네트워크 실패 · 기존과 동일
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class RemoteHolidaySyncTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val server = MockWebServer()
    private val io = Dispatchers.Unconfined

    // 2026-10-21 00:30 KST
    private val clock = Clock.fixed(Instant.parse("2026-10-20T15:30:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.of(2026, 10, 21)

    private lateinit var store: RemoteHolidayStore
    private lateinit var repo: HolidayRepository
    private lateinit var prefs: AlertPrefsRepository
    private lateinit var sync: RemoteHolidaySync

    @Before
    fun setUp() {
        server.start()
        File(context.filesDir, "holidays_override.json").delete()
        store = RemoteHolidayStore(context, io)
        repo = HolidayRepository(context, StoredHolidayOverrides(store), io)
        val file = File(context.filesDir, "prefs_${UUID.randomUUID()}.preferences_pb")
        prefs = AlertPrefsRepository(PreferenceDataStoreFactory.create { file })
        val fetcher = RemoteHolidayFetcher(server.url("/holidays_override.json").toString(), io)
        sync = RemoteHolidaySync(fetcher, store, repo, prefs, clock)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun respond(body: String, code: Int = 200) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
    }

    @Test
    fun valid_addsHolidayAndMarksNew() = runTest {
        assertFalse(repo.calendar().isHoliday(ELECTION))
        respond(V1)

        val result = sync.sync()

        assertEquals(listOf(ELECTION), (result as SyncResult.Updated).newDays.map { it.date })
        val added = repo.calendar().on(ELECTION).single()
        assertEquals(HolidaySource.REMOTE, added.source)
        assertEquals(setOf(ELECTION), prefs.newHolidays(today).first())
        assertEquals("/holidays_override.json", server.takeRequest().path)
    }

    @Test
    fun observe_emitsNewCalendarAfterSync() = runTest {
        assertFalse(repo.observe().first().isHoliday(ELECTION))
        respond(V1)
        sync.sync()
        assertTrue(repo.observe().first().isHoliday(ELECTION))
    }

    @Test
    fun schemaError_keepsBundleAndStored() = runTest {
        respond(V1)
        sync.sync()
        respond("""{"version":2,"holidays":[{"date":"2030-13-01","name":"x","kind":"ELECTION"}]}""")

        assertEquals(SyncResult.Invalid, sync.sync())
        assertTrue(repo.calendar().isHoliday(ELECTION))
        assertEquals(1, store.read()?.version)
    }

    @Test
    fun schemaError_onFirstSync_keepsBundleOnly() = runTest {
        respond("""{"holidays":[]}""")
        assertEquals(SyncResult.Invalid, sync.sync())
        assertEquals(null, store.read())
        // 번들 확정 공휴일은 그대로
        assertTrue(repo.calendar().isHoliday(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun networkFailure_keepsBundle() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertEquals(SyncResult.NetworkError, sync.sync())
        respond("oops", code = 500)
        assertEquals(SyncResult.NetworkError, sync.sync())
        assertFalse(repo.calendar().isHoliday(ELECTION))
        assertTrue(repo.calendar().isHoliday(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun sameAsStored_isUnchanged() = runTest {
        respond(V1)
        sync.sync()
        val before = repo.calendar()
        respond(V1)

        assertEquals(SyncResult.Unchanged, sync.sync())
        // 다시 병합하지 않는다
        assertTrue(before === repo.calendar())
    }

    @Test
    fun newVersion_reportsOnlyAddedDays() = runTest {
        respond(V1)
        sync.sync()
        respond(
            """{"version":2,"holidays":[$ELECTION_JSON,
               {"date":"2030-10-04","name":"임시공휴일","kind":"TEMPORARY"}]}""",
        )
        val result = sync.sync() as SyncResult.Updated
        assertEquals(listOf(LocalDate.of(2030, 10, 4)), result.newDays.map { it.date })
    }

    @Test
    fun newBadge_expiresAfter14Days() = runTest {
        prefs.addNewHolidays(listOf(ELECTION), today)
        assertEquals(setOf(ELECTION), prefs.newHolidays(today.plusDays(14)).first())
        assertEquals(emptySet<LocalDate>(), prefs.newHolidays(today.plusDays(15)).first())
    }

    private companion object {
        val ELECTION: LocalDate = LocalDate.of(2030, 6, 5)
        const val ELECTION_JSON =
            """{"date":"2030-06-05","name":"전국동시지방선거","kind":"ELECTION"}"""
        const val V1 = """{"version":1,"holidays":[$ELECTION_JSON]}"""
    }
}
