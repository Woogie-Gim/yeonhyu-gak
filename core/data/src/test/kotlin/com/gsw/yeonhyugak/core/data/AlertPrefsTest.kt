package com.gsw.yeonhyugak.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.data.nav.OpenDateStore
import com.gsw.yeonhyugak.core.data.prefs.AlertPrefsRepository
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 알림 동의 · 발송 기록 · QR · 열 날짜
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AlertPrefsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val file = File(context.filesDir, "prefs_${UUID.randomUUID()}.preferences_pb")
    private val store = PreferenceDataStoreFactory.create { file }

    private fun prefs(): AlertPrefsRepository = AlertPrefsRepository(store)

    @Test
    fun defaults_notAskedNotOnQrOn() = runTest {
        val p = prefs()
        assertFalse(p.notifyAsked.first())
        assertFalse(p.notifyOn.first())
        assertTrue(UserPrefsRepository(store).shareQr.first())
    }

    @Test
    fun setNotify_marksAsked() = runTest {
        val p = prefs()
        p.setNotify(false)
        assertTrue(p.notifyAsked.first())
        assertFalse(p.notifyOn.first())
        p.setNotify(true)
        assertTrue(p.notifyOn.first())
    }

    @Test
    fun alertSent_accumulates() = runTest {
        val p = prefs()
        p.addAlertSent(listOf("a"))
        p.addAlertSent(listOf("b"))
        assertEquals(setOf("a", "b"), p.alertSentKeys.first())
    }

    @Test
    fun openDate_consumedOnce() {
        val s = OpenDateStore()
        val d = LocalDate.of(2026, 5, 1)
        s.open(d)
        assertEquals(d, s.date.value)
        s.consume()
        assertNull(s.date.value)
    }
}
