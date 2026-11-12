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
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class RoadNoteService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> requestStart()
            ACTION_STOP -> { demo.stop(); stopTracking() }
            ACTION_STORAGE_HOLD -> finishWhenStorageIsIdle()

            else -> { demo.stop(); stopTracking() }
        }
        return START_NOT_STICKY
    }

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

    private val receipts: NoteReceiptProcessor by lazy {
        NoteReceiptProcessor(
            scope = scope,
            store = RoomReceiptStore(this, onError = { error ->
                if (BuildConfig.DEBUG) Log.d("RoadNote", "RECEIPT_INSERT ${error.javaClass.simpleName}")
            }),
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

    private val demoDelegate: Lazy<DemoRuntime> = lazy {
        DemoRuntime(context = this, scope = scope, host = object : DemoServiceContract {
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
        }, store = scenarioStore, active = activeScenario, restrictions = restrictions.state)
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

    inner class LocalBinder : Binder(), RoadNoteServiceContract {
        override val status get() = tracking.status
        override val submissionState get() = submissions.state
        override val inboxState get() = receipts.inbox
        override val pickupState get() = pickup.state
        override val uiRestrictions get() = restrictions.state
        override val canStartTracking get() = !demo.enabled || (restrictions.state.value.canSetup && !demo.transitioning)
        val demoRuntime get() = demo
        fun bufferSnapshot() = tracking.bufferSnapshot()
        override fun confirmReceipt(key: ReceiptKey, screenResumed: Boolean, visitedContent: Set<String>) {
            if (!demo.blocksNewWrites) receipts.confirmVisibleReceipt(key, screenResumed, restrictions.state.value, visitedContent)
        }

        override fun submitRoadNote(content: String, category: NoteCategory) {
            submitRoadNoteWithResult(content, category, SystemClock.elapsedRealtime())
        }

        fun submitRoadNoteWithResult(content: String, category: NoteCategory, receivedElapsedMillis: Long): Deferred<SubmissionState> {
            if (submissions.isSubmitting) return CompletableDeferred(SubmissionState.Failed(SubmissionFailure.BUSY))
            if (!isTracking || (demo.enabled && (!demo.canSubmit || demo.blocksNewWrites)))
                return CompletableDeferred(SubmissionState.Failed(SubmissionFailure.LOCATION_UNAVAILABLE))
            return submissions.submit(content, category, tracking.selectAnchorAt(receivedElapsedMillis))
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        try {
            try { if (demoDelegate.isInitialized()) demo.close() }
            catch (error: Exception) { if (BuildConfig.DEBUG) Log.d("RoadNote", "DEMO_CLEANUP ${error.javaClass.simpleName}") }
            try { restrictions.close() }
            catch (error: Exception) { if (BuildConfig.DEBUG) Log.d("RoadNote", "UX_CLEANUP ${error.javaClass.simpleName}") }
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
        const val ACTION_START = "com.jinkim.hyundaiAndGretel.START_LOCATION"
        const val ACTION_STOP = "com.jinkim.hyundaiAndGretel.STOP_LOCATION"
        private const val ACTION_STORAGE_HOLD = "com.jinkim.hyundaiAndGretel.HOLD_NOTE_WRITES"
    }
}
