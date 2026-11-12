package com.jinkim.hyundaiAndGretel.service

import android.content.pm.ServiceInfo
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState

internal // 리뷰용: 서비스 종료 여부만 계산하는 함수다. 알림 제거와 stopSelf 실행은 서비스 본문에서 담당한다.
fun shouldStopRoadNoteService(status: TrackingStatus, submission: SubmissionState, receiptWritesPending: Boolean = false): Boolean =
    !receiptWritesPending && !status.isCollecting && submission != SubmissionState.Submitting

internal // 리뷰용: 수집 중에는 location, 수집을 멈추고 저장만 유지할 때는 dataSync 유형을 선택한다.
fun foregroundServiceType(collecting: Boolean, sdkInt: Int): Int =
    when {
        sdkInt < 29 -> 0
        collecting -> ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        else -> ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    }
