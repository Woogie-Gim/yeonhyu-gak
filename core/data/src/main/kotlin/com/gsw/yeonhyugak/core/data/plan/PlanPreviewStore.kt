package com.gsw.yeonhyugak.core.data.plan

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 담기 전 미리보기. focus = 달력이 열 날짜
data class PlanPreview(val leaveDates: Set<LocalDate>, val focus: LocalDate)

// 추천 화면이 쓰고 달력이 읽는다. 메모리에만 두고 저장하지 않는다
@Singleton
class PlanPreviewStore @Inject constructor() {
    private val current = MutableStateFlow<PlanPreview?>(null)

    val preview: StateFlow<PlanPreview?> = current.asStateFlow()

    fun show(leaveDates: Collection<LocalDate>) {
        if (leaveDates.isEmpty()) return
        current.value = PlanPreview(leaveDates.toSet(), leaveDates.min())
    }

    fun clear() {
        current.value = null
    }
}
