package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_manuscripts")
data class LocalManuscriptEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val originalContent: String,
    val category: String,
    val writingStyle: String,
    val preferredMeter: String,
    val expectedMatra: Int,
    val wordCount: Int,
    val charCount: Int,
    val paragraphCount: Int,
    val pinned: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val pendingSync: Boolean,
    val deletedLocally: Boolean = false
)

@Entity(tableName = "local_chapters")
data class LocalChapterEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val manuscriptId: String,
    val title: String,
    val content: String,
    val orderIndex: Int,
    val wordCount: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val pendingSync: Boolean
)

@Entity(tableName = "local_revisions")
data class LocalRevisionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val manuscriptId: String,
    val title: String,
    val content: String,
    val summary: String,
    val createdAtMillis: Long,
    val pendingSync: Boolean
)

@Entity(tableName = "local_dictionary")
data class LocalDictionaryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val word: String,
    val category: String,
    val note: String,
    val createdAtMillis: Long,
    val pendingSync: Boolean
)

@Entity(tableName = "analysis_cache")
data class AnalysisCacheEntity(
    @PrimaryKey val cacheKey: String, // sha256(mode + style + text)
    val mode: String,
    val payloadJson: String,
    val createdAtMillis: Long
)
