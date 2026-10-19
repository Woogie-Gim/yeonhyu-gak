package com.gsw.yeonhyugak.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

// 야구 테마: 구단명·마스코트·영문 SAFE를 앱 문구에 쓰지 않는다 (SPEC 7장)
class BaseballWordingTest {
    private val banned = listOf(
        "SAFE", "Safe",
        // KBO 구단 이름·연고 별칭
        "베어스", "트윈스", "히어로즈", "랜더스", "위즈", "다이노스", "타이거즈", "자이언츠", "라이온즈", "이글스",
        "두산", "키움", "롯데", "삼성", "한화", "기아", "KIA", "SSG",
    )

    // 테스트 작업 디렉터리는 :app 모듈
    private val repo = File("").absoluteFile.parentFile

    @Test
    fun strings_haveNoTeamNames() {
        val files = repo.walkTopDown()
            .onEnter { it.name != "build" && it.name != ".gradle" && !it.name.startsWith(".") }
            .filter { it.name == "strings.xml" }
            .toList()
        assertTrue("strings.xml을 찾지 못함: $repo", files.size >= 5)
        val hits = files.flatMap { f ->
            // 주석은 빼고 실제 문구만
            val text = f.readText().replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
            banned.filter { text.contains(it) }.map { "${f.path}: $it" }
        }
        assertTrue(hits.joinToString("\n"), hits.isEmpty())
    }
}
