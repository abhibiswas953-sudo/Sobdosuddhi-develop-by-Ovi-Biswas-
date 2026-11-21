package com.example.data.remote

import android.util.Log
import com.example.core.BengaliTextUtils
import com.example.data.model.AiWritingReport
import com.example.data.model.FictionContinuityReport
import com.example.data.model.GrammarIssue
import com.example.data.model.ManuscriptCategory
import com.example.data.model.MeterType
import com.example.data.model.PoetryReviewReport
import com.example.data.model.SpellIssue
import com.example.data.model.WeakWordSuggestion
import com.example.data.model.WritingStyle
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val TAG = "SobdosuddhiAiService"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val AI_TIMEOUT_MS = 60_000L

/**
 * Gemini AI-powered Bengali Literary Proofreading, Grammar, Prosody, Poetry, Fiction,
 * and AI-Writing Signal Engine powered by Firebase AI Logic (`Firebase.ai`).
 */
class SobdosuddhiAiService {

    // Lazy models configured with strict structured JSON schemas (Schema.obj)
    private val spellModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "issues" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "original" to Schema.string(description = "Exact Bengali word or short phrase from the input"),
                            "issueType" to Schema.string(description = "Bengali category e.g. বানান ভুল, যুক্তাক্ষরের ভুল, ই/ঈ-কারের ভুল, ণ/ন-এর ব্যবহার, শ/ষ/স-এর ভুল, বিরামচিহ্ন"),
                            "suggestion" to Schema.string(description = "Proposed accurate Bengali correction"),
                            "reason" to Schema.string(description = "Concise literary Bengali explanation of why this correction is suggested")
                        )
                    )
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val grammarModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "issues" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "originalSentence" to Schema.string(description = "Exact original sentence from text"),
                            "issueType" to Schema.string(description = "Bengali grammar issue label"),
                            "reason" to Schema.string(description = "Clear Bengali explanation"),
                            "minimalCorrection" to Schema.string(description = "A. ন্যূনতম সংশোধন preserving author voice"),
                            "grammaticalCorrection" to Schema.string(description = "B. ব্যাকরণসম্মত সংশোধন"),
                            "fluentAlternative" to Schema.string(description = "C. আরও সাবলীল বিকল্প বাক্য")
                        )
                    )
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val prosodyAdviceModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "overallAdvice" to Schema.string(description = "Detailed Bengali literary prosody feedback and meter guidance"),
                "lineSuggestions" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "lineNumber" to Schema.integer(description = "1-based line number"),
                            "suggestion" to Schema.string(description = "Gentle poetic word tweak preserving emotion, metaphor, and end-rhyme")
                        )
                    )
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val poetryModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "toneAndVoiceSummary" to Schema.string(),
                "rhymeAnalysis" to Schema.string(),
                "repeatedWords" to Schema.array(items = Schema.string()),
                "weakWordNotes" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "originalWord" to Schema.string(),
                            "contextLine" to Schema.string(),
                            "alternatives" to Schema.array(items = Schema.string()),
                            "reason" to Schema.string()
                        )
                    )
                ),
                "lineFlowFeedback" to Schema.array(items = Schema.string()),
                "preservationNote" to Schema.string()
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val fictionModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "overallFlowSummary" to Schema.string(),
                "dialogueFeedback" to Schema.array(items = Schema.string()),
                "characterConsistencyNotes" to Schema.array(items = Schema.string()),
                "timelineAndContinuityNotes" to Schema.array(items = Schema.string()),
                "tenseAndPersonNotes" to Schema.array(items = Schema.string()),
                "redundancyNotes" to Schema.array(items = Schema.string()),
                "paragraphStructureTips" to Schema.array(items = Schema.string())
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val aiStyleModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "aiSignalConfidencePercent" to Schema.integer(description = "0 to 100 cautious statistical indicator"),
                "signalBandLabel" to Schema.string(description = "Cautious Bengali label"),
                "naturalnessScore" to Schema.integer(description = "0 to 100"),
                "lexicalDiversityScore" to Schema.integer(description = "0 to 100"),
                "sentenceUniformityNote" to Schema.string(),
                "repetitionSignals" to Schema.array(items = Schema.string()),
                "templatePhrasingSignals" to Schema.array(items = Schema.string()),
                "detailedObservations" to Schema.array(items = Schema.string())
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private suspend fun <T> executeWithRetry(block: suspend () -> T): T {
        var lastError: Exception? = null
        for (attempt in 0..1) {
            try {
                return withTimeout(AI_TIMEOUT_MS) { block() }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI request timed out on attempt ${attempt + 1}", e)
                lastError = e
                if (attempt == 0) delay(900L)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI request failed on attempt ${attempt + 1}", e)
                lastError = e
                if (attempt == 0) delay(900L)
            }
        }
        throw lastError ?: IllegalStateException("AI বিশ্লেষণ সম্পন্ন করা যায়নি।")
    }

    suspend fun analyzeSpellingWithAi(
        text: String,
        customDictionary: Set<String>,
        writingStyle: WritingStyle,
        category: ManuscriptCategory,
        chunkSize: Int = 2200,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<SpellIssue> {
        val chunks = BengaliTextUtils.chunkManuscript(text, chunkSize)
        val combined = mutableListOf<SpellIssue>()
        val protectedList = customDictionary.take(60).joinToString(", ")

        for (chunk in chunks) {
            onProgress(chunk.index + 1, chunk.totalChunks)
            val prompt = buildString {
                appendLine("তুমি একজন প্রবীণ বাংলা সাহিত্য সম্পাদক ও বানান বিশারদ।")
                appendLine("নিচের বাংলা লেখাটির বানান, যুক্তাক্ষর, কার-ফলা, ই/ঈ, উ/ঊ, রেফ/র-ফলা, ণ/ন, শ/ষ/স, য-ফলা/ব-ফলা, শব্দের পুনরাবৃত্তি, অপ্রয়োজনীয় শব্দ এবং বিরামচিহ্নের ভুলগুলো শনাক্ত করো।")
                appendLine("কঠোর নিয়ম:")
                appendLine("১. লেখার ধরন: ${category.labelBn}, ভাষারীতি: ${writingStyle.labelBn}।")
                appendLine("২. আঞ্চলিক শব্দ, কবিতার নিজস্ব শব্দপ্রয়োগ, সাধুভাষা, কথ্যভাষা এবং ইচ্ছাকৃত সাহিত্যিক রূপক কখনোই ভুল হিসেবে চিহ্নিত করবে না।")
                if (protectedList.isNotBlank()) {
                    appendLine("৩. লেখকের সংরক্ষিত ডিকশনারি শব্দ (ভুল ধরবে না): $protectedList")
                }
                appendLine("৪. কেবল প্রকৃত ও স্পষ্ট ভুলগুলোই তালিকাভুক্ত করো। ভুল না থাকলে খালি অ্যারে দাও।")
                if (chunk.previousContext.isNotBlank()) {
                    appendLine("পূর্ববর্তী প্রসঙ্গ (বিশ্লেষণ করবে না, কেবল ধারাবাহিকতার জন্য): ${chunk.previousContext}")
                }
                appendLine("\n--- বিশ্লেষণীয় লেখা (অংশ ${chunk.index + 1}/${chunk.totalChunks}) ---")
                appendLine(chunk.content)
            }

            val rawJson = executeWithRetry {
                spellModel.generateContent(prompt).text
                    ?: throw IllegalStateException("AI থেকে কোনো উত্তর পাওয়া যায়নি।")
            }

            val root = JSONObject(rawJson)
            val arr = root.optJSONArray("issues") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val original = obj.optString("original").trim()
                val suggestion = obj.optString("suggestion").trim()
                if (original.isNotEmpty() && original != suggestion && original !in customDictionary) {
                    combined.add(
                        SpellIssue(
                            id = UUID.randomUUID().toString(),
                            original = original,
                            issueType = obj.optString("issueType", "বানান সংশোধন"),
                            suggestion = suggestion,
                            reason = obj.optString("reason", "প্রমিত বাংলা বানান অনুযায়ী সংশোধন প্রস্তাব।"),
                            source = "AI",
                            startIndex = text.indexOf(original)
                        )
                    )
                }
            }
        }
        return combined.distinctBy { "${it.original}_${it.suggestion}" }
    }

    suspend fun analyzeGrammarWithAi(
        text: String,
        writingStyle: WritingStyle,
        category: ManuscriptCategory,
        preserveVoice: Boolean,
        chunkSize: Int = 2200,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<GrammarIssue> {
        val chunks = BengaliTextUtils.chunkManuscript(text, chunkSize)
        val combined = mutableListOf<GrammarIssue>()

        for (chunk in chunks) {
            onProgress(chunk.index + 1, chunk.totalChunks)
            val prompt = buildString {
                appendLine("তুমি একজন বাংলা ব্যাকরণবিদ ও সাহিত্য সম্পাদক।")
                appendLine("নিচের বাংলা লেখার প্রতিটি ত্রুটিপূর্ণ বা দুর্বল বাক্য বিশ্লেষণ করো: কর্তা-ক্রিয়ার সামঞ্জস্য, বাক্যের কাল, ক্রিয়ার ব্যবহার, পদক্রম, স্বাভাবিকতা, অসম্পূর্ণতা, পুনরাবৃত্তি, বিরামচিহ্ন, অর্থের অস্পষ্টতা এবং পাঠযোগ্যতা।")
                appendLine("ধরন: ${category.labelBn}, রীতি: ${writingStyle.labelBn}।")
                if (category == ManuscriptCategory.POETRY || preserveVoice) {
                    appendLine("বিশেষ নির্দেশনা: লেখকের নিজস্ব কণ্ঠস্বর, উপমা, রূপক ও কাব্যিক স্বাধীনতা সম্পূর্ণ অক্ষুণ্ণ রাখো। বাক্যকে কৃত্রিম বা সাধারণ AI ভাষায় রূপান্তর করবে না।")
                }
                appendLine("প্রতিটি সমস্যাযুক্ত বাক্যের জন্য ৩টি বিকল্প দাও: minimalCorrection (A. ন্যূনতম সংশোধন), grammaticalCorrection (B. ব্যাকরণসম্মত সংশোধন), fluentAlternative (C. আরও সাবলীল বিকল্প বাক্য)।")
                appendLine("\n--- লেখা ---")
                appendLine(chunk.content)
            }

            val rawJson = executeWithRetry {
                grammarModel.generateContent(prompt).text
                    ?: throw IllegalStateException("AI থেকে কোনো উত্তর পাওয়া যায়নি।")
            }

            val root = JSONObject(rawJson)
            val arr = root.optJSONArray("issues") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val originalSentence = obj.optString("originalSentence").trim()
                if (originalSentence.isNotEmpty()) {
                    combined.add(
                        GrammarIssue(
                            id = UUID.randomUUID().toString(),
                            originalSentence = originalSentence,
                            issueType = obj.optString("issueType", "বাক্য গঠন"),
                            reason = obj.optString("reason", ""),
                            minimalCorrection = obj.optString("minimalCorrection", originalSentence),
                            grammaticalCorrection = obj.optString("grammaticalCorrection", originalSentence),
                            fluentAlternative = obj.optString("fluentAlternative", originalSentence),
                            source = "AI"
                        )
                    )
                }
            }
        }
        return combined.distinctBy { it.originalSentence }
    }

    suspend fun enrichProsodyWithAi(
        poemText: String,
        meter: MeterType,
        expectedMatra: Int
    ): Pair<String, Map<Int, String>> {
        val prompt = buildString {
            appendLine("তুমি একজন অভিজ্ঞ বাংলা ছন্দবিশারদ ও কবি।")
            appendLine("নির্বাচিত ছন্দ: ${meter.labelBn} (${meter.shortRuleBn})। প্রত্যাশিত মাত্রা: $expectedMatra।")
            appendLine("নিচের কবিতাটির ছন্দ, মাত্রা ও পর্ববিন্যাস যাচাই করো। কোথাও সম্ভাব্য ছন্দপতন থাকলে তা ‘সম্ভাব্য মাত্রার হেরফের’ হিসেবে উল্লেখ করো (নিশ্চিত ভুল বলবে না) এবং মূল ভাব, আবেগ ও অন্ত্যমিল বজায় রেখে সংক্ষিপ্ত শব্দপরামর্শ দাও।")
            appendLine("\n--- কবিতা ---")
            appendLine(poemText)
        }
        val rawJson = executeWithRetry {
            prosodyAdviceModel.generateContent(prompt).text
                ?: throw IllegalStateException("AI ছন্দ পরামর্শ পাওয়া যায়নি।")
        }
        val root = JSONObject(rawJson)
        val overall = root.optString("overallAdvice", "")
        val lineMap = mutableMapOf<Int, String>()
        val arr = root.optJSONArray("lineSuggestions") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val lineNum = obj.optInt("lineNumber", -1)
            val sug = obj.optString("suggestion", "").trim()
            if (lineNum > 0 && sug.isNotEmpty()) {
                lineMap[lineNum] = sug
            }
        }
        return overall to lineMap
    }

    suspend fun analyzePoetryWithAi(poemText: String): PoetryReviewReport {
        val prompt = buildString {
            appendLine("তুমি একজন সংবেদনশীল বাংলা কবিতা সম্পাদক।")
            appendLine("কবিতাটি নতুন করে লিখে দেবে না। কবির নিজস্ব ভাষা, আবেগ, উপমা, রূপক ও বক্তব্য অক্ষুণ্ণ রেখে নিচের বিষয়গুলো বিশ্লেষণ করো:")
            appendLine("- কাব্যস্বর ও ভাবের সংক্ষিপ্ত মূল্যায়ন")
            appendLine("- অন্ত্যমিল ও ধ্বনিসাম্য বিশ্লেষণ")
            appendLine("- শব্দের পুনরাবৃত্তি")
            appendLine("- দুর্বল বা প্রচলিত শব্দপ্রয়োগের জন্য কাব্যিক বিকল্প শব্দ (weakWordNotes)")
            appendLine("- পঙ্ক্তির প্রবাহ বিশ্লেষণ (lineFlowFeedback)")
            appendLine("- কবির স্বকীয়তা রক্ষার নোট (preservationNote)")
            appendLine("\n--- কবিতা ---")
            appendLine(poemText)
        }
        val rawJson = executeWithRetry {
            poetryModel.generateContent(prompt).text
                ?: throw IllegalStateException("AI কবিতা বিশ্লেষণ পাওয়া যায়নি।")
        }
        val root = JSONObject(rawJson)
        val weakArr = root.optJSONArray("weakWordNotes") ?: JSONArray()
        val weakList = buildList {
            for (i in 0 until weakArr.length()) {
                val o = weakArr.getJSONObject(i)
                add(
                    WeakWordSuggestion(
                        originalWord = o.optString("originalWord"),
                        contextLine = o.optString("contextLine"),
                        alternatives = jsonStringList(o.optJSONArray("alternatives")),
                        reason = o.optString("reason")
                    )
                )
            }
        }
        return PoetryReviewReport(
            toneAndVoiceSummary = root.optString("toneAndVoiceSummary"),
            rhymeAnalysis = root.optString("rhymeAnalysis"),
            repeatedWords = jsonStringList(root.optJSONArray("repeatedWords")),
            weakWordNotes = weakList,
            lineFlowFeedback = jsonStringList(root.optJSONArray("lineFlowFeedback")),
            preservationNote = root.optString(
                "preservationNote",
                "আপনার মূল কাব্যভাব, উপমা ও নিজস্ব শৈলী অক্ষুণ্ণ রেখেই কেবল সহায়ক পরামর্শ দেওয়া হয়েছে।"
            )
        )
    }

    suspend fun analyzeFictionWithAi(
        text: String,
        chunkSize: Int = 2200,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): FictionContinuityReport {
        val chunks = BengaliTextUtils.chunkManuscript(text, chunkSize)
        val summaries = mutableListOf<String>()
        val dialogue = mutableListOf<String>()
        val characters = mutableListOf<String>()
        val timeline = mutableListOf<String>()
        val tense = mutableListOf<String>()
        val redundancy = mutableListOf<String>()
        val paragraphs = mutableListOf<String>()

        for (chunk in chunks) {
            onProgress(chunk.index + 1, chunk.totalChunks)
            val prompt = buildString {
                appendLine("তুমি একজন অভিজ্ঞ বাংলা কথাসাহিত্য (গল্প ও উপন্যাস) সম্পাদক।")
                appendLine("নিচের অধ্যায়/অংশটি বিশ্লেষণ করে বাংলায় জানাও: অনুচ্ছেদের প্রবাহ, সংলাপের স্বাভাবিকতা, চরিত্রের নামের ধারাবাহিকতা, সময় ও ঘটনার ধারাবাহিকতা, একই তথ্যের অপ্রয়োজনীয় পুনরাবৃত্তি, কাল ও পুরুষের অসামঞ্জস্য এবং অনুচ্ছেদ বিভাজন।")
                if (chunk.previousContext.isNotBlank()) {
                    appendLine("পূর্ববর্তী অংশের প্রসঙ্গ: ${chunk.previousContext}")
                }
                if (chunk.nextContext.isNotBlank()) {
                    appendLine("পরবর্তী অংশের প্রসঙ্গ: ${chunk.nextContext}")
                }
                appendLine("\n--- পান্ডুলিপি অংশ ${chunk.index + 1}/${chunk.totalChunks} ---")
                appendLine(chunk.content)
            }
            val rawJson = executeWithRetry {
                fictionModel.generateContent(prompt).text
                    ?: throw IllegalStateException("AI কথাসাহিত্য বিশ্লেষণ পাওয়া যায়নি।")
            }
            val root = JSONObject(rawJson)
            root.optString("overallFlowSummary").takeIf { it.isNotBlank() }?.let { summaries.add(it) }
            dialogue.addAll(jsonStringList(root.optJSONArray("dialogueFeedback")))
            characters.addAll(jsonStringList(root.optJSONArray("characterConsistencyNotes")))
            timeline.addAll(jsonStringList(root.optJSONArray("timelineAndContinuityNotes")))
            tense.addAll(jsonStringList(root.optJSONArray("tenseAndPersonNotes")))
            redundancy.addAll(jsonStringList(root.optJSONArray("redundancyNotes")))
            paragraphs.addAll(jsonStringList(root.optJSONArray("paragraphStructureTips")))
        }

        return FictionContinuityReport(
            overallFlowSummary = summaries.joinToString("\n\n").ifBlank { "গল্প ও অনুচ্ছেদের প্রবাহ বিশ্লেষণ সম্পন্ন হয়েছে।" },
            dialogueFeedback = dialogue.distinct(),
            characterConsistencyNotes = characters.distinct(),
            timelineAndContinuityNotes = timeline.distinct(),
            tenseAndPersonNotes = tense.distinct(),
            redundancyNotes = redundancy.distinct(),
            paragraphStructureTips = paragraphs.distinct(),
            chunksAnalyzed = chunks.size
        )
    }

    suspend fun analyzeAiWritingSignalsWithAi(text: String): AiWritingReport {
        val sample = text.take(4500)
        val prompt = buildString {
            appendLine("তুমি একজন বাংলা ভাষাতত্ত্ব ও শৈলীবিজ্ঞান গবেষক।")
            appendLine("নিচের বাংলা লেখাটিতে AI-ধাঁচের গঠনের সম্ভাব্য ভাষাগত লক্ষণ (অতিরিক্ত একই ধরনের বাক্যগঠন, অস্বাভাবিক পুনরাবৃত্তি, টেমপ্লেট-ধাঁচের বাক্যাংশ, লেখার স্বাভাবিকতা ও শব্দবৈচিত্র্য) বিশ্লেষণ করো।")
            appendLine("সতর্কতা: কখনোই নিশ্চিতভাবে বলবে না যে লেখাটি AI বা মানুষ লিখেছে। ফলাফলকে কেবল সম্ভাব্য ভাষাগত ইঙ্গিত হিসেবে দেখাও।")
            appendLine("\n--- লেখা ---")
            appendLine(sample)
        }
        val rawJson = executeWithRetry {
            aiStyleModel.generateContent(prompt).text
                ?: throw IllegalStateException("AI লিখন বিশ্লেষণ পাওয়া যায়নি।")
        }
        val root = JSONObject(rawJson)
        return AiWritingReport(
            aiSignalConfidencePercent = root.optInt("aiSignalConfidencePercent", 30).coerceIn(0, 100),
            signalBandLabel = root.optString("signalBandLabel", "সম্ভাব্য শৈলীগত ইঙ্গিত"),
            naturalnessScore = root.optInt("naturalnessScore", 70).coerceIn(0, 100),
            lexicalDiversityScore = root.optInt("lexicalDiversityScore", 65).coerceIn(0, 100),
            sentenceUniformityNote = root.optString("sentenceUniformityNote"),
            repetitionSignals = jsonStringList(root.optJSONArray("repetitionSignals")),
            templatePhrasingSignals = jsonStringList(root.optJSONArray("templatePhrasingSignals")),
            detailedObservations = jsonStringList(root.optJSONArray("detailedObservations")),
            disclaimer = "গুরুত্বপূর্ণ: কোনো AI Detector শতভাগ নির্ভুলভাবে লেখার উৎস শনাক্ত করতে পারে না। এই বিশ্লেষণটি কেবল সম্ভাব্য শৈলীগত ইঙ্গিত মাত্র, নিশ্চিত রায় নয়।",
            fromAi = true
        )
    }

    private fun jsonStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optString(i).trim()
                if (item.isNotEmpty()) add(item)
            }
        }
    }
}
