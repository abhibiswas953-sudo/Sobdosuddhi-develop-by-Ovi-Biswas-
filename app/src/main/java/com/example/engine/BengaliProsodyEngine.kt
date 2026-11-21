package com.example.engine

import com.example.core.BengaliTextUtils
import com.example.data.model.MeterType
import com.example.data.model.ProsodyLineResult
import com.example.data.model.ProsodyReport
import com.example.data.model.SyllableUnit

/**
 * Dedicated Bengali Prosody & Meter Analysis Engine (বাংলা ছন্দ ও মাত্রা বিশ্লেষক).
 *
 * Distinct linguistic matra rules strictly separated by meter:
 * 1. অক্ষরবৃত্ত (Aksharbritto / তানপ্রধান):
 *    - মুক্তদল (Open syllable) = 1 মাত্রা
 *    - রুদ্ধদল (Closed syllable) at word start/middle = 1 মাত্রা
 *    - রুদ্ধদল at word end OR monosyllabic closed word = 2 মাত্রা
 * 2. মাত্রাবৃত্ত (Matrabritto / ধ্বনিপ্রধান):
 *    - মুক্তদল = 1 মাত্রা
 *    - প্রতিটি রুদ্ধদল (যে অবস্থানেই থাকুক) = 2 মাত্রা
 *    - যৌগিক স্বর (ঐ, ঔ) = 2 মাত্রা
 * 3. স্বরবৃত্ত (Swarabritto / শ্বাসাঘাতপ্রধান):
 *    - মুক্তদল = 1 মাত্রা, রুদ্ধদল = 1 মাত্রা (প্রতিটি দলই 1 মাত্রা)
 */
object BengaliProsodyEngine {

    private val INDEPENDENT_VOWELS = setOf('অ', 'আ', 'ই', 'ঈ', 'উ', 'ঊ', 'ঋ', 'এ', 'ঐ', 'ও', 'ঔ')
    private val DIPHTHONG_VOWELS = setOf('ঐ', 'ঔ', 'ৈ', 'ৌ')
    private val VOWEL_SIGNS = setOf('া', 'ি', 'ী', 'ু', 'ূ', 'ৃ', 'ে', 'ৈ', 'ো', 'ৌ')
    private val CONSONANTS = setOf(
        'ক', 'খ', 'গ', 'ঘ', 'ঙ', 'চ', 'ছ', 'জ', 'ঝ', 'ঞ',
        'ট', 'ঠ', 'ড', 'ঢ', 'ণ', 'ত', 'থ', 'দ', 'ধ', 'ন',
        'প', 'ফ', 'ব', 'ভ', 'ম', 'য', 'র', 'ল', 'শ', 'ষ',
        'স', 'হ', 'ড়', 'ঢ়', 'য়'
    )
    private val CODA_SIGNS = setOf('ং', 'ঃ', 'ৎ')
    private const val HASANTA = '্'
    private const val CHANDRABINDU = 'ঁ'

    // Words whose final consonant retains inherent 'অ'/'ও' vowel in standard Bengali pronunciation
    private val OPEN_ENDING_EXCEPTIONS = setOf(
        "ভালো", "কালো", "আলো", "কত", "যত", "তত", "মত", "নত", "শত", "গত", "মৃত",
        "প্রিয়", "শ্রেয়", "ছোট", "বড়", "প্রথম", "নব", "তব", "মম", "সদা", "কভু"
    )

    data class RawSyllable(
        val text: String,
        val closed: Boolean,
        val hasDiphthong: Boolean,
        val isWordFinal: Boolean,
        val isMonosyllabicWord: Boolean
    )

    fun analyzePoem(
        poemText: String,
        selectedMeter: MeterType = MeterType.AUTO,
        userExpectedMatra: Int = 0
    ): ProsodyReport {
        val rawLines = poemText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (rawLines.isEmpty()) {
            return ProsodyReport(
                selectedMeter = selectedMeter,
                detectedMeter = if (selectedMeter == MeterType.AUTO) MeterType.AKSHARBRITTO else selectedMeter,
                expectedMatra = userExpectedMatra,
                rhymeScheme = "—",
                continuitySummary = "ছন্দ বিশ্লেষণের জন্য অন্তত একটি কাব্যপঙ্ক্তি লিখুন।",
                lineResults = emptyList()
            )
        }

        // Auto-detect meter if AUTO by comparing line-to-line matra variance across the 3 meters
        val effectiveMeter = if (selectedMeter == MeterType.AUTO) {
            detectMostLikelyMeter(rawLines)
        } else {
            selectedMeter
        }

        // First pass: calculate raw matra counts for every line in effectiveMeter
        val scannedPerLine = rawLines.map { line ->
            val syllables = scanLineSyllables(line, effectiveMeter)
            syllables to syllables.sumOf { it.matra }
        }

        // Determine expected matra per line (user override or mode/dominant matra)
        val expectedMatra = if (userExpectedMatra > 0) {
            userExpectedMatra
        } else {
            inferDominantMatra(scannedPerLine.map { it.second }, effectiveMeter)
        }

        // Compute end-rhyme keys and scheme (ক, খ, গ...)
        val rhymeKeys = rawLines.map { extractEndRhymeKey(it) }
        val rhymeScheme = buildRhymeSchemeLabel(rhymeKeys)

        val lineResults = rawLines.mapIndexed { idx, line ->
            val (syllables, actualMatra) = scannedPerLine[idx]
            val diff = actualMatra - expectedMatra
            val possibleBreak = expectedMatra > 0 && diff != 0
            val parbaBreakdown = buildParbaBreakdown(syllables, effectiveMeter, expectedMatra)

            val (statusLabel, breakLocation, suggestion) = when {
                expectedMatra <= 0 -> Triple(
                    "মোট ${BengaliTextUtils.toBengaliDigits(actualMatra)} মাত্রা",
                    "সমগ্র পঙ্ক্তি",
                    "পর্ববিন্যাস অনুযায়ী পাঠ করে লয় যাচাই করুন।"
                )
                diff == 0 -> Triple(
                    "ছন্দসম্মত (${BengaliTextUtils.toBengaliDigits(actualMatra)} মাত্রা)",
                    "কোনো মাত্রাগত হেরফের শনাক্ত হয়নি",
                    "পঙ্ক্তিটির মাত্রাসাম্য প্রত্যাশিত কাঠামোর সঙ্গে সামঞ্জস্যপূর্ণ।"
                )
                diff < 0 -> {
                    val missing = -diff
                    val loc = locateLikelyBreakSegment(syllables, effectiveMeter)
                    Triple(
                        "সম্ভাব্য মাত্রার হেরফের (${BengaliTextUtils.toBengaliDigits(missing)} মাত্রা কম)",
                        loc,
                        "পঙ্ক্তিটিতে সম্ভাব্য ${BengaliTextUtils.toBengaliDigits(missing)} মাত্রা ঘাটতি রয়েছে। মূল ভাব ও অন্ত্যমিল বজায় রেখে একটি ${BengaliTextUtils.toBengaliDigits(missing)}-মাত্রার অব্যয় বা বিশেষণ যুক্ত করা যায়।"
                    )
                }
                else -> {
                    val extra = diff
                    val loc = locateLikelyBreakSegment(syllables, effectiveMeter)
                    Triple(
                        "সম্ভাব্য মাত্রার হেরফের (${BengaliTextUtils.toBengaliDigits(extra)} মাত্রা বেশি)",
                        loc,
                        "পঙ্ক্তিটিতে সম্ভাব্য ${BengaliTextUtils.toBengaliDigits(extra)} মাত্রা অতিরিক্ত হতে পারে। কাব্যভাব অক্ষুণ্ণ রেখে সমার্থক সংক্ষিপ্ত শব্দ প্রয়োগের কথা বিবেচনা করতে পারেন।"
                    )
                }
            }

            ProsodyLineResult(
                lineNumber = idx + 1,
                lineText = line,
                detectedMeter = effectiveMeter,
                expectedMatra = expectedMatra,
                actualMatra = actualMatra,
                syllables = syllables,
                parbaBreakdown = parbaBreakdown,
                endRhymeKey = rhymeKeys[idx],
                statusLabel = statusLabel,
                possibleBreak = possibleBreak,
                breakLocation = breakLocation,
                suggestion = suggestion
            )
        }

        val consistentCount = lineResults.count { !it.possibleBreak }
        val total = lineResults.size
        val continuitySummary = buildString {
            append("বিশ্লেষিত ছন্দ: ${effectiveMeter.labelBn}। ")
            append("প্রত্যাশিত মাত্রা: ${BengaliTextUtils.toBengaliDigits(expectedMatra)}। ")
            append("মোট ${BengaliTextUtils.toBengaliDigits(total)} পঙ্ক্তির মধ্যে ${BengaliTextUtils.toBengaliDigits(consistentCount)}টিতে সামঞ্জস্যপূর্ণ মাত্রা পাওয়া গেছে। ")
            if (consistentCount < total) {
                append("বাকি পঙ্ক্তিগুলোতে সম্ভাব্য মাত্রাগত হেরফের চিহ্নিত হয়েছে (আবৃত্তির টান ও যতিভেদে ভিন্নতা থাকতে পারে)।")
            } else {
                append("সম্পূর্ণ স্তবকে চমৎকার ছন্দ-ধারাবাহিকতা রয়েছে।")
            }
        }

        return ProsodyReport(
            selectedMeter = selectedMeter,
            detectedMeter = effectiveMeter,
            expectedMatra = expectedMatra,
            rhymeScheme = rhymeScheme,
            continuitySummary = continuitySummary,
            lineResults = lineResults
        )
    }

    fun scanLineSyllables(line: String, meter: MeterType): List<SyllableUnit> {
        val words = Regex("[\\u0980-\\u09FF]+").findAll(line).map { it.value }.toList()
        val units = mutableListOf<SyllableUnit>()
        for (word in words) {
            val rawSyllables = segmentWordIntoSyllables(word)
            for (raw in rawSyllables) {
                val matra = calculateMatra(raw, meter)
                units.add(SyllableUnit(text = raw.text, closed = raw.closed, matra = matra))
            }
        }
        return units
    }

    /**
     * Segments a Bengali Unicode word into phonological syllables (মুক্তদল / রুদ্ধদল)
     * accounting for conjuncts (যুক্তাক্ষর with হসন্ত), vowel signs, ফলা, রেফ, ং/ঃ/ৎ,
     * and Bengali final inherent-vowel deletion (অ-লোপ).
     */
    fun segmentWordIntoSyllables(word: String): List<RawSyllable> {
        if (word.isEmpty()) return emptyList()

        // Step 1: Parse word into orthographic aksaras (syllabic clusters)
        val clusters = parseOrthographicClusters(word)
        if (clusters.isEmpty()) return emptyList()

        // Step 2: Merge trailing vowelless consonant (অ-লোপ) onto previous syllable as coda (রুদ্ধদল)
        val merged = mutableListOf<ClusterInfo>()
        for (i in clusters.indices) {
            val c = clusters[i]
            val isLast = i == clusters.lastIndex

            // Pure coda mark or trailing bare consonant subject to Bengali final অ-লোপ
            val endsWithSilentInherentA = isLast &&
                merged.isNotEmpty() &&
                !c.hasExplicitVowel &&
                !c.endsWithHasanta &&
                !c.hasCodaMark &&
                !c.isConjunctCluster &&
                word !in OPEN_ENDING_EXCEPTIONS &&
                c.baseText !in setOf("হ", "য়")

            val explicitHasantaCoda = c.endsWithHasanta && merged.isNotEmpty()

            if (endsWithSilentInherentA || explicitHasantaCoda) {
                val prev = merged.removeAt(merged.lastIndex)
                merged.add(
                    prev.copy(
                        text = prev.text + c.text,
                        closed = true
                    )
                )
            } else {
                // Medial conjunct (C1 + ্ + C2) closes the preceding syllable in Bengali phonology
                // (except য-ফলা, র-ফলা, ব-ফলা at word start)
                if (c.splitsPrecedingSyllable && merged.isNotEmpty()) {
                    val prev = merged.removeAt(merged.lastIndex)
                    val firstC = c.leadingCodaChar
                    merged.add(
                        prev.copy(
                            text = prev.text + (firstC ?: ""),
                            closed = true
                        )
                    )
                    merged.add(
                        c.copy(
                            text = c.remainderAfterSplit.ifEmpty { c.text },
                            closed = c.hasCodaMark || (isLast && c.endsWithHasanta)
                        )
                    )
                } else {
                    merged.add(
                        c.copy(
                            closed = c.hasCodaMark || c.endsWithHasanta ||
                                (clusters.size == 1 && !c.hasExplicitVowel && word !in OPEN_ENDING_EXCEPTIONS && word.length >= 2)
                        )
                    )
                }
            }
        }

        val isMonosyllabic = merged.size == 1
        return merged.mapIndexed { idx, info ->
            RawSyllable(
                text = info.text,
                closed = info.closed,
                hasDiphthong = info.hasDiphthong,
                isWordFinal = idx == merged.lastIndex,
                isMonosyllabicWord = isMonosyllabic
            )
        }
    }

    private data class ClusterInfo(
        val text: String,
        val baseText: String,
        val hasExplicitVowel: Boolean,
        val hasDiphthong: Boolean,
        val hasCodaMark: Boolean,
        val endsWithHasanta: Boolean,
        val isConjunctCluster: Boolean,
        val splitsPrecedingSyllable: Boolean,
        val leadingCodaChar: String?,
        val remainderAfterSplit: String,
        val closed: Boolean = false
    )

    private fun parseOrthographicClusters(word: String): List<ClusterInfo> {
        val list = mutableListOf<ClusterInfo>()
        var i = 0
        val n = word.length

        while (i < n) {
            val ch = word[i]
            if (ch in INDEPENDENT_VOWELS) {
                val sb = StringBuilder().append(ch)
                var hasCoda = false
                i++
                while (i < n && (word[i] == CHANDRABINDU || word[i] in CODA_SIGNS)) {
                    if (word[i] in CODA_SIGNS) hasCoda = true
                    sb.append(word[i])
                    i++
                }
                list.add(
                    ClusterInfo(
                        text = sb.toString(),
                        baseText = ch.toString(),
                        hasExplicitVowel = true,
                        hasDiphthong = ch in DIPHTHONG_VOWELS,
                        hasCodaMark = hasCoda,
                        endsWithHasanta = false,
                        isConjunctCluster = false,
                        splitsPrecedingSyllable = false,
                        leadingCodaChar = null,
                        remainderAfterSplit = ""
                    )
                )
            } else if (ch in CONSONANTS) {
                val sb = StringBuilder().append(ch)
                val baseConsonants = mutableListOf(ch)
                var endsHasanta = false
                i++
                // Consume conjunct chain: ্ + consonant
                while (i < n && word[i] == HASANTA) {
                    sb.append(HASANTA)
                    i++
                    if (i < n && word[i] in CONSONANTS) {
                        sb.append(word[i])
                        baseConsonants.add(word[i])
                        i++
                    } else {
                        endsHasanta = true
                        break
                    }
                }
                var hasVowel = false
                var hasDiphthong = false
                if (i < n && word[i] in VOWEL_SIGNS) {
                    hasVowel = true
                    if (word[i] in DIPHTHONG_VOWELS) hasDiphthong = true
                    sb.append(word[i])
                    i++
                }
                var hasCoda = false
                while (i < n && (word[i] == CHANDRABINDU || word[i] in CODA_SIGNS)) {
                    if (word[i] in CODA_SIGNS) hasCoda = true
                    sb.append(word[i])
                    i++
                }

                val isConjunct = baseConsonants.size >= 2
                // Non-phala conjunct after the first syllable splits: C1 closes previous syllable, C2 starts new one.
                // Also য-ফলা/ব-ফলা/ম-ফলা/র-ফলা inside a word often geminates preceding vowel in Bengali (e.g. সত্য -> সৎ-ত, বিদ্যা -> বিদ্-দা)
                val second = baseConsonants.getOrNull(1)
                val splitsPrev = isConjunct && list.isNotEmpty() && !(baseConsonants.first() == 'ক' && second == 'ষ' && list.isEmpty())
                val leading = if (splitsPrev) "${baseConsonants.first()}$HASANTA" else null
                val rem = if (splitsPrev && sb.length > 2) sb.substring(2) else sb.toString()

                list.add(
                    ClusterInfo(
                        text = sb.toString(),
                        baseText = baseConsonants.joinToString(""),
                        hasExplicitVowel = hasVowel,
                        hasDiphthong = hasDiphthong,
                        hasCodaMark = hasCoda,
                        endsWithHasanta = endsHasanta,
                        isConjunctCluster = isConjunct,
                        splitsPrecedingSyllable = splitsPrev,
                        leadingCodaChar = leading,
                        remainderAfterSplit = rem
                    )
                )
            } else if (ch in CODA_SIGNS && list.isNotEmpty()) {
                val prev = list.removeAt(list.lastIndex)
                list.add(prev.copy(text = prev.text + ch, hasCodaMark = true))
                i++
            } else {
                i++
            }
        }
        return list
    }

    private fun calculateMatra(syllable: RawSyllable, meter: MeterType): Int {
        return when (meter) {
            MeterType.SWARABRITTO -> 1
            MeterType.MATRABRITTO -> {
                if (syllable.closed || syllable.hasDiphthong) 2 else 1
            }
            MeterType.AKSHARBRITTO, MeterType.AUTO -> {
                if (syllable.closed && (syllable.isWordFinal || syllable.isMonosyllabicWord)) {
                    2
                } else {
                    1
                }
            }
        }
    }

    private fun detectMostLikelyMeter(lines: List<String>): MeterType {
        val candidates = listOf(MeterType.AKSHARBRITTO, MeterType.MATRABRITTO, MeterType.SWARABRITTO)
        var bestMeter = MeterType.AKSHARBRITTO
        var bestScore = Double.MAX_VALUE

        for (meter in candidates) {
            val counts = lines.map { scanLineSyllables(it, meter).sumOf { s -> s.matra } }
            val avg = counts.average()
            val variance = counts.map { (it - avg) * (it - avg) }.average()
            // Favor canonical Bengali line lengths (8+6=14, 8+10=18 for Aksharbritto; 6+6=12, 6+6+2=14, 5+5=10 for Matrabritto; 4+4+4=12/16 for Swarabritto)
            val canonicalBonus = when (meter) {
                MeterType.AKSHARBRITTO -> if (avg.toInt() in setOf(14, 18, 16)) -1.2 else 0.0
                MeterType.MATRABRITTO -> if (avg.toInt() in setOf(12, 14, 16, 18, 20)) -0.8 else 0.0
                MeterType.SWARABRITTO -> if (avg.toInt() in setOf(8, 12, 14, 16)) -0.6 else 0.0
                else -> 0.0
            }
            val score = variance + canonicalBonus
            if (score < bestScore) {
                bestScore = score
                bestMeter = meter
            }
        }
        return bestMeter
    }

    private fun inferDominantMatra(counts: List<Int>, meter: MeterType): Int {
        if (counts.isEmpty()) return 14
        val freq = counts.groupingBy { it }.eachCount()
        val top = freq.entries.maxByOrNull { it.value }
        if (top != null && top.value >= 2) return top.key
        return when (meter) {
            MeterType.AKSHARBRITTO -> if (counts.average() >= 16.0) 18 else 14
            MeterType.MATRABRITTO -> counts.sorted()[counts.size / 2]
            MeterType.SWARABRITTO -> counts.sorted()[counts.size / 2]
            MeterType.AUTO -> counts.sorted()[counts.size / 2]
        }
    }

    private fun buildParbaBreakdown(
        syllables: List<SyllableUnit>,
        meter: MeterType,
        expectedMatra: Int
    ): String {
        if (syllables.isEmpty()) return ""
        val parbaTarget = when (meter) {
            MeterType.AKSHARBRITTO -> 8
            MeterType.MATRABRITTO -> if (expectedMatra % 6 == 0 || expectedMatra == 14) 6 else 5
            MeterType.SWARABRITTO -> 4
            MeterType.AUTO -> 8
        }

        val groups = mutableListOf<String>()
        val currentText = StringBuilder()
        var currentMatra = 0

        for (s in syllables) {
            if (currentMatra >= parbaTarget && currentText.isNotEmpty()) {
                groups.add("${currentText.toString().trim()} (${BengaliTextUtils.toBengaliDigits(currentMatra)})")
                currentText.clear()
                currentMatra = 0
            }
            currentText.append(s.text)
            if (s.closed) currentText.append("·")
            currentMatra += s.matra
        }
        if (currentText.isNotEmpty()) {
            groups.add("${currentText.toString().trim()} (${BengaliTextUtils.toBengaliDigits(currentMatra)})")
        }
        return groups.joinToString("  |  ")
    }

    private fun locateLikelyBreakSegment(syllables: List<SyllableUnit>, meter: MeterType): String {
        if (syllables.isEmpty()) return "পঙ্ক্তির প্রারম্ভ"
        val half = syllables.size / 2
        val firstHalfMatra = syllables.take(half).sumOf { it.matra }
        val secondHalfMatra = syllables.drop(half).sumOf { it.matra }
        val targetFirst = when (meter) {
            MeterType.AKSHARBRITTO -> 8
            MeterType.MATRABRITTO -> 6
            MeterType.SWARABRITTO -> 4
            MeterType.AUTO -> 8
        }
        return if (kotlin.math.abs(firstHalfMatra - targetFirst) > 1) {
            "পঙ্ক্তির প্রথম পর্বে (${syllables.take(half).joinToString("") { it.text }})"
        } else {
            "পঙ্ক্তির শেষ পর্বে (${syllables.drop(half).joinToString("") { it.text }})"
        }
    }

    fun extractEndRhymeKey(line: String): String {
        val lastWord = Regex("[\\u0980-\\u09FF]+").findAll(line).lastOrNull()?.value ?: return "—"
        return if (lastWord.length >= 2) lastWord.takeLast(2) else lastWord
    }

    private fun buildRhymeSchemeLabel(keys: List<String>): String {
        val labels = listOf("ক", "খ", "গ", "ঘ", "ঙ", "চ", "ছ", "জ", "ঝ", "ঞ")
        val assigned = mutableMapOf<String, String>()
        var nextIdx = 0
        return keys.map { key ->
            val normalized = key.takeLast(1)
            val matchKey = assigned.keys.firstOrNull { existing ->
                existing == key || (existing.length >= 2 && key.length >= 2 && existing.takeLast(2) == key.takeLast(2)) ||
                    (existing.lastOrNull() == normalized.lastOrNull() && key.length == 1)
            }
            if (matchKey != null) {
                assigned[matchKey]!!
            } else {
                val label = labels.getOrElse(nextIdx) { "অন্য" }
                nextIdx++
                assigned[key] = label
                label
            }
        }.joinToString("-")
    }
}
