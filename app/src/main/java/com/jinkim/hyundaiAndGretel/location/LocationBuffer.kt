package com.jinkim.hyundaiAndGretel.location

import kotlin.math.abs
import java.util.concurrent.TimeUnit

data class LocationBufferSnapshot(
    val sampleCount: Int,
    val oldestReceivedElapsedMillis: Long?,
    val latestReceivedElapsedMillis: Long?,
)

data class LocationAnchor(val sample: LocationFix, val selectedElapsedMillis: Long) {
    val offsetMillis get() = selectedElapsedMillis - sample.receivedElapsedMillis
}

sealed interface AnchorSelection {
    data class Selected(val anchor: LocationAnchor) : AnchorSelection
    enum class Unavailable : AnchorSelection { INVALID_TIME, EMPTY, STALE, NO_TARGET_SAMPLE }
}

/** 위치 수집 실행 문맥에서만 접근한다. 시각은 콜백 수신 시점의 단조시계 밀리초다. */
class LocationBuffer {
    enum class Update { ACCEPTED, INVALIDATED, IGNORED }

    private val samples = ArrayDeque<LocationFix>()
    // 표본 삭제·무효화 뒤에도 마지막 이벤트 시각을 유지하여 늦은 콜백이 과거 위치를 되살리지 못하게 한다.
    private var lastEventElapsedMillis = -1L
    val size get() = samples.size
    val oldestElapsedMillis get() = samples.firstOrNull()?.receivedElapsedMillis
    val latestElapsedMillis get() = samples.lastOrNull()?.receivedElapsedMillis

    // 리뷰용: 입력 처리: 시각 역전·중복은 무시하고, 무효 위치는 기존 표본을 비운다. 정상 좌표만 최근 45초 창에 보관한다.
    fun offer(fix: LocationFix?, receivedElapsedMillis: Long, nowElapsedMillis: Long): Update {
        if (receivedElapsedMillis < 0 || receivedElapsedMillis > nowElapsedMillis || receivedElapsedMillis < lastEventElapsedMillis) return Update.IGNORED
        if (fix != null && fix.receivedElapsedMillis != receivedElapsedMillis) return Update.IGNORED
        if (fix == null || !fix.hasValidCoordinates()) {
            lastEventElapsedMillis = receivedElapsedMillis
            samples.clear()
            return Update.INVALIDATED
        }
        if (receivedElapsedMillis == lastEventElapsedMillis) return Update.IGNORED
        lastEventElapsedMillis = receivedElapsedMillis
        prune(nowElapsedMillis)
        if (nowElapsedMillis - receivedElapsedMillis > RETENTION_MILLIS) return Update.IGNORED
        samples.addLast(fix)
        return Update.ACCEPTED
    }

    // 리뷰용: 제보 시 현재 단조시각에서 30초 전과 가장 가까운 표본을 선택한다. 이력이 짧으면 가장 오래된 표본을 사용한다.
    fun select(now: Long): AnchorSelection {
        if (now < 0 || now < lastEventElapsedMillis) return AnchorSelection.Unavailable.INVALID_TIME
        prune(now)
        val oldest = samples.firstOrNull() ?: return AnchorSelection.Unavailable.EMPTY
        val latest = samples.last()
        if (now - latest.receivedElapsedMillis > FRESHNESS_MILLIS) return AnchorSelection.Unavailable.STALE
        val historyCoversTarget = now - oldest.receivedElapsedMillis >= TARGET_MILLIS
        val targetElapsedMillis = now - TARGET_MILLIS
        val sample = when {
            !historyCoversTarget -> oldest
            else -> samples.minWith(
                compareBy<LocationFix> { abs(it.receivedElapsedMillis - targetElapsedMillis) }
                    .thenBy { it.receivedElapsedMillis },
            )
        }
        // 리뷰용: 30초 이력이 충분한 경우에만 목표 시각과의 5초 허용 오차를 검사한다. 최신 위치는 별도로 5초 이내여야 한다.
        val targetErrorMillis = abs(sample.receivedElapsedMillis - targetElapsedMillis)
        return when {
            historyCoversTarget && targetErrorMillis > TARGET_TOLERANCE_MILLIS -> AnchorSelection.Unavailable.NO_TARGET_SAMPLE
            else -> AnchorSelection.Selected(LocationAnchor(sample, now))
        }
    }

    fun reset() {
        samples.clear()
        lastEventElapsedMillis = -1L
    }

    // 리뷰용: 오래된 표본만 앞에서 제거한다. DB에 저장하지 않는 서비스 메모리 버퍼다.
    private fun prune(now: Long) {
        while (samples.isNotEmpty() && now - samples.first().receivedElapsedMillis > RETENTION_MILLIS) samples.removeFirst()
    }

    private companion object {
        val RETENTION_MILLIS = TimeUnit.SECONDS.toMillis(45)
        val FRESHNESS_MILLIS = TimeUnit.SECONDS.toMillis(5)
        val TARGET_MILLIS = TimeUnit.SECONDS.toMillis(30)
        val TARGET_TOLERANCE_MILLIS = TimeUnit.SECONDS.toMillis(5)
    }
}

private fun LocationFix.hasValidCoordinates() = latitude.isFinite() && longitude.isFinite() &&
    latitude in -90.0..90.0 && longitude in -180.0..180.0
