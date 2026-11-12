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

    /** 동시 접근을 잠근 상태에서 파일의 저장 동의를 읽는다. */
    override suspend fun hasConsent(): Boolean = locked { read().consent }

    /** 동의를 기록한다. 철회 시 기존 파일을 읽지 않고 빈 상태로 덮어써 손상 파일도 지울 수 있다. */
    override suspend fun setConsent(granted: Boolean) = locked {
        // 철회할 때는 손상된 원문도 읽지 않고 동의와 초안을 함께 초기화한다.
        val snapshot = if (granted) read() else Snapshot(false, emptyList())
        write(snapshot.copy(consent = granted))
    }

    /**
     * 동의·만료·용량을 확인하고 초안을 생성/갱신한 뒤 파일에 반영한다.
     * 갱신해도 최초 수신 시각과 기존 관측 시각을 유지하므로 보관 기간이 늘어나지 않는다.
     */
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
                observedAt = if (previous != null) previous.input.observedAt
                    else input.observedAt),
            receivedAt = firstReceived,
            updatedAt = current,
            state = draftState(input),
        )
        write(active.copy(drafts = active.drafts.filterNot { it.id == id } + draft))
        draft
    }

    /** 위치/방향 표현에 추가 정보가 필요한지 판단해 초안의 대기 상태를 정한다. 실제 위치를 측정하지 않는다. */
    private fun draftState(input: RoadNoteInput): DraftState {
        val unresolvedMode = input.locationMode in setOf(LocationMode.NEEDS_CLARIFICATION, LocationMode.DEFERRED)
        val missingPlace = input.locationMode == LocationMode.NAMED_PLACE && input.placeQuery.isNullOrBlank()
        val missingDirection = input.directionMode == DirectionMode.USER_DESCRIPTION && input.directionDescription.isNullOrBlank()
        return if (unresolvedMode || missingPlace || missingDirection) {
            DraftState.AWAITING_LOCATION
        } else {
            DraftState.PENDING_LOCATION
        }
    }

    /** 만료 항목을 제거하고 남은 초안을 최초 수신 시각의 내림차순으로 반환한다. */
    override suspend fun list(): List<RoadNoteDraft> = locked {
        prune(read(), now()).drafts.sortedByDescending { it.receivedAt }
    }

    /** ID가 일치하는 초안만 제거해 저장하고 실제 삭제 여부를 반환한다. */
    override suspend fun delete(id: String): Boolean = locked {
        val snapshot = read()
        val remaining = snapshot.drafts.filterNot { it.id == id }
        if (remaining.size == snapshot.drafts.size) false else {
            write(snapshot.copy(drafts = remaining))
            true
        }
    }

    /** 동의 상태를 유지한 채 초안 목록만 비워 저장한다. */
    override suspend fun clear() = locked {
        write(read().copy(drafts = emptyList()))
    }

    /** 최초 수신부터 7일 이상 지난 초안을 제거하고, 목록이 달라졌을 때만 저장한다. */
    private fun prune(snapshot: Snapshot, current: Instant): Snapshot {
        val active = snapshot.copy(drafts = snapshot.drafts.filter {
            it.receivedAt > current.minus(RETENTION)
        })
        if (active != snapshot) write(active)
        return active
    }

    /** 블로킹 파일 작업을 IO에서 실행하고 프로세스 공용 Mutex로 읽기·수정·쓰기의 충돌을 막는다. */
    private suspend fun <T> locked(action: () -> T): T = withContext(Dispatchers.IO) {
        // Receiver와 화면이 서로 다른 저장소 인스턴스를 써도 같은 파일을 동시에 수정하지 못하게 한다.
        mutex.withLock { action() }
    }

    /**
     * 남은 임시 파일을 정리한 뒤 확정 파일을 복호화하고 형식·버전·항목을 검증한다.
     * 손상이나 접근 오류를 빈 목록으로 바꾸지 않아 기존 데이터의 조용한 유실을 막는다.
     */
    private fun read(): Snapshot {
        // 원자 교체 전에 프로세스가 죽을 수 있으므로 미완성 임시 파일은 복구 원본으로 사용하지 않는다.
        val temp = File(file.parentFile, "${file.name}.tmp")
        if (temp.exists() && !temp.delete()) throw IOException("Cannot remove draft temporary file")
        if (!Files.exists(file.toPath())) {
            if (!Files.notExists(file.toPath())) throw IOException("Cannot access local draft store")
            return Snapshot(false, emptyList())
        }
        try {
            if (file.length() > 2_000_000) throw IOException("Local draft store is too large")
            DataInputStream(ByteArrayInputStream(decode(file.readBytes()))).use { data ->
                val version = data.readInt()
                if (version !in 1..VERSION) throw IOException("Unsupported local draft format")
                val consent = data.readBoolean()
                val count = data.readInt()
                if (count !in 0..MAX_DRAFTS || (!consent && count != 0)) {
                    throw IOException("Invalid local draft store")
                }
                val drafts = List(count) { data.readDraft(version) }
                if (data.read() != -1 || drafts.map { it.id }.toSet().size != count) {
                    throw IOException("Invalid local draft store")
                }
                return Snapshot(consent, drafts)
            }
        } catch (error: IOException) {
            throw error
        } catch (_: RuntimeException) {
            // 잘못된 저장값이 예외 메시지에 노출되지 않도록 공통 오류로 바꾼다.
            throw IOException("Invalid local draft store")
        }
    }

    /** 파일 버전에 맞는 순서로 초안 한 건을 복원한다. v1의 없는 분류값은 null로 보존한다. */
    private fun DataInputStream.readDraft(version: Int): RoadNoteDraft {
        val id = readUTF()
        val content = readUTF()
        val location = LocationMode.valueOf(readUTF())
        val place = readNullable()
        val direction = DirectionMode.valueOf(readUTF())
        val description = readNullable()
        val observed = readNullable()?.let(Instant::parse)
        val received = Instant.parse(readUTF())
        val updated = Instant.parse(readUTF())
        val state = DraftState.valueOf(readUTF())
        // v1에는 이 필드가 없으므로 읽으려 하면 다음 초안의 시작 위치가 어긋난다.
        val category = if (version >= 2) readNullable() else null
        if (category != null && category !in setOf("WARNING", "INFORMATION")) {
            throw IOException("Invalid report category")
        }
        if (UUID.fromString(id).toString() != id) throw IOException("Invalid local draft ID")
        return RoadNoteDraft(
            id = id,
            input = RoadNoteInput(content, location, place, direction, description, observed, id, category),
            receivedAt = received,
            updatedAt = updated,
            state = state,
        )
    }

    /**
     * 전체 상태를 직렬화·암호화해 임시 파일에 쓰고 확정 파일로 원자 교체한다.
     * 교체 전 실패하면 기존 확정 파일을 유지하며, 사용이 끝난 임시 파일은 정리한다.
     */
    private fun write(snapshot: Snapshot) {
        val bytes = ByteArrayOutputStream().use { buffer ->
            DataOutputStream(buffer).use { data ->
                data.writeInt(VERSION)
                data.writeBoolean(snapshot.consent)
                data.writeInt(snapshot.drafts.size)
                snapshot.drafts.forEach { draft -> data.writeDraft(draft) }
            }
            buffer.toByteArray()
        }
        val temp = File(file.parentFile, "${file.name}.tmp")
        try {
            FileOutputStream(temp).use { output ->
                output.write(encode(bytes))
                // 임시 파일 내용을 디스크에 반영한 후에만 확정 파일을 교체한다.
                output.fd.sync()
            }
            replace(temp, file)
        } finally {
            if (temp.exists() && !temp.delete()) throw IOException("Cannot remove draft temporary file")
        }
    }

    /** 초안 한 건을 읽기 함수와 동일한 필드 순서로 직렬화한다. 순서를 바꾸면 기존 파일 호환성이 깨진다. */
    private fun DataOutputStream.writeDraft(draft: RoadNoteDraft) {
        writeUTF(draft.id)
        writeUTF(draft.input.content)
        writeUTF(draft.input.locationMode.name)
        writeNullable(draft.input.placeQuery)
        writeUTF(draft.input.directionMode.name)
        writeNullable(draft.input.directionDescription)
        writeNullable(draft.input.observedAt?.toString())
        writeUTF(draft.receivedAt.toString())
        writeUTF(draft.updatedAt.toString())
        writeUTF(draft.state.name)
        writeNullable(draft.input.category)
    }

    /** 앞의 존재 여부를 읽고, 값이 있는 경우에만 UTF 문자열을 읽는다. */
    private fun DataInputStream.readNullable(): String? = if (readBoolean()) readUTF() else null

    /** null 여부를 먼저 기록하고 값이 있을 때만 UTF 문자열을 이어 쓴다. */
    private fun DataOutputStream.writeNullable(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeUTF(value)
    }

    companion object {
        private const val VERSION = 2
        private const val MAX_DRAFTS = 100
        private val RETENTION = Duration.ofDays(7)
        private val mutex = Mutex()
    }
}
