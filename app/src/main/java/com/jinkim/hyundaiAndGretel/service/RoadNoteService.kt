package com.jinkim.hyundaiAndGretel.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import com.jinkim.hyundaiAndGretel.BuildConfig
import com.jinkim.hyundaiAndGretel.demo.*
import com.jinkim.hyundaiAndGretel.location.*
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.pickup.*
import com.jinkim.hyundaiAndGretel.notes.spatial.H3SpatialIndex
import com.jinkim.hyundaiAndGretel.notes.storage.RoomReceiptStore
import com.jinkim.hyundaiAndGretel.notes.storage.RoomRoadNoteStore
import com.jinkim.hyundaiAndGretel.notes.storage.RoomScenarioStore
import com.jinkim.hyundaiAndGretel.notes.submit.RoadNoteSubmission
import com.jinkim.hyundaiAndGretel.ui.ux.CarRestrictionsSource
import com.jinkim.hyundaiAndGretel.ui.ux.confirmVisibleReceipt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class RoadNoteService : Service() {
    // 리뷰용: 서비스 명령 진입점: 시작·중지·저장 유지 명령을 구분한다. 이후 기능 판단은 각 처리자에게 위임한다.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> requestStart()
            ACTION_STOP -> { demo.stop(); stopTracking() }
            ACTION_STORAGE_HOLD -> finishWhenStorageIsIdle()
            // 명령이 없거나 알 수 없는 경우에도 기존처럼 수집을 중지한다.
            else -> { demo.stop(); stopTracking() }
        }
        return START_NOT_STICKY
    }

    // 리뷰용: 디버그에서는 시연 준비 절차를 먼저 거친다. 릴리스에서는 실제 위치 수집을 바로 시작한다.
    private fun requestStart() {
        if (!demo.enabled) { startTracking(); return }
        try {
            if (restrictions.state.value.canSetup && !submissions.isSubmitting && !receipts.hasPendingWrites &&
                !demo.transitioning && activeScenario.value != null) {
                ServiceCompat.startForeground(this, 1, notifications.build(),
                    foregroundServiceType(collecting = true, sdkInt = Build.VERSION.SDK_INT))
            }
            demo.requestStart()
            finishWhenStorageIsIdle()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { tracking.fail(error, "DEMO_FOREGROUND") }
    }

    // 리뷰용: 알림으로 포그라운드 전환 → 위치 SDK 시작 → 같은 세션의 쪽지 접근 감시 시작 순서를 지킨다.
    private fun startTracking() {
        if (isTracking || submissions.isSubmitting || receipts.hasPendingWrites) return
        try {
            val notification = notifications.build()
            val serviceType = foregroundServiceType(collecting = true, sdkInt = Build.VERSION.SDK_INT)
            ServiceCompat.startForeground(this, 1, notification, serviceType)
            candidates
            tracking.start()
            tracking.proximitySource?.let { pickup.start(tracking.sessionId, it, setup) }
        } catch (cancelled: CancellationException) {
            tracking.stop()
            finishWhenStorageIsIdle()
            throw cancelled
        } catch (error: Exception) {
            tracking.fail(error, operation = "FOREGROUND_START")
        }
    }

    private fun stopReceiving() {
        if (pickupDelegate.isInitialized()) pickup.stop()
    }

    // 리뷰용: 근접 등록을 먼저 정리하고 위치 SDK를 해제한다. 이미 수락한 DB 쓰기는 여기서 취소하지 않는다.
    private fun stopTracking() {
        try {
            stopReceiving()
        } finally {
            try { tracking.stop() }
            finally { finishWhenStorageIsIdle() }
        }
    }

    // 리뷰용: 종료 판단: 쓰기가 남으면 서비스를 유지하고, 수집·전환·쓰기 모두 끝났을 때만 종료를 요청한다.
    private fun finishWhenStorageIsIdle() {
        if (submissions.isSubmitting || receipts.hasPendingWrites || (demoDelegate.isInitialized() && demo.hasPendingWrites)) {
            if (isTracking) {
                getSystemService(NotificationManager::class.java).notify(1, notifications.build())
            } else {
                try { promoteStorageForeground() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    // 포그라운드 유형 전환에 실패해도 이미 수락한 쓰기는 완료까지 유지한다.
                    if (BuildConfig.DEBUG) Log.d("RoadNote", "STORAGE_FOREGROUND ${error.javaClass.simpleName}")
                }
            }
        } else if (!(demoDelegate.isInitialized() && demo.transitioning) && shouldStopRoadNoteService(tracking.status.value, submissions.state.value, receipts.hasPendingWrites)) {
            stopForegroundAndRequestServiceStop()
        }
    }

    // 리뷰용: 수집이 멈춰 있어도 수신 확인 등의 쓰기를 수락하기 전에 저장용 포그라운드 상태를 확보한다.
    private fun holdStorageForeground() {
        if (isTracking) return
        // 화면 연결이 해제돼도 수락한 확인 저장을 끝낼 수 있도록 서비스를 시작 상태로 유지한다.
        promoteStorageForeground()
        try {
            startService(Intent(this, RoadNoteService::class.java).setAction(ACTION_STORAGE_HOLD))
        } catch (error: RuntimeException) {
            if (!submissions.isSubmitting && !receipts.hasPendingWrites && !(demoDelegate.isInitialized() && demo.hasPendingWrites)) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            }
            throw error
        }
    }

    private fun promoteStorageForeground() {
        ServiceCompat.startForeground(this, 1, notifications.build(savingOnly = true),
            foregroundServiceType(collecting = false, sdkInt = Build.VERSION.SDK_INT))
    }

    private fun stopForegroundAndRequestServiceStop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // 리뷰용: 기능 연결 지점: 서비스 수명의 코루틴, 실제 SDK 소스, Room 저장소를 생성해 처리자들에게 전달한다.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val activeScenario = MutableStateFlow<String?>(null)
    private val scenarioStore by lazy { RoomScenarioStore(this) }
    private val restrictions = CarRestrictionsSource(scope)
    private val tracking = LocationTracking(
        scope = scope,
        createSource = { PleosLocationSource(this) },
        log = { message ->
            if (BuildConfig.DEBUG) Log.d("RoadLocation", message)
        },
        onFailure = {
            if (demoDelegate.isInitialized()) demo.onSourceFailure()
            stopReceiving(); finishWhenStorageIsIdle()
        },
        clock = SystemClock::elapsedRealtime,
        acceptLocation = { demo.accept(it) },
    )
    private val spatial = H3SpatialIndex()
    private val noteStore by lazy {
        RoomRoadNoteStore(this, spatial, scenario = activeScenario) { error ->
            if (BuildConfig.DEBUG) Log.d("RoadNote", "H3_CREATE_ERROR ${error.javaClass.simpleName}")
        }
    }
    private val candidates by lazy {
        NearbyNoteCandidates(scope, tracking.currentPosition, spatial, noteStore,
            elapsedMillis = SystemClock::elapsedRealtime, utcMillis = System::currentTimeMillis,
            onFailure = { operation, error ->
                if (BuildConfig.DEBUG) Log.d("RoadNote", "$operation ${error.javaClass.simpleName}")
            })
    }
    private var setup = SyntheticSession()
    private val recipient = MutableStateFlow(setup.actorId)
    // 리뷰용: 수신·확인 쓰기 수락 시 역할 전환 여부와 서비스 유지 가능성을 확인한다. DB 구현은 RoomReceiptStore다.
    private val receipts: NoteReceiptProcessor by lazy {
        NoteReceiptProcessor(scope, RoomReceiptStore(this, onError = { error ->
            if (BuildConfig.DEBUG) Log.d("RoadNote", "RECEIPT_INSERT ${error.javaClass.simpleName}")
        }), recipient, ::finishWhenStorageIsIdle,
            onWriteAccepted = {
                check(!demo.blocksNewWrites) { "시연 전환 중입니다" }
                holdStorageForeground()
            })
    }
    private val pickupDelegate = lazy {
        ProximityPickup(scope, tracking.currentPosition, candidates.state, candidates::currentCandidates, receipts,
            tracking::ownsSource, SystemClock::elapsedRealtime, System::currentTimeMillis,
            onError = { operation, error ->
                if (BuildConfig.DEBUG) Log.d("RoadNote", "$operation ${error.javaClass.simpleName}")
            }, scenarioId = { activeScenario.value })
    }
    private val pickup by pickupDelegate

    private val submissions: RoadNoteSubmission by lazy {
        RoadNoteSubmission(
            scope = scope,
            store = noteStore,
            captureAnchor = tracking::selectAnchor,
            utcMillis = System::currentTimeMillis,
            onFinished = ::finishWhenStorageIsIdle,
            setup = { setup },
            scenarioId = { activeScenario.value },
            onUnexpectedError = { error ->
                if (BuildConfig.DEBUG) Log.d("RoadNote", "STORE_UNEXPECTED_ERROR ${error.javaClass.simpleName}")
            },
        )
    }
    // 리뷰용: 디버그 시연은 이 계약을 통해 기존 수집·역할·쓰기 수명을 조작한다. 별도 위치 수집기를 만들지 않는다.
    private val demoDelegate: Lazy<DemoRuntime> = lazy {
        DemoRuntime(this, scope, object : DemoServiceContract {
            override val busy get() = submissions.isSubmitting || receipts.hasPendingWrites
            override fun startTracking() = this@RoadNoteService.startTracking()
            override fun stopTracking() = this@RoadNoteService.stopTracking()
            override fun changeActor(actor: String) {
                setup = SyntheticSession(actor)
                recipient.value = actor
                receipts.refreshObservation()
                submissions.clearResult()
            }
            override fun currentFix() = tracking.currentPosition.value?.fix
            override fun holdStorage() = holdStorageForeground()
            override fun writesFinished() = finishWhenStorageIsIdle()
        }, scenarioStore, activeScenario, restrictions.state)
    }
    private val demo: DemoRuntime by demoDelegate

    override fun onCreate() {
        super.onCreate()
        restrictions.start(this)
        scope.launch {
            try { scenarioStore.observeActive().collect { activeScenario.value = it } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (BuildConfig.DEBUG) Log.d("RoadNote", "SCENARIO_READ ${error.javaClass.simpleName}") }
        }
    }

    private val isTracking get() = tracking.status.value.isCollecting
    private val notifications by lazy { RoadNoteServiceNotification(this) }
    private val binder = LocalBinder()

    // 리뷰용: 화면이 접근하는 실제 연결 객체다. 상태를 공개하고 제보·확인 요청을 해당 처리자에게 전달한다.
    inner class LocalBinder : Binder(), RoadNoteServiceContract {
        override val status get() = tracking.status
        override val submissionState get() = submissions.state
        override val inboxState get() = receipts.inbox
        override val pickupState get() = pickup.state
        override val uiRestrictions get() = restrictions.state
        val demoRuntime get() = demo
        fun bufferSummary() = tracking.bufferSummary()
        override fun confirmReceipt(key: ReceiptKey, screenResumed: Boolean, visitedContent: Set<String>) {
            if (!demo.blocksNewWrites) receipts.confirmVisibleReceipt(key, screenResumed, restrictions.state.value, visitedContent)
        }

        override fun submitTestNote(content: String, category: NoteCategory) {
            if (BuildConfig.DEBUG && demo.canSubmit && !demo.blocksNewWrites) submissions.submit(content, category)
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    // 리뷰용: 서비스 최종 해제: 시연 입력·차량 규제·근접 감시·위치 SDK를 정리한 뒤 코루틴을 취소한다.
    override fun onDestroy() {
        try {
            try { if (demoDelegate.isInitialized()) demo.close() }
            catch (error: Exception) { if (BuildConfig.DEBUG) Log.d("RoadNote", "DEMO_CLEANUP ${error.javaClass.simpleName}") }
            try { restrictions.close() }
            catch (error: Exception) { if (BuildConfig.DEBUG) Log.d("RoadNote", "UX_CLEANUP ${error.javaClass.simpleName}") }
            stopReceiving()
        } finally {
            try { tracking.stop() }
            finally {
                scope.cancel()
                super.onDestroy()
            }
        }
    }

    companion object {
        const val ACTION_START = "com.jinkim.hyundaiAndGretel.START_LOCATION"
        const val ACTION_STOP = "com.jinkim.hyundaiAndGretel.STOP_LOCATION"
        private const val ACTION_STORAGE_HOLD = "com.jinkim.hyundaiAndGretel.HOLD_NOTE_WRITES"
    }
}
