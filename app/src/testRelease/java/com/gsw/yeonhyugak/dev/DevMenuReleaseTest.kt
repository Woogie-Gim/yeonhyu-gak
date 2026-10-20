package com.gsw.yeonhyugak.dev

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 릴리스 빌드에는 개발자 메뉴 코드·문구가 없다
// 실행: ./gradlew :app:testReleaseUnitTest --tests "*DevMenuReleaseTest*"
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class)
class DevMenuReleaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun entry_isNull() {
        assertNull(DevMenus.entry)
    }

    @Test
    fun debugClasses_notCompiledIn() {
        DEBUG_CLASSES.forEach { name ->
            try {
                Class.forName(name)
                fail("릴리스에 $name 포함")
            } catch (_: ClassNotFoundException) {
                // 기대한 결과
            }
        }
    }

    @Test
    fun debugStrings_notInResources() {
        DEBUG_STRINGS.forEach { name ->
            val id = context.resources.getIdentifier(name, "string", context.packageName)
            assertEquals("릴리스 리소스에 $name 포함", 0, id)
        }
    }

    private companion object {
        val DEBUG_CLASSES = listOf(
            "com.gsw.yeonhyugak.dev.DevMenuScreenKt",
            "com.gsw.yeonhyugak.dev.DevActions",
            "com.gsw.yeonhyugak.dev.DevEntryPoint",
            "com.gsw.yeonhyugak.dev.DebugDevMenu",
        )
        val DEBUG_STRINGS = listOf(
            "dev_menu_title",
            "dev_send_alerts",
            "dev_sync_holidays",
            "dev_refresh_widgets",
        )
    }
}
