package com.jinkim.hyundaiAndGretel.roadnote

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class DraftFileStore(
    private val file: File,
    private val now: () -> Instant = Instant::now,
    private val encode: (ByteArray) -> ByteArray = { it },
    private val decode: (ByteArray) -> ByteArray = { it },
    private val replace: (File, File) -> Unit = { source, target ->
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING)
        Unit
    },
) : DraftStore {
    private data class Snapshot(val consent: Boolean, val drafts: List<RoadNoteDraft>)

    override suspend fun hasConsent(): Boolean = locked { read().consent }

    override suspend fun setConsent(granted: Boolean) = locked {
        // Revocation also recovers a corrupt file without reading or retaining its contents.
        val snapshot = if (granted) read() else Snapshot(false, emptyList())
        write(snapshot.copy(consent = granted))
    }

    override suspend fun save(input: RoadNoteInput, receivedAt: Instant): RoadNoteDraft = locked {
        val snapshot = read()
        check(snapshot.consent) { "Local draft consent required" }
        val current = now()
        val active = prune(snapshot, current)
        val previous = input.draftId?.let { id ->
            active.drafts.find { it.id == id }
                ?: throw IllegalArgumentException("Unknown or expired draft ID")
        }
        check(previous != null || active.drafts.size < MAX_DRAFTS) { "Local draft limit reached" }
        val firstReceived = previous?.receivedAt ?: receivedAt
        require(firstReceived > current.minus(RETENTION)) { "Draft has expired" }
        require(firstReceived <= current) { "Draft receipt is in the future" }
        val id = previous?.id ?: UUID.randomUUID().toString()
        val draft = RoadNoteDraft(
            id = id,
            input = input.copy(draftId = id,
                utteranceRecordedAt = if (previous != null) previous.input.utteranceRecordedAt
                    else input.utteranceRecordedAt),
            receivedAt = firstReceived,
            updatedAt = current,
            state = if (input.locationMode in setOf(LocationMode.NEEDS_CLARIFICATION, LocationMode.DEFERRED) ||
                (input.locationMode == LocationMode.NAMED_PLACE && input.placeQuery.isNullOrBlank()) ||
                (input.directionMode == DirectionMode.USER_DESCRIPTION && input.directionDescription.isNullOrBlank())
            ) DraftState.AWAITING_LOCATION else DraftState.PENDING_LOCATION,
        )
        write(active.copy(drafts = active.drafts.filterNot { it.id == id } + draft))
        draft
    }

    override suspend fun list(): List<RoadNoteDraft> = locked {
        prune(read(), now()).drafts.sortedByDescending { it.receivedAt }
    }

    override suspend fun delete(id: String): Boolean = locked {
        val snapshot = read()
        val remaining = snapshot.drafts.filterNot { it.id == id }
        if (remaining.size == snapshot.drafts.size) false else {
            write(snapshot.copy(drafts = remaining))
            true
        }
    }

    override suspend fun clear() = locked {
        write(read().copy(drafts = emptyList()))
    }

    private fun prune(snapshot: Snapshot, current: Instant): Snapshot {
        val active = snapshot.copy(drafts = snapshot.drafts.filter {
            it.receivedAt > current.minus(RETENTION)
        })
        if (active != snapshot) write(active)
        return active
    }

    private suspend fun <T> locked(action: () -> T): T = withContext(Dispatchers.IO) {
        // All instances share the lock; the app uses a single Android process.
        mutex.withLock { action() }
    }

    private fun read(): Snapshot {
        // A process may die before rename; only the committed file is authoritative.
        val temp = File(file.parentFile, "${file.name}.tmp")
        if (temp.exists() && !temp.delete()) throw IOException("Cannot remove draft temporary file")
        if (!Files.exists(file.toPath())) {
            if (!Files.notExists(file.toPath())) throw IOException("Cannot access local draft store")
            return Snapshot(false, emptyList())
        }
        try {
            if (file.length() > 2_000_000) throw IOException("Local draft store is too large")
            DataInputStream(ByteArrayInputStream(decode(file.readBytes()))).use { data ->
                if (data.readInt() != VERSION) throw IOException("Unsupported local draft format")
                val consent = data.readBoolean()
                val count = data.readInt()
                if (count !in 0..MAX_DRAFTS || (!consent && count != 0)) {
                    throw IOException("Invalid local draft store")
                }
                val drafts = List(count) {
                    val id = data.readUTF()
                    val content = data.readUTF()
                    val location = LocationMode.valueOf(data.readUTF())
                    val place = data.readNullable()
                    val direction = DirectionMode.valueOf(data.readUTF())
                    val description = data.readNullable()
                    val utterance = data.readNullable()?.let(Instant::parse)
                    val received = Instant.parse(data.readUTF())
                    val updated = Instant.parse(data.readUTF())
                    val state = DraftState.valueOf(data.readUTF())
                    if (UUID.fromString(id).toString() != id) throw IOException("Invalid local draft ID")
                    RoadNoteDraft(id, RoadNoteInput(content, location, place, direction,
                        description, utterance, id), received, updated, state)
                }
                if (data.read() != -1 || drafts.map { it.id }.toSet().size != count) {
                    throw IOException("Invalid local draft store")
                }
                return Snapshot(consent, drafts)
            }
        } catch (error: IOException) {
            throw error
        } catch (_: RuntimeException) {
            // Parsing errors must not disclose stored values through exception messages.
            throw IOException("Invalid local draft store")
        }
    }

    private fun write(snapshot: Snapshot) {
        val bytes = ByteArrayOutputStream().use { buffer ->
            DataOutputStream(buffer).use { data ->
                data.writeInt(VERSION)
                data.writeBoolean(snapshot.consent)
                data.writeInt(snapshot.drafts.size)
                snapshot.drafts.forEach { draft ->
                    data.writeUTF(draft.id)
                    data.writeUTF(draft.input.content)
                    data.writeUTF(draft.input.locationMode.name)
                    data.writeNullable(draft.input.placeQuery)
                    data.writeUTF(draft.input.directionMode.name)
                    data.writeNullable(draft.input.directionDescription)
                    data.writeNullable(draft.input.utteranceRecordedAt?.toString())
                    data.writeUTF(draft.receivedAt.toString())
                    data.writeUTF(draft.updatedAt.toString())
                    data.writeUTF(draft.state.name)
                }
            }
            buffer.toByteArray()
        }
        val temp = File(file.parentFile, "${file.name}.tmp")
        try {
            FileOutputStream(temp).use { output ->
                output.write(encode(bytes))
                output.fd.sync()
            }
            replace(temp, file)
        } finally {
            if (temp.exists() && !temp.delete()) throw IOException("Cannot remove draft temporary file")
        }
    }

    private fun DataInputStream.readNullable(): String? = if (readBoolean()) readUTF() else null
    private fun DataOutputStream.writeNullable(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeUTF(value)
    }

    companion object {
        private const val VERSION = 1
        private const val MAX_DRAFTS = 100
        private val RETENTION = Duration.ofDays(7)
        private val mutex = Mutex()
    }
}
