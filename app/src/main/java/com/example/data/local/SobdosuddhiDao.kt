package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SobdosuddhiDao {
    // --- Manuscripts ---
    @Query("SELECT * FROM local_manuscripts WHERE userId = :userId AND deletedLocally = 0 ORDER BY pinned DESC, updatedAtMillis DESC")
    fun observeManuscripts(userId: String): Flow<List<LocalManuscriptEntity>>

    @Query("SELECT * FROM local_manuscripts WHERE id = :id LIMIT 1")
    suspend fun getManuscriptById(id: String): LocalManuscriptEntity?

    @Query("SELECT * FROM local_manuscripts WHERE userId = :userId AND pendingSync = 1")
    suspend fun getPendingManuscripts(userId: String): List<LocalManuscriptEntity>

    @Query("SELECT COUNT(*) FROM local_manuscripts WHERE userId = :userId AND pendingSync = 1")
    fun observePendingManuscriptCount(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManuscript(entity: LocalManuscriptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManuscripts(entities: List<LocalManuscriptEntity>)

    @Query("DELETE FROM local_manuscripts WHERE id = :id")
    suspend fun hardDeleteManuscript(id: String)

    @Query("DELETE FROM local_manuscripts WHERE userId = :userId")
    suspend fun deleteAllManuscriptsForUser(userId: String)

    // --- Chapters ---
    @Query("SELECT * FROM local_chapters WHERE userId = :userId AND manuscriptId = :manuscriptId ORDER BY orderIndex ASC, createdAtMillis ASC")
    fun observeChapters(userId: String, manuscriptId: String): Flow<List<LocalChapterEntity>>

    @Query("SELECT * FROM local_chapters WHERE userId = :userId AND manuscriptId = :manuscriptId ORDER BY orderIndex ASC")
    suspend fun getChaptersOnce(userId: String, manuscriptId: String): List<LocalChapterEntity>

    @Query("SELECT * FROM local_chapters WHERE userId = :userId AND pendingSync = 1")
    suspend fun getPendingChapters(userId: String): List<LocalChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapter(entity: LocalChapterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapters(entities: List<LocalChapterEntity>)

    @Query("DELETE FROM local_chapters WHERE id = :id")
    suspend fun deleteChapterById(id: String)

    @Query("DELETE FROM local_chapters WHERE manuscriptId = :manuscriptId")
    suspend fun deleteChaptersForManuscript(manuscriptId: String)

    @Query("DELETE FROM local_chapters WHERE userId = :userId")
    suspend fun deleteAllChaptersForUser(userId: String)

    // --- Revisions ---
    @Query("SELECT * FROM local_revisions WHERE userId = :userId AND manuscriptId = :manuscriptId ORDER BY createdAtMillis DESC")
    fun observeRevisions(userId: String, manuscriptId: String): Flow<List<LocalRevisionEntity>>

    @Query("SELECT * FROM local_revisions WHERE userId = :userId AND pendingSync = 1")
    suspend fun getPendingRevisions(userId: String): List<LocalRevisionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRevision(entity: LocalRevisionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRevisions(entities: List<LocalRevisionEntity>)

    @Query("DELETE FROM local_revisions WHERE manuscriptId = :manuscriptId")
    suspend fun deleteRevisionsForManuscript(manuscriptId: String)

    @Query("DELETE FROM local_revisions WHERE userId = :userId")
    suspend fun deleteAllRevisionsForUser(userId: String)

    // --- Custom Dictionary ---
    @Query("SELECT * FROM local_dictionary WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeDictionary(userId: String): Flow<List<LocalDictionaryEntity>>

    @Query("SELECT * FROM local_dictionary WHERE userId = :userId AND pendingSync = 1")
    suspend fun getPendingDictionary(userId: String): List<LocalDictionaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDictionaryWord(entity: LocalDictionaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDictionaryWords(entities: List<LocalDictionaryEntity>)

    @Query("DELETE FROM local_dictionary WHERE id = :id")
    suspend fun deleteDictionaryWordById(id: String)

    @Query("DELETE FROM local_dictionary WHERE userId = :userId")
    suspend fun deleteAllDictionaryForUser(userId: String)

    // --- AI Analysis Cache ---
    @Query("SELECT * FROM analysis_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun getCachedAnalysis(key: String): AnalysisCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCachedAnalysis(entity: AnalysisCacheEntity)

    @Query("DELETE FROM analysis_cache")
    suspend fun clearAnalysisCache()
}
