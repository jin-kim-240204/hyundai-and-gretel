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
import com.jinkim.hyundaiAndGretel.speech.AnnouncementOwner
import com.jinkim.hyundaiAndGretel.speech.CarAudioPower
import com.jinkim.hyundaiAndGretel.speech.PleosNoteSpeaker
import com.jinkim.hyundaiAndGretel.speech.ReceiptAnnouncement
import com.jinkim.hyundaiAndGretel.speech.speechFailureCode
import kotlinx.coroutines.Deferred
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionFailure
import com.jinkim.hyundaiAndGretel.notes.submit.RoadNoteSubmission
import com.jinkim.hyundaiAndGretel.ui.ux.CarRestrictionsSource
import com.jinkim.hyundaiAndGretel.ui.ux.confirmVisibleReceipt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class RoadNoteService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> { automaticTrackingPaused = false; requestStart() }
            ACTION_STOP -> {
                automaticTrackingPaused = intent.getBooleanExtra(EXTRA_PAUSE_AUTOMATIC, true)
                demo.stop()
                stopTracking()
            }
            ACTION_STORAGE_HOLD -> finishWhenStorageIsIdle()

            else -> { demo.stop(); stopTracking() }
        }
        return START_NOT_STICKY
    }

    private var automaticTrackingPaused = false

    private fun requestStart() {
        if (!demo.enabled) { startTracking(); return }
        try {

            ServiceCompat.startForeground(this, 1, notifications.build(),
                foregroundServiceType(collecting = true, sdkInt = Build.VERSION.SDK_INT))
            demo.requestStart()
            finishWhenStorageIsIdle()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { tracking.fail(error, "DEMO_FOREGROUND") }
    }

    private fun startTracking() {
        if (isTracking || submissions.isSubmitting || receipts.hasPendingWrites) return
        try {
            val notification = notifications.build()
            val serviceType = foregroundServiceType(collecting = true, sdkInt = Build.VERSION.SDK_INT)
            ServiceCompat.startForeground(this, 1, notification, serviceType)
            tracking.start()
            startNotePickup()
        } catch (cancelled: CancellationException) {
            tracking.stop()
            finishWhenStorageIsIdle()
            throw cancelled
        } catch (error: Exception) {
            tracking.fail(error, operation = "FOREGROUND_START")
        }
    }

    private fun startNotePickup() {
        val source = tracking.proximitySource ?: return
        pickup.start(session = tracking.sessionId, source = source, setup = setup)
    }

    private fun stopNotePickup() {
        if (announcementsDelegate.isInitialized()) {
            logSpeech("RESET_TRACKING")
            announcements.reset()
        }
        if (pickupDelegate.isInitialized()) pickup.stop()
    }

    private fun stopTracking() {
        try {
            stopNotePickup()
        } finally {
            try { tracking.stop() }
            finally { finishWhenStorageIsIdle() }
        }
    }

    private fun finishWhenStorageIsIdle() {
        if (submissions.isSubmitting || receipts.hasPendingWrites || (demoDelegate.isInitialized() && demo.hasPendingWrites)) {
            if (isTracking) {
                getSystemService(NotificationManager::class.java).notify(1, notifications.build())
            } else {
                try { promoteStorageForeground() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {

                    if (BuildConfig.DEBUG) Log.d("RoadNote", "STORAGE_FOREGROUND ${error.javaClass.simpleName}")
                }
            }
        } else if (!(demoDelegate.isInitialized() && demo.transitioning) && shouldStopRoadNoteService(tracking.status.value, submissions.state.value, receipts.hasPendingWrites)) {
            stopForegroundAndRequestServiceStop()
        }
    }

    private fun holdStorageForeground() {
        if (isTracking) return

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

    private fun logSpeech(event: String) {
        if (BuildConfig.DEBUG) Log.d("RoadSpeech", event)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val audioPower: CarAudioPower = CarAudioPower(scope, ::logSpeech) {
        logSpeech("RESET_POWER")
        if (announcementsDelegate.isInitialized()) announcements.reset()
    }
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
            stopNotePickup(); finishWhenStorageIsIdle()
        },
        clock = SystemClock::elapsedRealtime,
        acceptLocation = { demo.accept(it) },
    )
    private val spatial = H3SpatialIndex()
    private val noteStore by lazy {
        RoomRoadNoteStore(context = this, spatial = spatial, scenario = activeScenario) { error ->
            if (BuildConfig.DEBUG) Log.d("RoadNote", "H3_CREATE_ERROR ${error.javaClass.simpleName}")
        }
    }
    private val candidates by lazy {
        NearbyNoteCandidates(
            scope = scope,
            positions = tracking.currentPosition,
            spatial = spatial,
            store = noteStore,
            elapsedMillis = SystemClock::elapsedRealtime,
            utcMillis = System::currentTimeMillis,
            onFailure = { operation, error ->
                if (BuildConfig.DEBUG) Log.d("RoadNote", "$operation ${error.javaClass.simpleName}")
            })
    }
    private var setup = SyntheticSession()
    private val recipient = MutableStateFlow(setup.actorId)

    private val receiptStore by lazy {
        RoomReceiptStore(this, onError = { error ->
            if (BuildConfig.DEBUG) Log.d("RoadNote", "RECEIPT_INSERT ${error.javaClass.simpleName}")
        })
    }
    private val announcementsDelegate: Lazy<ReceiptAnnouncement> = lazy {
        ReceiptAnnouncement(
            scope = scope,
            store = receiptStore,
            speaker = PleosNoteSpeaker(this, ::logSpeech),
            currentOwner = {
                if (isTracking && audioPower.enabled && !demo.blocksNewWrites) {
                    AnnouncementOwner(recipient.value, activeScenario.value, tracking.sessionId)
                } else null
            },
            utcMillis = System::currentTimeMillis,
            canReadBody = { restrictions.state.value.connected && restrictions.state.value.canShowBody },
            onFailure = { error ->
                logSpeech("ANNOUNCEMENT_FAILED ${speechFailureCode(error)}")
            },
            log = ::logSpeech,
        )
    }
    private val announcements: ReceiptAnnouncement by announcementsDelegate

    private val receipts: NoteReceiptProcessor by lazy {
        NoteReceiptProcessor(
            scope = scope,
            store = receiptStore,
            recipient = recipient,
            onFinished = ::finishWhenStorageIsIdle,
            onWriteAccepted = {
                check(!demo.blocksNewWrites) { "시연 전환 중입니다" }
                holdStorageForeground()
            })
    }
    private val pickupDelegate = lazy {
        ProximityPickup(
            scope = scope,
            positions = tracking.currentPosition,
            candidateSignals = candidates.state,
            currentCandidates = candidates::currentCandidates,
            receipts = receipts,
            ownsSource = tracking::ownsSource,
            elapsedMillis = SystemClock::elapsedRealtime,
            utcMillis = System::currentTimeMillis,
            onError = { operation, error ->
                if (BuildConfig.DEBUG) Log.d("RoadNote", "$operation ${error.javaClass.simpleName}")
            }, scenarioId = { activeScenario.value },
            onReceived = { request ->
                logSpeech("RECEIPT_COMMITTED tracking=$isTracking audio=${audioPower.enabled} transition=${demo.blocksNewWrites}")
                announcements.enqueue(request)
            })
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

    private val demoDelegate: Lazy<DemoRuntime> = lazy {
        DemoRuntime(context = this, scope = scope, host = object : DemoServiceContract {
            override val busy get() = submissions.isSubmitting || receipts.hasPendingWrites
            override fun startTracking() = this@RoadNoteService.startTracking()
            override fun stopTracking() = this@RoadNoteService.stopTracking()
            override fun changeActor(actor: String) {
                if (announcementsDelegate.isInitialized()) announcements.reset()
                setup = SyntheticSession(actor)
                recipient.value = actor
                receipts.refreshObservation()
                submissions.clearResult()
            }
            override fun currentFix() = tracking.currentPosition.value?.fix
            override fun holdStorage() = holdStorageForeground()
            override fun writesFinished() = finishWhenStorageIsIdle()
        }, store = scenarioStore, active = activeScenario, restrictions = restrictions.state)
    }
    private val demo: DemoRuntime by demoDelegate

    override fun onCreate() {
        super.onCreate()
        restrictions.start(this)
        audioPower.start(this)
        scope.launch {
            restrictions.state.map { it.connected && it.canShowBody }.distinctUntilChanged().collect {
                if (announcementsDelegate.isInitialized()) {
                    logSpeech("RESET_BODY_POLICY")
                    announcements.reset()
                }
            }
        }
        scope.launch {
            try {
                if (demo.enabled) {
                    restrictions.state.first { it.canSetup }
                    scenarioStore.ensureActive()
                }
                scenarioStore.observeActive().collect {
                if (activeScenario.value != it && announcementsDelegate.isInitialized()) announcements.reset()
                activeScenario.value = it
            } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (BuildConfig.DEBUG) Log.d("RoadNote", "SCENARIO_READ ${error.javaClass.simpleName}") }
        }
    }

    private val isTracking get() = tracking.status.value.isCollecting
    private val notifications by lazy { RoadNoteServiceNotification(this) }
    private val binder = LocalBinder()

    inner class LocalBinder : Binder(), RoadNoteServiceContract {
        override val status get() = tracking.status
        override val submissionState get() = submissions.state
        override val inboxState get() = receipts.inbox
        override val pickupState get() = pickup.state
        override val uiRestrictions get() = restrictions.state
        override val automaticStartBlockReason get() =
            if (automaticTrackingPaused) "사용자가 위치 수집을 중지했습니다" else demo.automaticStartBlockReason
        override val canStartTracking get() = !demo.enabled || (restrictions.state.value.canSetup && !demo.transitioning)
        val demoRuntime get() = demo
        fun bufferSnapshot() = tracking.bufferSnapshot()
        override fun confirmReceipt(key: ReceiptKey, screenResumed: Boolean, visitedContent: Set<String>) {
            if (!demo.blocksNewWrites) receipts.confirmVisibleReceipt(key, screenResumed, restrictions.state.value, visitedContent)
        }

        override fun submitRoadNote(content: String, category: NoteCategory) {
            submitRoadNoteWithResult(content, category, SystemClock.elapsedRealtime())
        }

        fun submitRoadNoteWithResult(
            content: String,
            category: NoteCategory,
            receivedElapsedMillis: Long,
            receiverGuidance: String? = null,
        ): Deferred<SubmissionState> {
            val rejection = submissionRejection(
                busy = submissions.isSubmitting,
                demoEnabled = demo.enabled,
                scenarioId = activeScenario.value,
                canSubmit = demo.canSubmit,
                blocksNewWrites = demo.blocksNewWrites,
                tracking = isTracking,
            )
            if (rejection != null) return submissions.reject(rejection)
            return submissions.submit(content, category, tracking.selectAnchorAt(receivedElapsedMillis), receiverGuidance)
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        try {
            try { if (demoDelegate.isInitialized()) demo.close() }
            catch (error: Exception) { if (BuildConfig.DEBUG) Log.d("RoadNote", "DEMO_CLEANUP ${error.javaClass.simpleName}") }
            try { restrictions.close() }
            catch (error: Exception) { if (BuildConfig.DEBUG) Log.d("RoadNote", "UX_CLEANUP ${error.javaClass.simpleName}") }
            try { audioPower.close() }
            catch (error: Exception) { if (BuildConfig.DEBUG) Log.d("RoadSpeech", "POWER_CLEANUP ${error.javaClass.simpleName}") }
            stopNotePickup()
        } finally {
            try { tracking.stop() }
            finally {
                scope.cancel()
                super.onDestroy()
            }
        }
    }

    companion object {
        const val EXTRA_PAUSE_AUTOMATIC = "pauseAutomaticTracking"
        const val ACTION_START = "com.jinkim.hyundaiAndGretel.START_LOCATION"
        const val ACTION_STOP = "com.jinkim.hyundaiAndGretel.STOP_LOCATION"
        private const val ACTION_STORAGE_HOLD = "com.jinkim.hyundaiAndGretel.HOLD_NOTE_WRITES"
    }
}

internal fun submissionRejection(
    busy: Boolean,
    demoEnabled: Boolean,
    scenarioId: String?,
    canSubmit: Boolean,
    blocksNewWrites: Boolean,
    tracking: Boolean,
): SubmissionFailure? = when {
    busy -> SubmissionFailure.BUSY
    !demoEnabled || scenarioId.isNullOrBlank() || !canSubmit || blocksNewWrites -> SubmissionFailure.DEMO_REQUIRED
    !tracking -> SubmissionFailure.LOCATION_UNAVAILABLE
    else -> null
}
