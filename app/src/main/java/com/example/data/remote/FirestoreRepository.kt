package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.Chapter
import com.example.data.model.DictionaryWord
import com.example.data.model.Manuscript
import com.example.data.model.Revision
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeManuscripts(userId: String): Flow<List<Manuscript>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/manuscripts"
        emitAll(
            db.collection("users").document(uid).collection("manuscripts")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Manuscript::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeChapters(userId: String, manuscriptId: String): Flow<List<Chapter>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/manuscripts/$manuscriptId/chapters"
        emitAll(
            db.collection("users").document(uid)
                .collection("manuscripts").document(manuscriptId)
                .collection("chapters")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Chapter::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.orderIndex }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeRevisions(userId: String, manuscriptId: String): Flow<List<Revision>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/manuscripts/$manuscriptId/revisions"
        emitAll(
            db.collection("users").document(uid)
                .collection("manuscripts").document(manuscriptId)
                .collection("revisions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Revision::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeDictionary(userId: String): Flow<List<DictionaryWord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/dictionary"
        emitAll(
            db.collection("users").document(uid).collection("dictionary")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        DictionaryWord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertManuscript(
        manuscriptId: String,
        title: String,
        content: String,
        originalContent: String,
        category: String,
        writingStyle: String,
        preferredMeter: String,
        expectedMatra: Int,
        wordCount: Int,
        charCount: Int,
        paragraphCount: Int,
        pinned: Boolean
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/manuscripts/$manuscriptId"
        return try {
            val uid = requireUserId()
            require(manuscriptId.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) { "Invalid manuscriptId" }
            val safeTitle = title.trim().ifBlank { "শিরোনামহীন পান্ডুলিপি" }.take(200)
            val docRef = db.collection("users").document(uid)
                .collection("manuscripts").document(manuscriptId)
            docPath = docRef.path

            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "title" to safeTitle,
                        "content" to content.take(300000),
                        "originalContent" to originalContent.take(300000),
                        "category" to category,
                        "writingStyle" to writingStyle,
                        "preferredMeter" to preferredMeter,
                        "expectedMatra" to expectedMatra.coerceIn(0, 64),
                        "wordCount" to wordCount.coerceIn(0, 500000),
                        "charCount" to charCount.coerceIn(0, 3000000),
                        "paragraphCount" to paragraphCount.coerceIn(0, 100000),
                        "pinned" to pinned,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to safeTitle,
                        "content" to content.take(300000),
                        "originalContent" to originalContent.take(300000),
                        "category" to category,
                        "writingStyle" to writingStyle,
                        "preferredMeter" to preferredMeter,
                        "expectedMatra" to expectedMatra.coerceIn(0, 64),
                        "wordCount" to wordCount.coerceIn(0, 500000),
                        "charCount" to charCount.coerceIn(0, 3000000),
                        "paragraphCount" to paragraphCount.coerceIn(0, 100000),
                        "pinned" to pinned,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteManuscript(manuscriptId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/manuscripts/$manuscriptId"
        return try {
            val uid = requireUserId()
            val msRef = db.collection("users").document(uid)
                .collection("manuscripts").document(manuscriptId)
            docPath = msRef.path

            // Delete subcollections first cleanly
            val chapters = msRef.collection("chapters").get().await()
            chapters.documents.forEach { it.reference.delete().await() }
            val revisions = msRef.collection("revisions").get().await()
            revisions.documents.forEach { it.reference.delete().await() }

            msRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertChapter(
        manuscriptId: String,
        chapterId: String,
        title: String,
        content: String,
        orderIndex: Int,
        wordCount: Int
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/manuscripts/$manuscriptId/chapters/$chapterId"
        return try {
            val uid = requireUserId()
            val safeTitle = title.trim().ifBlank { "অধ্যায় ${orderIndex + 1}" }.take(200)
            val docRef = db.collection("users").document(uid)
                .collection("manuscripts").document(manuscriptId)
                .collection("chapters").document(chapterId)
            docPath = docRef.path

            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "title" to safeTitle,
                        "content" to content.take(200000),
                        "orderIndex" to orderIndex.coerceIn(0, 1000),
                        "wordCount" to wordCount.coerceIn(0, 200000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "manuscriptId" to manuscriptId,
                        "title" to safeTitle,
                        "content" to content.take(200000),
                        "orderIndex" to orderIndex.coerceIn(0, 1000),
                        "wordCount" to wordCount.coerceIn(0, 200000),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteChapter(manuscriptId: String, chapterId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/manuscripts/$manuscriptId/chapters/$chapterId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("manuscripts").document(manuscriptId)
                .collection("chapters").document(chapterId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addRevision(
        manuscriptId: String,
        revisionId: String,
        title: String,
        content: String,
        summary: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/manuscripts/$manuscriptId/revisions/$revisionId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("manuscripts").document(manuscriptId)
                .collection("revisions").document(revisionId)
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "manuscriptId" to manuscriptId,
                    "title" to title.trim().ifBlank { "শিরোনামহীন" }.take(200),
                    "content" to content.take(300000),
                    "summary" to summary.take(300),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertDictionaryWord(
        wordId: String,
        word: String,
        category: String,
        note: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/dictionary/$wordId"
        return try {
            val uid = requireUserId()
            val safeWord = word.trim().take(100)
            require(safeWord.isNotEmpty()) { "Word must not be empty" }
            val docRef = db.collection("users").document(uid)
                .collection("dictionary").document(wordId)
            docPath = docRef.path

            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "word" to safeWord,
                        "category" to category,
                        "note" to note.take(300)
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "word" to safeWord,
                        "category" to category,
                        "note" to note.take(300),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDictionaryWord(wordId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/dictionary/$wordId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("dictionary").document(wordId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAllUserCloudData(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val userDoc = db.collection("users").document(uid)
            val manuscripts = userDoc.collection("manuscripts").get().await()
            for (ms in manuscripts.documents) {
                deleteManuscript(ms.id)
            }
            val words = userDoc.collection("dictionary").get().await()
            for (w in words.documents) {
                w.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }
}
