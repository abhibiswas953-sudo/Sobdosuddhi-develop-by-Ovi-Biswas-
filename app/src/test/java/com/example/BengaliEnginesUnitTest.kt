package com.example

import com.example.core.BengaliTextUtils
import com.example.data.model.ManuscriptCategory
import com.example.data.model.MeterType
import com.example.data.model.WritingStyle
import com.example.engine.BengaliProsodyEngine
import com.example.engine.BengaliRuleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BengaliEnginesUnitTest {

    @Test
    fun spellEngine_detectsCommonBengaliErrorsAndRespectsCustomDictionary() {
        val sample = "কবির স্মরণে শ্রদ্ধাঞ্জলী ও পুরষ্কার দেওয়া হলো . এই মূহুর্তে কথা কথা বলো না।"
        val issues = BengaliRuleEngine.analyzeOfflineSpellAndRules(
            text = sample,
            customDictionary = emptySet(),
            writingStyle = WritingStyle.CHOLITO,
            category = ManuscriptCategory.GENERAL
        )

        val originals = issues.map { it.original }
        assertTrue("Expected শ্রদ্ধাঞ্জলী to be flagged", "শ্রদ্ধাঞ্জলী" in originals)
        assertTrue("Expected পুরষ্কার to be flagged", "পুরষ্কার" in originals)
        assertTrue("Expected মূহুর্তে or punctuation flagged", issues.size >= 3)

        // Protecting a word in Custom Dictionary prevents false flags
        val protectedIssues = BengaliRuleEngine.analyzeOfflineSpellAndRules(
            text = "শ্রদ্ধাঞ্জলী ও পুরষ্কার",
            customDictionary = setOf("শ্রদ্ধাঞ্জলী"),
            writingStyle = WritingStyle.CHOLITO,
            category = ManuscriptCategory.GENERAL
        )
        assertFalse(protectedIssues.any { it.original == "শ্রদ্ধাঞ্জলী" })
        assertTrue(protectedIssues.any { it.original == "পুরষ্কার" })
    }

    @Test
    fun grammarEngine_detectsSadhuCholitoMixingInProseButRespectsPoetry() {
        val mixedSentence = "তিনি বাড়ি যাইতেছেন কিন্তু এখনো পৌঁছাননি এবং সব কিছু দেখে চলে গেলেন।"
        val proseIssues = BengaliRuleEngine.analyzeOfflineGrammar(
            text = mixedSentence,
            writingStyle = WritingStyle.CHOLITO,
            category = ManuscriptCategory.STORY
        )
        assertTrue("Prose should detect sadhu-cholito mix", proseIssues.isNotEmpty())
        assertTrue(proseIssues.first().minimalCorrection.contains("যাচ্ছেন"))

        val poetryIssues = BengaliRuleEngine.analyzeOfflineGrammar(
            text = mixedSentence,
            writingStyle = WritingStyle.POETIC,
            category = ManuscriptCategory.POETRY
        )
        assertTrue("Poetry should preserve deliberate register blending", poetryIssues.isEmpty())
    }

    @Test
    fun prosodyEngine_appliesDistinctRulesForAksharbrittoMatrabrittoSwarabritto() {
        // Word with closed syllable: "বন্ধন" (বন্-ধন্)
        val line = "বন্ধন ছিন্ন"
        val swaraMatra = BengaliProsodyEngine.scanLineSyllables(line, MeterType.SWARABRITTO).sumOf { it.matra }
        val matraMatra = BengaliProsodyEngine.scanLineSyllables(line, MeterType.MATRABRITTO).sumOf { it.matra }
        val aksharMatra = BengaliProsodyEngine.scanLineSyllables(line, MeterType.AKSHARBRITTO).sumOf { it.matra }

        assertTrue(
            "Matrabritto ($matraMatra) should count closed syllables heavier than Swarabritto ($swaraMatra)",
            matraMatra > swaraMatra
        )
        assertTrue(
            "Aksharbritto ($aksharMatra) should be >= Swarabritto ($swaraMatra)",
            aksharMatra >= swaraMatra
        )

        val poem = """
            আকাশের বুকে জ্বলে তারার আলো
            সন্ধ্যার বাতাসে মন হয়ে যায় ভালো
        """.trimIndent()
        val report = BengaliProsodyEngine.analyzePoem(poem, MeterType.MATRABRITTO, 14)
        assertEquals(2, report.lineResults.size)
        assertEquals(MeterType.MATRABRITTO, report.detectedMeter)
        assertTrue("End rhyme scheme should detect matching rhyme", report.rhymeScheme.startsWith("ক-ক"))
    }

    @Test
    fun textUtils_computesBengaliMetricsChunkingAndDiffAccurately() {
        val text = "আকাশে সাদা মেঘ।\n\nনদীর তীরে কাশফুল ফুটেছে।"
        val metrics = BengaliTextUtils.computeMetrics(text)
        assertEquals(7, metrics.wordCount)
        assertEquals(2, metrics.paragraphCount)
        assertEquals("৭", BengaliTextUtils.toBengaliDigits(metrics.wordCount))

        val longParagraphs = (1..25).joinToString("\n\n") { idx ->
            "অনুচ্ছেদ $idx: বাংলার আকাশে শরতের রোদ আর সাদা মেঘের ভেলা ভেসে বেড়ায় প্রতিদিন সকালে ও বিকেলে।"
        }
        val chunks = BengaliTextUtils.chunkManuscript(longParagraphs, maxCharsPerChunk = 400)
        assertTrue("Long manuscript should be split into multiple chunks", chunks.size >= 3)
        assertTrue("Second chunk should carry previous context", chunks[1].previousContext.isNotBlank())

        val diff = BengaliTextUtils.buildLineDiff("পুরষ্কার পেলাম।", "পুরস্কার পেলাম।")
        assertEquals(1, diff.size)
        assertTrue(diff.first().changed)
    }

    @Test
    fun offlineAiSignalAnalyzer_alwaysIncludesNonAbsoluteDisclaimer() {
        val text = "পরিশেষে বলা যায় যে এটি অত্যন্ত গুরুত্বপূর্ণ যে আমাদের সাহিত্যচর্চা অব্যাহত রাখতে হবে। সংক্ষেপে বলা যায় যে ভাষা আমাদের প্রাণ।"
        val report = BengaliRuleEngine.analyzeOfflineAiSignals(text)
        assertTrue(report.aiSignalConfidencePercent in 0..100)
        assertTrue(report.templatePhrasingSignals.isNotEmpty())
        assertTrue(report.disclaimer.contains("শতভাগ"))
    }
}
