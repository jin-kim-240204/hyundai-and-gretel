package com.jinkim.hyundaiAndGretel.gleo

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import com.jinkim.hyundaiAndGretel.roadnote.LocalDraftStore
import com.jinkim.hyundaiAndGretel.roadnote.LocationMode
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

// Platform-only runtime self-check: isolated storage, no AndroidX test dependency or real report data.
class DraftIntegrationInstrumentation : Instrumentation() {
    private var phase: String? = null

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        phase = arguments?.getString("phase")
        start()
    }

    override fun onStart() {
        val result = Bundle()
        val directory = File(targetContext.cacheDir, if (phase != null) "draft-process-probe" else "draft-probe-${UUID.randomUUID()}")
        val existed = directory.exists()
        var keepForRestart = false
        directory.mkdirs()
        val isolated = object : ContextWrapper(targetContext) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir(): File = directory
        }
        try {
            runBlocking {
                val store = LocalDraftStore(isolated)
                if (phase == "seed") {
                    check(!existed)
                    store.setConsent(true)
                    val now = Instant.now()
                    val parsed = parseGleoAction("""{"content":"synthetic restart check","locationMode":"DEFERRED"}""", now)
                    check(GleoActionProcessor(store).process(parsed, now).status == GleoStatus.SUCCESS)
                    keepForRestart = true
                    result.putString("stream", "Synthetic draft saved for process restart check.\n")
                    return@runBlocking
                }
                if (phase == "recover") {
                    check(existed && store.hasConsent())
                    val draft = store.list().single()
                    check(draft.input.content == "synthetic restart check")
                    check(store.delete(draft.id))
                    store.setConsent(false)
                    check(!store.hasConsent() && store.list().isEmpty())
                    result.putString("stream", "Process restart recovery and revocation passed.\n")
                    return@runBlocking
                }
                check(phase == null)
                check(!store.hasConsent())
                val now = Instant.now()
                val raw = """{"content":"synthetic runtime check","locationMode":"CURRENT_ROAD"}"""
                val parsed = parseGleoAction(raw, now)
                check(parsed is ParsedGleoAction.Valid)
                check(parseGleoAction("[]", now) is ParsedGleoAction.Rejected)
                check(parseGleoAction("{} trailing", now) is ParsedGleoAction.Rejected)
                check(parseGleoAction("a".repeat(8193), now) is ParsedGleoAction.Rejected)
                check(GleoActionProcessor(store).process(parsed, now).reason == "CONSENT_REQUIRED")
                store.setConsent(true)
                val saved = GleoActionProcessor(store).process(parsed, now)
                check(saved.status == GleoStatus.SUCCESS)
                val callback = JSONObject(saved.toCallbackJson())
                check(callback.getString("status") == "success")
                check(!JSONArray(callback.getString("data")).getJSONObject(0).getBoolean("published"))
                val restored = LocalDraftStore(isolated).list().single()
                check(restored.input.locationMode == LocationMode.CURRENT_ROAD)
                check(!File(directory, "road-note-drafts.bin").readBytes()
                    .toString(Charsets.ISO_8859_1).contains("synthetic runtime check"))
                check(store.delete(restored.id))
                check(LocalDraftStore(isolated).list().isEmpty())
                store.setConsent(false)
                check(!LocalDraftStore(isolated).hasConsent())
                result.putString("storage", "PASS: Keystore/GCM, parse, callback, reopen, delete, revoke")
            }
            if (phase == null) {
                checkReceiver(result)
                result.putString("stream", "Draft runtime self-check passed.\n")
            }
            finish(Activity.RESULT_OK, result)
        } catch (error: Throwable) {
            result.putString("stream", "Draft runtime self-check failed: ${error.javaClass.simpleName}\n")
            finish(Activity.RESULT_CANCELED, result)
        } finally {
            if (!keepForRestart) directory.deleteRecursively()
        }
    }

    private fun checkReceiver(result: Bundle) {
        GleoProbe.clear()
        targetContext.sendBroadcast(
            Intent(CREATE_ROAD_NOTE_DRAFT)
                .setClass(targetContext, GleoActionReceiver::class.java)
                .putExtra("ACTION_PARAM", "[]"),
        )
        runBlocking {
            val event = kotlinx.coroutines.withTimeout(5_000) {
                GleoProbe.events.first { events -> events.any { it.callback != "아직 없음" } }.first()
            }
            check(JSONObject(event.callback).getString("status") == "fail")
        }
        result.putString("receiver", "PASS: debug receive, validation, observed callback request")
    }
}
