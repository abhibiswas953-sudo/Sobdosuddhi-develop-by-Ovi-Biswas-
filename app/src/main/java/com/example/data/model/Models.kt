package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ManuscriptCategory(val labelBn: String) {
    GENERAL("সাধারণ লেখা"),
    POETRY("কবিতা"),
    STORY("ছোটগল্প"),
    NOVEL("উপন্যাস"),
    MANUSCRIPT("পূর্ণাঙ্গ পান্ডুলিপি");

    companion object {
        fun fromRaw(raw: String?): ManuscriptCategory =
            entries.firstOrNull { it.name == raw } ?: GENERAL
    }
}

enum class WritingStyle(val labelBn: String) {
    CHOLITO("চলিত ভাষা"),
    SADHU("সাধু ভাষা"),
    POETIC("কাব্যভাষা");

    companion object {
        fun fromRaw(raw: String?): WritingStyle =
            entries.firstOrNull { it.name == raw } ?: CHOLITO
    }
}

enum class MeterType(val labelBn: String, val shortRuleBn: String) {
    AUTO("স্বয়ংক্রিয় শনাক্তকরণ", "পঙ্ক্তির ছন্দবিন্যাস অনুযায়ী সম্ভাব্য ছন্দ নির্ণয়"),
    AKSHARBRITTO("অক্ষরবৃত্ত", "মুক্তদল ১ মাত্রা; আদিমধ্য রুদ্ধদল ১ মাত্রা, প্রান্তিক ও একক রুদ্ধদল ২ মাত্রা"),
    MATRABRITTO("মাত্রাবৃত্ত", "মুক্তদল ১ মাত্রা; প্রতিটি রুদ্ধদল ও যৌগিক স্বর ২ মাত্রা"),
    SWARABRITTO("স্বরবৃত্ত", "মুক্তদল ও রুদ্ধদল প্রতিটি ১ মাত্রা (লৌকিক ও দ্রুত লয়)");

    companion object {
        fun fromRaw(raw: String?): MeterType =
            entries.firstOrNull { it.name == raw } ?: AUTO
    }
}

enum class DictionaryCategory(val labelBn: String) {
    REGIONAL("আঞ্চলিক শব্দ"),
    POETIC("কাব্যিক প্রয়োগ"),
    CHARACTER("চরিত্রের নাম"),
    SADHU("সাধুভাষা"),
    CUSTOM("ব্যক্তিগত শব্দ");

    companion object {
        fun fromRaw(raw: String?): DictionaryCategory =
            entries.firstOrNull { it.name == raw } ?: CUSTOM
    }
}

enum class AnalysisTab(val labelBn: String) {
    SPELL("বানান"),
    GRAMMAR("বাক্য ও ব্যাকরণ"),
    PROSODY("ছন্দ ও মাত্রা"),
    POETRY("কবিতা সম্পাদনা"),
    FICTION("গল্প ও উপন্যাস"),
    AI_STYLE("AI লিখন বিশ্লেষণ")
}

enum class AppThemeMode(val labelBn: String) {
    SYSTEM("সিস্টেম অনুযায়ী"),
    LIGHT("লাইট মোড (আইভরি)"),
    DARK("ডার্ক মোড (নাইট ইঙ্ক)");

    companion object {
        fun fromRaw(raw: String?): AppThemeMode =
            entries.firstOrNull { it.name == raw } ?: LIGHT
    }
}

enum class BengaliFontOption(val labelBn: String) {
    NOTO_SERIF("Noto Serif Bengali (সাহিত্য)"),
    NOTO_SANS("Noto Sans Bengali (আধুনিক)");

    companion object {
        fun fromRaw(raw: String?): BengaliFontOption =
            entries.firstOrNull { it.name == raw } ?: NOTO_SERIF
    }
}

// --- Firestore Domain Models (1:1 parity with firestore.rules) ---

@Keep
@IgnoreExtraProperties
data class Manuscript(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val originalContent: String = "",
    val category: String = ManuscriptCategory.GENERAL.name,
    val writingStyle: String = WritingStyle.CHOLITO.name,
    val preferredMeter: String = MeterType.AUTO.name,
    val expectedMatra: Long = 0L,
    val wordCount: Long = 0L,
    val charCount: Long = 0L,
    val paragraphCount: Long = 0L,
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Chapter(
    @DocumentId val id: String = "",
    val userId: String = "",
    val manuscriptId: String = "",
    val title: String = "",
    val content: String = "",
    val orderIndex: Long = 0L,
    val wordCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Revision(
    @DocumentId val id: String = "",
    val userId: String = "",
    val manuscriptId: String = "",
    val title: String = "",
    val content: String = "",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DictionaryWord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val word: String = "",
    val category: String = DictionaryCategory.CUSTOM.name,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

// --- Proofreading, Grammar, Prosody & Literary Analysis Models ---

data class SpellIssue(
    val id: String,
    val original: String,
    val issueType: String,
    val suggestion: String,
    val reason: String,
    val source: String = "RULE", // "RULE" or "AI"
    val startIndex: Int = -1,
    val ignored: Boolean = false,
    val applied: Boolean = false
)

data class GrammarIssue(
    val id: String,
    val originalSentence: String,
    val issueType: String,
    val reason: String,
    val minimalCorrection: String,      // A. ন্যূনতম সংশোধন
    val grammaticalCorrection: String,  // B. ব্যাকরণসম্মত সংশোধন
    val fluentAlternative: String,      // C. আরও সাবলীল বিকল্প বাক্য
    val source: String = "AI",
    val ignored: Boolean = false,
    val appliedOption: String? = null
)

data class SyllableUnit(
    val text: String,
    val closed: Boolean, // true = রুদ্ধদল, false = মুক্তদল
    val matra: Int
)

data class ProsodyLineResult(
    val lineNumber: Int,
    val lineText: String,
    val detectedMeter: MeterType,
    val expectedMatra: Int,
    val actualMatra: Int,
    val syllables: List<SyllableUnit>,
    val parbaBreakdown: String,
    val endRhymeKey: String,
    val statusLabel: String,
    val possibleBreak: Boolean,
    val breakLocation: String,
    val suggestion: String
)

data class ProsodyReport(
    val selectedMeter: MeterType,
    val detectedMeter: MeterType,
    val expectedMatra: Int,
    val rhymeScheme: String,
    val continuitySummary: String,
    val lineResults: List<ProsodyLineResult>,
    val aiPoeticAdvice: String = "",
    val fromAi: Boolean = false
)

data class PoetryReviewReport(
    val toneAndVoiceSummary: String,
    val rhymeAnalysis: String,
    val repeatedWords: List<String>,
    val weakWordNotes: List<WeakWordSuggestion>,
    val lineFlowFeedback: List<String>,
    val preservationNote: String
)

data class WeakWordSuggestion(
    val originalWord: String,
    val contextLine: String,
    val alternatives: List<String>,
    val reason: String
)

data class FictionContinuityReport(
    val overallFlowSummary: String,
    val dialogueFeedback: List<String>,
    val characterConsistencyNotes: List<String>,
    val timelineAndContinuityNotes: List<String>,
    val tenseAndPersonNotes: List<String>,
    val redundancyNotes: List<String>,
    val paragraphStructureTips: List<String>,
    val chunksAnalyzed: Int
)

data class AiWritingReport(
    val aiSignalConfidencePercent: Int, // 0..100 cautious indicator, never absolute verdict
    val signalBandLabel: String,
    val naturalnessScore: Int,
    val lexicalDiversityScore: Int,
    val sentenceUniformityNote: String,
    val repetitionSignals: List<String>,
    val templatePhrasingSignals: List<String>,
    val detailedObservations: List<String>,
    val disclaimer: String,
    val fromAi: Boolean
)

data class UsageGuideEntry(
    val id: String,
    val title: String,
    val category: String,
    val incorrectOrConfused: String,
    val correctUsage: String,
    val explanation: String,
    val exampleSentence: String
)
