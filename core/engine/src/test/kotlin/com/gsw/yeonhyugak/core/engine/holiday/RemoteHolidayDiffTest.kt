package com.gsw.yeonhyugak.core.engine.holiday

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class RemoteHolidayDiffTest :
    FunSpec({
        val election = Holiday(
            LocalDate.of(2028, 4, 12),
            "국회의원선거일",
            HolidayKind.ELECTION,
            true,
            HolidaySource.REMOTE,
        )
        val temp = Holiday(
            LocalDate.of(2028, 10, 2),
            "임시공휴일",
            HolidayKind.TEMPORARY,
            true,
            HolidaySource.REMOTE,
        )
        val v1 = RemoteHolidays(1, listOf(election))
        val v2 = RemoteHolidays(2, listOf(election, temp))

        test("처음 받은 파일은 전부 새 날") {
            RemoteHolidayDiff.newDays(null, v1) shouldContainExactly listOf(election)
        }

        test("이전에 없던 날만 새 날") {
            RemoteHolidayDiff.newDays(v1, v2) shouldContainExactly listOf(temp)
        }

        test("개정 번호·내용이 같으면 같은 파일") {
            RemoteHolidayDiff.isSame(v1, v1.copy()) shouldBe true
            RemoteHolidayDiff.isSame(v1, v2) shouldBe false
            RemoteHolidayDiff.isSame(null, v1) shouldBe false
        }
    })
