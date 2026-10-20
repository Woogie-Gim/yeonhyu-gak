package com.gsw.yeonhyugak.feature.share

import android.app.Application
import android.content.Context
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.gsw.yeonhyugak.core.data.db.YhDatabase
import com.gsw.yeonhyugak.core.data.holiday.HolidayRepository
import com.gsw.yeonhyugak.core.data.holiday.NoHolidayOverrides
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.memo.MemoRepository
import com.gsw.yeonhyugak.core.data.prefs.HireInfo
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.leave.LeaveBasis
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 공유 카드: 개인 정보 제외 · QR = 스토어 주소 · 이미지 생성
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class ShareCardTest {
    @get:Rule
    val rule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, YhDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val prefsFile = File(context.filesDir, "prefs_${UUID.randomUUID()}.preferences_pb")
    private val prefs = UserPrefsRepository(PreferenceDataStoreFactory.create { prefsFile })
    private val leaves = LeaveRepository(db)
    private val memos = MemoRepository(db, Clock.systemUTC())
    private val holidays = HolidayRepository(context, NoHolidayOverrides(), Dispatchers.Unconfined)
    private val store = ViewModelStore()

    @After
    fun tearDown() {
        store.clear()
        db.close()
        prefsFile.delete()
    }

    private fun viewModel(): ShareViewModel = ViewModelProvider.create(
        store,
        viewModelFactory { initializer { ShareViewModel(holidays, leaves, prefs) } },
    )[ShareViewModel::class]

    // 입사일·메모가 저장된 사용자
    private fun setUpPrivateUser(qr: Boolean = true) = runBlocking {
        prefs.completeOnboarding(15.0, HireInfo(HIRE, LeaveBasis.ANNIVERSARY))
        prefs.setShareQr(qr)
        leaves.mark(listOf(MAY_4), LeaveType.FULL)
        memos.save(MAY_4, MEMO)
        memos.save(LocalDate.of(2026, 5, 1), MEMO_2)
    }

    private fun show() {
        val vm = viewModel()
        rule.setContent {
            YhTheme(darkTheme = false) {
                ShareRoute(year = 2026, onDismiss = {}, viewModel = vm, onShare = { _, _ -> })
            }
        }
        rule.waitUntil(TIMEOUT) {
            rule.onAllNodes(hasTestTag(SHARE_CARD_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun card_hasNoMemoOrHireDate() {
        setUpPrivateUser()
        show()
        val text = allText()
        // 카드는 실제로 그려졌다
        assertTrue(text, text.contains("2026년 연휴"))
        assertTrue(text, text.contains("5월 1일(금) ~ 5월 5일(화)"))
        FORBIDDEN.forEach { assertFalse("카드에 '$it' 포함: $text", text.contains(it)) }
    }

    @Test
    fun card_bothRatios_haveNoPrivateText() {
        setUpPrivateUser()
        show()
        rule.onNodeWithTag(ratioTag(ShareRatio.STORY)).performClick()
        rule.waitForIdle()
        val text = allText()
        FORBIDDEN.forEach { assertFalse("스토리 카드에 '$it' 포함", text.contains(it)) }
    }

    @Test
    fun viewModel_cannotReadMemoOrHireInfo() {
        val params = ShareViewModel::class.java.constructors.flatMap { it.parameterTypes.toList() }
        assertFalse(params.contains(MemoRepository::class.java))
        assertFalse(params.contains(HireInfo::class.java))
        val fields = ShareCardData::class.java.declaredFields.map { it.type } +
            ShareBlock::class.java.declaredFields.map { it.type }
        assertFalse(fields.contains(HireInfo::class.java))
        assertFalse(ShareCardData::class.java.declaredFields.any { it.name.contains("memo", true) })
    }

    @Test
    fun qr_decodesToPlayStoreUrl() {
        val m = QrMatrix.encode(PLAY_STORE_URL)
        // 칸 하나를 SCALE×SCALE 픽셀로 키워 실제 카메라 인식과 비슷하게
        val w = m.width * SCALE
        val h = m.height * SCALE
        val pixels = IntArray(w * h) { i ->
            if (m[(i % w) / SCALE, (i / w) / SCALE]) BLACK else WHITE
        }
        val source = RGBLuminanceSource(w, h, pixels)
        val decoded = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
        assertEquals("https://play.google.com/store/apps/details?id=com.gsw.yeonhyugak", decoded)
        assertFalse(decoded.contains(".debug"))
    }

    @Test
    fun qrToggleOff_hidesQr() {
        setUpPrivateUser(qr = false)
        show()
        assertTrue(rule.onAllNodes(hasTestTag(SHARE_QR_TAG)).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun qrOn_shownByDefault() {
        runBlocking { leaves.mark(listOf(MAY_4), LeaveType.FULL) }
        show()
        assertEquals(1, rule.onAllNodes(hasTestTag(SHARE_QR_TAG)).fetchSemanticsNodes().size)
    }

    // 이미지로 나가는 카드 안의 글자·설명 전부 (병합 전 트리)
    private fun allText(): String {
        val root = rule.onNodeWithTag(SHARE_CARD_TAG, useUnmergedTree = true).fetchSemanticsNode()
        val out = StringBuilder()
        fun walk(node: SemanticsNode) {
            node.config.getOrNull(SemanticsProperties.Text)?.forEach {
                out.append(it.text).append('\n')
            }
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.forEach {
                out.append(it).append('\n')
            }
            node.children.forEach(::walk)
        }
        walk(root)
        return out.toString()
    }

    private companion object {
        const val TIMEOUT = 5_000L
        const val BLACK = 0xFF000000.toInt()
        const val WHITE = 0xFFFFFFFF.toInt()
        const val SCALE = 4
        val HIRE: LocalDate = LocalDate.of(2023, 3, 2)
        val MAY_4: LocalDate = LocalDate.of(2026, 5, 4)
        const val MEMO = "병원 예약"
        const val MEMO_2 = "제주 항공권 KE1234"
        val FORBIDDEN = listOf(
            "2023-03-02",
            "2023.03.02",
            "2023. 3. 2",
            "2023년 3월 2일",
            "3월 2일",
            "입사",
            MEMO,
            MEMO_2,
            "KE1234",
        )
    }
}
