package com.gsw.yeonhyugak.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.gsw.yeonhyugak.core.data.db.YhDatabase
import com.gsw.yeonhyugak.core.data.holiday.HolidayRepository
import com.gsw.yeonhyugak.core.data.holiday.NoHolidayOverrides
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.memo.MemoRepository
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.feature.calendar.CalendarRoute
import com.gsw.yeonhyugak.feature.calendar.CalendarViewModel
import com.gsw.yeonhyugak.feature.calendar.cellTag
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 달력 라이트·다크 × 글자 100%·200% + 예상 연도 + 빈 탭
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class CalendarScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(
        context,
        YhDatabase::class.java,
    ).allowMainThreadQueries().build()
    private val prefsFile = File(context.filesDir, "prefs_${UUID.randomUUID()}.preferences_pb")
    private val store = ViewModelStore()

    @After
    fun tearDown() {
        store.clear()
        db.close()
        prefsFile.delete()
    }

    @Test
    fun calendar_light_100() = capture("calendar_light_100", dark = false, fontScale = 1f)

    @Test
    fun calendar_dark_100() = capture("calendar_dark_100", dark = true, fontScale = 1f)

    @Test
    fun calendar_light_200() = capture("calendar_light_200", dark = false, fontScale = 2f)

    @Test
    fun calendar_dark_200() = capture("calendar_dark_200", dark = true, fontScale = 2f)

    // 예상 공휴일 점선 + 예상 배지 + 첫 진입 코치마크
    @Test
    fun calendar_expectedYear() = capture(
        "calendar_expected_2030",
        dark = false,
        fontScale = 1f,
        today = LocalDate.of(2030, 9, 10),
        coachShown = false,
    )

    @Test
    fun emptyTab_leave() {
        launch(dark = false, today = MAY_6, coachShown = true)
        rule.onNodeWithTag(tabTag(YhTab.LEAVE)).performClick()
        rule.onRoot().captureRoboImage("src/test/screenshots/empty_leave_light.png")
    }

    private fun capture(
        name: String,
        dark: Boolean,
        fontScale: Float,
        today: LocalDate = MAY_6,
        coachShown: Boolean = true,
    ) {
        RuntimeEnvironment.setFontScale(fontScale)
        launch(dark, today, coachShown)
        rule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private fun launch(dark: Boolean, today: LocalDate, coachShown: Boolean) {
        val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        val leaves = LeaveRepository(db)
        val memos = MemoRepository(db, clock)
        val prefs = UserPrefsRepository(PreferenceDataStoreFactory.create { prefsFile })
        runBlocking {
            if (coachShown) prefs.markCoachMarkShown()
            // 연차 · 오전/오후 반차 · 메모 점 · 연휴 띠가 모두 보이게
            leaves.mark(listOf(today.withDayOfMonth(4)), LeaveType.FULL)
            leaves.mark(listOf(today.withDayOfMonth(19)), LeaveType.AM_HALF)
            leaves.mark(listOf(today.withDayOfMonth(20)), LeaveType.PM_HALF)
            memos.save(today.withDayOfMonth(14), "screenshot")
        }
        val holidays = HolidayRepository(context, NoHolidayOverrides(), Dispatchers.Unconfined)
        val vm = ViewModelProvider.create(
            store,
            viewModelFactory {
                initializer { CalendarViewModel(holidays, leaves, memos, prefs, clock) }
            },
        )[CalendarViewModel::class]
        rule.setContent {
            YhTheme(darkTheme = dark) {
                YhApp(calendar = { CalendarRoute(snackbarHostState = it, viewModel = vm) })
            }
        }
        rule.waitUntil(TIMEOUT) {
            rule.onAllNodes(
                hasTestTag(cellTag(today.withDayOfMonth(1))),
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        val MAY_6: LocalDate = LocalDate.of(2026, 5, 6)
        const val TIMEOUT = 5_000L
    }
}
