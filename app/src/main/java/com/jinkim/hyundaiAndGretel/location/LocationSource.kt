package com.jinkim.hyundaiAndGretel.location

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val receivedElapsedMillis: Long,
)

/** 좌표가 같아도 세션을 구분하여 과거 세션의 조회 결과가 다시 사용되지 않게 한다. */
data class TrackedPosition(val session: Long, val fix: LocationFix)

interface LocationSource : ProximitySource {
    /**
     * SDK 미지원일 때만 false를 반환한다. 초기화·등록 실패는 예외로 전달한다.
     * true는 등록 호출이 반환됐다는 뜻이며, 첫 위치 수신이나 SDK 연결 완료를 보장하지 않는다.
     * receivedElapsedMillis는 SDK 콜백 진입 시 기록한 단조시계 밀리초다. 좌표가 null인 무효화 이벤트에도 필요하다.
     */
    fun start(
        onLocation: (fix: LocationFix?, receivedElapsedMillis: Long) -> Unit,
        onError: (Exception) -> Unit,
    ): Boolean
    fun stop()
}
