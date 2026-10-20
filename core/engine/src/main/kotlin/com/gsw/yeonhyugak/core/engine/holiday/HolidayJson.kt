package com.gsw.yeonhyugak.core.engine.holiday

import com.gsw.yeonhyugak.core.engine.SupportedYears
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class HolidayDataException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

@Serializable
internal data class HolidayDto(
    val date: String,
    val name: String,
    val kind: HolidayKind = HolidayKind.NATIONAL,
)

@Serializable
internal data class BundleYearDto(
    val confirmed: Boolean,
    val holidays: List<HolidayDto> = emptyList(),
)

@Serializable
internal data class BundleDto(val schemaVersion: Int, val years: Map<String, BundleYearDto>)

@Serializable
internal data class LunarYearDto(val seollal: String, val buddha: String, val chuseok: String)

@Serializable
internal data class LunarTableDto(val schemaVersion: Int, val years: Map<String, LunarYearDto>)

// 원격 JSON. kind 필수, 번들에 더할 날만 (SPEC 3장)
@Serializable
internal data class RemoteHolidayDto(val date: String, val name: String, val kind: HolidayKind)

@Serializable
internal data class RemoteHolidaysDto(val version: Int, val holidays: List<RemoteHolidayDto>)

// 번들 확정 공휴일. 연도별 확정 여부
data class HolidayBundle(val confirmedYears: Map<Int, List<Holiday>>) {
    fun isConfirmed(year: Int): Boolean = year in confirmedYears
}

// 원격 공휴일 파일. version은 데이터 개정 번호
data class RemoteHolidays(val version: Int, val holidays: List<Holiday>) {
    fun toOverrides(): HolidayOverrides = HolidayOverrides(add = holidays)
}

// 원격 덮어쓰기: 임시공휴일·선거일 추가, 취소된 날 삭제
data class HolidayOverrides(
    val add: List<Holiday> = emptyList(),
    val remove: Set<Pair<LocalDate, String>> = emptySet(),
) {
    companion object {
        val NONE = HolidayOverrides()
    }
}

// 번들·원격 JSON 파싱과 검증
object HolidayBundleCodec {
    const val SCHEMA_VERSION = 1

    private val json = Json { ignoreUnknownKeys = true }

    fun parseBundle(text: String): HolidayBundle {
        val dto = decode<BundleDto>(text)
        checkSchema(dto.schemaVersion)
        val confirmed =
            dto.years
                .mapKeys { (key, _) -> parseYear(key) }
                .filterValues { it.confirmed }
                .mapValues { (year, y) ->
                    y.holidays.map {
                        it.toHoliday(year, HolidaySource.BUNDLED)
                    }.also { checkDuplicates(year, it) }
                }
        return HolidayBundle(confirmed)
    }

    fun parseLunarTable(text: String): LunarTable {
        val dto = decode<LunarTableDto>(text)
        checkSchema(dto.schemaVersion)
        val years =
            dto.years
                .mapKeys { (key, _) -> parseYear(key) }
                .mapValues { (year, y) ->
                    LunarDates(
                        seollal = parseDate(y.seollal, year),
                        buddha = parseDate(y.buddha, year),
                        chuseok = parseDate(y.chuseok, year),
                    )
                }
        return LunarTable(years)
    }

    // 규칙 하나라도 어기면 파일 전체 거부
    fun parseRemote(text: String): RemoteHolidays {
        val dto = decode<RemoteHolidaysDto>(text)
        check(dto.version >= 1) { "version은 1 이상: ${dto.version}" }
        val holidays = dto.holidays.map {
            HolidayDto(it.date, it.name, it.kind).toHoliday(null, HolidaySource.REMOTE)
        }
        val dup = holidays.groupBy { it.date to it.name }.filterValues { it.size > 1 }.keys
        check(dup.isEmpty()) { "원격 중복 공휴일: $dup" }
        return RemoteHolidays(dto.version, holidays.sortedWith(compareBy({ it.date }, { it.name })))
    }

    private inline fun <reified T> decode(text: String): T = try {
        json.decodeFromString<T>(text)
    } catch (e: SerializationException) {
        throw HolidayDataException("JSON 형식 오류: ${e.message}", e)
    } catch (e: IllegalArgumentException) {
        throw HolidayDataException("JSON 형식 오류: ${e.message}", e)
    }

    private fun checkSchema(version: Int) {
        if (version != SCHEMA_VERSION) throw HolidayDataException("지원하지 않는 schemaVersion $version")
    }

    private fun parseYear(key: String): Int {
        val year = key.toIntOrNull() ?: throw HolidayDataException("연도 형식 오류: $key")
        if (year !in SupportedYears.range) throw HolidayDataException("지원 범위 밖 연도: $year")
        return year
    }

    private fun parseDate(text: String, year: Int?): LocalDate {
        val date =
            try {
                LocalDate.parse(text)
            } catch (e: DateTimeParseException) {
                throw HolidayDataException("날짜 형식 오류: $text", e)
            }
        check(year == null || date.year == year) { "$year 연도에 다른 해 날짜: $text" }
        check(SupportedYears.contains(date)) { "지원 범위 밖 날짜: $text" }
        return date
    }

    private inline fun check(ok: Boolean, message: () -> String) {
        if (!ok) throw HolidayDataException(message())
    }

    private fun HolidayDto.toHoliday(year: Int?, source: HolidaySource): Holiday {
        if (name.isBlank()) throw HolidayDataException("이름 없는 공휴일: $date")
        return Holiday(parseDate(date, year), name, kind, confirmed = true, source = source)
    }

    private fun checkDuplicates(year: Int, holidays: List<Holiday>) {
        val dup = holidays.groupBy { it.date to it.name }.filterValues { it.size > 1 }.keys
        if (dup.isNotEmpty()) throw HolidayDataException("$year 중복 공휴일: $dup")
    }
}
