package com.example.data.repository

import android.util.Log
import com.example.core.BengaliTextUtils
import com.example.data.local.AnalysisCacheEntity
import com.example.data.local.LocalChapterEntity
import com.example.data.local.LocalDictionaryEntity
import com.example.data.local.LocalManuscriptEntity
import com.example.data.local.LocalRevisionEntity
import com.example.data.local.SobdosuddhiDao
import com.example.data.model.DictionaryCategory
import com.example.data.model.ManuscriptCategory
import com.example.data.model.MeterType
import com.example.data.model.WritingStyle
import com.example.data.remote.FirestoreRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

private const val TAG = "ManuscriptSyncRepo"

/**
 * Offline-First + Cloud-Synced Repository.
 *
 * - Writes immediately to Room SQLite so offline editing, Auto Save, and recovery never fail.
 * - Syncs with Firebase Firestore when online AND user has granted sync permission (or triggers explicit Sync).
 * - Maintains SHA-256 AI analysis cache in Room to prevent redundant Gemini API calls on unchanged text.
 */
class ManuscriptSyncRepository(
    private val dao: SobdosuddhiDao,
    private val remote: FirestoreRepository
) {
    fun observeManuscripts(userId: String): Flow<List<LocalManuscriptEntity>> =
        dao.observeManuscripts(userId)

    fun observePendingSyncCount(userId: String): Flow<Int> =
        dao.observePendingManuscriptCount(userId)

    fun observeChapters(userId: String, manuscriptId: String): Flow<List<LocalChapterEntity>> =
        dao.observeChapters(userId, manuscriptId)

    fun observeRevisions(userId: String, manuscriptId: String): Flow<List<LocalRevisionEntity>> =
        dao.observeRevisions(userId, manuscriptId)

    fun observeDictionary(userId: String): Flow<List<LocalDictionaryEntity>> =
        dao.observeDictionary(userId)

    suspend fun getManuscript(id: String): LocalManuscriptEntity? = dao.getManuscriptById(id)

    suspend fun createManuscript(
        userId: String,
        title: String,
        content: String,
        category: ManuscriptCategory,
        writingStyle: WritingStyle,
        preferredMeter: MeterType = MeterType.AUTO,
        expectedMatra: Int = 0,
        onlineAndAllowedToSync: Boolean
    ): LocalManuscriptEntity {
        val now = System.currentTimeMillis()
        val id = "ms_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val metrics = BengaliTextUtils.computeMetrics(content)
        val safeTitle = title.trim().ifBlank { defaultTitleFor(category) }.take(200)

        val entity = LocalManuscriptEntity(
            id = id,
            userId = userId,
            title = safeTitle,
            content = content,
            originalContent = content,
            category = category.name,
            writingStyle = writingStyle.name,
            preferredMeter = preferredMeter.name,
            expectedMatra = expectedMatra,
            wordCount = metrics.wordCount,
            charCount = metrics.charCount,
            paragraphCount = metrics.paragraphCount,
            pinned = false,
            createdAtMillis = now,
            updatedAtMillis = now,
            pendingSync = true
        )
        dao.upsertManuscript(entity)

        if (onlineAndAllowedToSync) {
            pushSingleManuscript(entity)
        }
        return entity
    }

    suspend fun saveManuscript(
        entity: LocalManuscriptEntity,
        onlineAndAllowedToSync: Boolean
    ): LocalManuscriptEntity {
        val metrics = BengaliTextUtils.computeMetrics(entity.content)
        val updated = entity.copy(
            title = entity.title.trim().ifBlank { "শিরোনামহীন পান্ডুলিপি" }.take(200),
            wordCount = metrics.wordCount,
            charCount = metrics.charCount,
            paragraphCount = metrics.paragraphCount,
            updatedAtMillis = System.currentTimeMillis(),
            pendingSync = true
        )
        dao.upsertManuscript(updated)

        if (onlineAndAllowedToSync) {
            pushSingleManuscript(updated)
        }
        return updated
    }

    suspend fun deleteManuscript(
        id: String,
        onlineAndAllowedToSync: Boolean
    ) {
        val existing = dao.getManuscriptById(id) ?: return
        if (onlineAndAllowedToSync) {
            val res = remote.deleteManuscript(id)
            if (res.isSuccess) {
                dao.deleteChaptersForManuscript(id)
                dao.deleteRevisionsForManuscript(id)
                dao.hardDeleteManuscript(id)
                return
            }
        }
        // Mark soft-deleted offline until synced
        dao.upsertManuscript(existing.copy(deletedLocally = true, pendingSync = true))
    }

    suspend fun addChapter(
        userId: String,
        manuscriptId: String,
        title: String,
        content: String = "",
        onlineAndAllowedToSync: Boolean
    ): LocalChapterEntity {
        val existing = dao.getChaptersOnce(userId, manuscriptId)
        val now = System.currentTimeMillis()
        val id = "ch_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val metrics = BengaliTextUtils.computeMetrics(content)
        val entity = LocalChapterEntity(
            id = id,
            userId = userId,
            manuscriptId = manuscriptId,
            title = title.trim().ifBlank { "অধ্যায় ${BengaliTextUtils.toBengaliDigits(existing.size + 1)}" }.take(200),
            content = content,
            orderIndex = existing.size,
            wordCount = metrics.wordCount,
            createdAtMillis = now,
            updatedAtMillis = now,
            pendingSync = true
        )
        dao.upsertChapter(entity)
        if (onlineAndAllowedToSync) {
            val parent = dao.getManuscriptById(manuscriptId)
            if (parent != null && parent.pendingSync) pushSingleManuscript(parent)
            val res = remote.upsertChapter(
                manuscriptId = entity.manuscriptId,
                chapterId = entity.id,
                title = entity.title,
                content = entity.content,
                orderIndex = entity.orderIndex,
                wordCount = entity.wordCount
            )
            if (res.isSuccess) dao.upsertChapter(entity.copy(pendingSync = false))
        }
        return entity
    }

    suspend fun updateChapter(
        chapter: LocalChapterEntity,
        onlineAndAllowedToSync: Boolean
    ): LocalChapterEntity {
        val metrics = BengaliTextUtils.computeMetrics(chapter.content)
        val updated = chapter.copy(
            title = chapter.title.trim().ifBlank { "অধ্যায়" }.take(200),
            wordCount = metrics.wordCount,
            updatedAtMillis = System.currentTimeMillis(),
            pendingSync = true
        )
        dao.upsertChapter(updated)
        if (onlineAndAllowedToSync) {
            val res = remote.upsertChapter(
                manuscriptId = updated.manuscriptId,
                chapterId = updated.id,
                title = updated.title,
                content = updated.content,
                orderIndex = updated.orderIndex,
                wordCount = updated.wordCount
            )
            if (res.isSuccess) dao.upsertChapter(updated.copy(pendingSync = false))
        }
        return updated
    }

    suspend fun deleteChapter(
        manuscriptId: String,
        chapterId: String,
        onlineAndAllowedToSync: Boolean
    ) {
        dao.deleteChapterById(chapterId)
        if (onlineAndAllowedToSync) {
            remote.deleteChapter(manuscriptId, chapterId)
        }
    }

    suspend fun saveRevisionSnapshot(
        userId: String,
        manuscriptId: String,
        title: String,
        content: String,
        summary: String,
        onlineAndAllowedToSync: Boolean
    ): LocalRevisionEntity {
        val id = "rev_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val entity = LocalRevisionEntity(
            id = id,
            userId = userId,
            manuscriptId = manuscriptId,
            title = title.trim().ifBlank { "শিরোনামহীন" }.take(200),
            content = content,
            summary = summary.take(300),
            createdAtMillis = System.currentTimeMillis(),
            pendingSync = true
        )
        dao.upsertRevision(entity)
        if (onlineAndAllowedToSync) {
            val parent = dao.getManuscriptById(manuscriptId)
            if (parent != null && parent.pendingSync) pushSingleManuscript(parent)
            val res = remote.addRevision(
                manuscriptId = entity.manuscriptId,
                revisionId = entity.id,
                title = entity.title,
                content = entity.content,
                summary = entity.summary
            )
            if (res.isSuccess) dao.upsertRevision(entity.copy(pendingSync = false))
        }
        return entity
    }

    suspend fun addDictionaryWord(
        userId: String,
        word: String,
        category: DictionaryCategory,
        note: String,
        onlineAndAllowedToSync: Boolean
    ): LocalDictionaryEntity {
        val id = "wd_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val entity = LocalDictionaryEntity(
            id = id,
            userId = userId,
            word = word.trim().take(100),
            category = category.name,
            note = note.trim().take(300),
            createdAtMillis = System.currentTimeMillis(),
            pendingSync = true
        )
        dao.upsertDictionaryWord(entity)
        if (onlineAndAllowedToSync) {
            val res = remote.upsertDictionaryWord(
                wordId = entity.id,
                word = entity.word,
                category = entity.category,
                note = entity.note
            )
            if (res.isSuccess) dao.upsertDictionaryWord(entity.copy(pendingSync = false))
        }
        return entity
    }

    suspend fun deleteDictionaryWord(wordId: String, onlineAndAllowedToSync: Boolean) {
        dao.deleteDictionaryWordById(wordId)
        if (onlineAndAllowedToSync) {
            remote.deleteDictionaryWord(wordId)
        }
    }

    /**
     * Explicit bidirectional Cloud Sync (invoked with user permission when online).
     */
    suspend fun syncWithCloud(userId: String): Result<Int> {
        return try {
            var syncedCount = 0

            // 1. Push pending local manuscripts
            val pendingMs = dao.getPendingManuscripts(userId)
            for (ms in pendingMs) {
                if (ms.deletedLocally) {
                    val res = remote.deleteManuscript(ms.id)
                    if (res.isSuccess) {
                        dao.deleteChaptersForManuscript(ms.id)
                        dao.deleteRevisionsForManuscript(ms.id)
                        dao.hardDeleteManuscript(ms.id)
                        syncedCount++
                    }
                } else {
                    if (pushSingleManuscript(ms)) syncedCount++
                }
            }

            // 2. Push pending chapters
            val pendingChapters = dao.getPendingChapters(userId)
            for (ch in pendingChapters) {
                val res = remote.upsertChapter(
                    manuscriptId = ch.manuscriptId,
                    chapterId = ch.id,
                    title = ch.title,
                    content = ch.content,
                    orderIndex = ch.orderIndex,
                    wordCount = ch.wordCount
                )
                if (res.isSuccess) {
                    dao.upsertChapter(ch.copy(pendingSync = false))
                    syncedCount++
                }
            }

            // 3. Push pending revisions
            val pendingRevs = dao.getPendingRevisions(userId)
            for (rev in pendingRevs) {
                val res = remote.addRevision(
                    manuscriptId = rev.manuscriptId,
                    revisionId = rev.id,
                    title = rev.title,
                    content = rev.content,
                    summary = rev.summary
                )
                if (res.isSuccess) {
                    dao.upsertRevision(rev.copy(pendingSync = false))
                    syncedCount++
                }
            }

            // 4. Push pending custom dictionary entries
            val pendingDict = dao.getPendingDictionary(userId)
            for (d in pendingDict) {
                val res = remote.upsertDictionaryWord(d.id, d.word, d.category, d.note)
                if (res.isSuccess) {
                    dao.upsertDictionaryWord(d.copy(pendingSync = false))
                    syncedCount++
                }
            }

            // 5. Pull remote manuscripts and merge newer ones locally
            val remoteManuscripts = remote.observeManuscripts(userId).first()
            for (rm in remoteManuscripts) {
                val local = dao.getManuscriptById(rm.id)
                val remoteUpdated = rm.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                val remoteCreated = rm.createdAt?.toDate()?.time ?: remoteUpdated
                if (local == null || (!local.pendingSync && remoteUpdated > local.updatedAtMillis)) {
                    dao.upsertManuscript(
                        LocalManuscriptEntity(
                            id = rm.id,
                            userId = userId,
                            title = rm.title,
                            content = rm.content,
                            originalContent = rm.originalContent,
                            category = rm.category,
                            writingStyle = rm.writingStyle,
                            preferredMeter = rm.preferredMeter,
                            expectedMatra = rm.expectedMatra.toInt(),
                            wordCount = rm.wordCount.toInt(),
                            charCount = rm.charCount.toInt(),
                            paragraphCount = rm.paragraphCount.toInt(),
                            pinned = rm.pinned,
                            createdAtMillis = remoteCreated,
                            updatedAtMillis = remoteUpdated,
                            pendingSync = false
                        )
                    )
                    syncedCount++
                }
            }

            // 6. Pull remote dictionary
            val remoteDict = remote.observeDictionary(userId).first()
            for (rd in remoteDict) {
                val created = rd.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                dao.upsertDictionaryWord(
                    LocalDictionaryEntity(
                        id = rd.id,
                        userId = userId,
                        word = rd.word,
                        category = rd.category,
                        note = rd.note,
                        createdAtMillis = created,
                        pendingSync = false
                    )
                )
            }

            Result.success(syncedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Cloud sync failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAllUserData(userId: String, includeCloud: Boolean): Result<Unit> {
        return try {
            dao.deleteAllChaptersForUser(userId)
            dao.deleteAllRevisionsForUser(userId)
            dao.deleteAllManuscriptsForUser(userId)
            dao.deleteAllDictionaryForUser(userId)
            dao.clearAnalysisCache()
            if (includeCloud) {
                remote.deleteAllUserCloudData()
            } else {
                Result.success(Unit)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- AI Analysis Cache ---
    suspend fun getCachedJson(cacheKey: String): String? =
        dao.getCachedAnalysis(cacheKey)?.payloadJson

    suspend fun putCachedJson(cacheKey: String, mode: String, json: String) {
        dao.putCachedAnalysis(
            AnalysisCacheEntity(
                cacheKey = cacheKey,
                mode = mode,
                payloadJson = json,
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearAnalysisCache() = dao.clearAnalysisCache()

    private suspend fun pushSingleManuscript(ms: LocalManuscriptEntity): Boolean {
        val res = remote.upsertManuscript(
            manuscriptId = ms.id,
            title = ms.title,
            content = ms.content,
            originalContent = ms.originalContent,
            category = ms.category,
            writingStyle = ms.writingStyle,
            preferredMeter = ms.preferredMeter,
            expectedMatra = ms.expectedMatra,
            wordCount = ms.wordCount,
            charCount = ms.charCount,
            paragraphCount = ms.paragraphCount,
            pinned = ms.pinned
        )
        if (res.isSuccess) {
            dao.upsertManuscript(ms.copy(pendingSync = false))
            return true
        }
        return false
    }

    private fun defaultTitleFor(category: ManuscriptCategory): String = when (category) {
        ManuscriptCategory.POETRY -> "নতুন কবিতা"
        ManuscriptCategory.STORY -> "নতুন ছোটগল্প"
        ManuscriptCategory.NOVEL -> "নতুন উপন্যাস"
        ManuscriptCategory.MANUSCRIPT -> "নতুন পান্ডুলিপি"
        ManuscriptCategory.GENERAL -> "নতুন বাংলা লেখা"
    }
}
