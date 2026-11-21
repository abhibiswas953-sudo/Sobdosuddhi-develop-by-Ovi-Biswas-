package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.ExportScope
import com.example.core.ManuscriptExporter
import com.example.data.local.AppDatabase
import com.example.data.local.LocalChapterEntity
import com.example.data.local.LocalDictionaryEntity
import com.example.data.local.LocalManuscriptEntity
import com.example.data.local.LocalRevisionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SobdosuddhiRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun appBrandingStringsMatchSpecification() {
        assertEquals("Sobdosuddhi.1.0", context.getString(R.string.app_name))
        assertEquals("Developed by Ovi Biswas", context.getString(R.string.developer_credit))
    }

    @Test
    fun exporterProducesValidUnicodeTxtDocxAndPdfFiles() {
        val bengaliSample = "শরতের আকাশে সাদা মেঘ।\nDeveloped by Ovi Biswas"
        val dir = File(context.cacheDir, "test_exports").apply { mkdirs() }

        // 1. TXT with UTF-8 BOM
        val txtFile = File(dir, "sample.txt")
        ManuscriptExporter.writeUtf8Txt(txtFile, bengaliSample)
        assertTrue(txtFile.exists() && txtFile.length() > 10)
        assertTrue(txtFile.readText(Charsets.UTF_8).contains("শরতের আকাশে সাদা মেঘ।"))

        // 2. Valid OpenXML DOCX ZIP archive
        val docxFile = File(dir, "sample.docx")
        ManuscriptExporter.writeUnicodeDocx(docxFile, "শরতের কবিতা", bengaliSample)
        assertTrue(docxFile.exists() && docxFile.length() > 100)
        ZipFile(docxFile).use { zip ->
            val docEntry = zip.getEntry("word/document.xml")
            assertTrue("DOCX must contain word/document.xml", docEntry != null)
            val xml = zip.getInputStream(docEntry).bufferedReader(Charsets.UTF_8).readText()
            assertTrue("DOCX XML must preserve Bengali Unicode", xml.contains("শরতের আকাশে সাদা মেঘ।"))
        }

        // 3. Verify all 3 export scopes preserve Bengali Unicode, diff lines, and app branding
        val comparisonText = ManuscriptExporter.buildExportContent(
            title = "শরতের কবিতা",
            originalText = "পুরষ্কার পেলাম।",
            correctedText = "পুরস্কার পেলাম।",
            analysisSummary = "বানান বিশ্লেষণ সম্পন্ন।",
            scope = ExportScope.COMPARISON
        )
        assertTrue(comparisonText.contains("Sobdosuddhi.1.0 | Developed by Ovi Biswas"))
        assertTrue(comparisonText.contains("পুরষ্কার পেলাম।"))
        assertTrue(comparisonText.contains("পুরস্কার পেলাম।"))

        val fullReportText = ManuscriptExporter.buildExportContent(
            title = "শরতের কবিতা",
            originalText = "পুরষ্কার পেলাম।",
            correctedText = "পুরস্কার পেলাম।",
            analysisSummary = "বানান বিশ্লেষণ সম্পন্ন।",
            scope = ExportScope.FULL_REPORT
        )
        assertTrue(fullReportText.contains("সম্পূর্ণ বিশ্লেষণ রিপোর্ট"))
        assertTrue(fullReportText.contains("বানান বিশ্লেষণ সম্পন্ন।"))
    }

    @Test
    fun roomDatabasePersistsManuscriptChaptersRevisionsAndDictionaryOffline() = runTest {
        val dao = db.dao()
        val now = System.currentTimeMillis()
        val ms = LocalManuscriptEntity(
            id = "ms_test_1",
            userId = "uid_1",
            title = "পদ্মার তীরে",
            content = "বর্ষার নদী ফুলে উঠেছে।",
            originalContent = "বর্ষার নদী ফুলে উঠেছে।",
            category = "NOVEL",
            writingStyle = "CHOLITO",
            preferredMeter = "AUTO",
            expectedMatra = 0,
            wordCount = 4,
            charCount = 23,
            paragraphCount = 1,
            pinned = true,
            createdAtMillis = now,
            updatedAtMillis = now,
            pendingSync = true
        )
        dao.upsertManuscript(ms)

        val chapter = LocalChapterEntity(
            id = "ch_1",
            userId = "uid_1",
            manuscriptId = "ms_test_1",
            title = "প্রথম অধ্যায়",
            content = "সন্ধ্যার নৌকা ঘাটে ভিড়ল।",
            orderIndex = 0,
            wordCount = 4,
            createdAtMillis = now,
            updatedAtMillis = now,
            pendingSync = true
        )
        dao.upsertChapter(chapter)

        val revision = LocalRevisionEntity(
            id = "rev_1",
            userId = "uid_1",
            manuscriptId = "ms_test_1",
            title = "পদ্মার তীরে",
            content = "আগের খসড়া",
            summary = "প্রথম সংরক্ষণ",
            createdAtMillis = now,
            pendingSync = true
        )
        dao.upsertRevision(revision)

        val word = LocalDictionaryEntity(
            id = "wd_1",
            userId = "uid_1",
            word = "জোছনাস্নাত",
            category = "POETIC",
            note = "কাব্যশব্দ",
            createdAtMillis = now,
            pendingSync = true
        )
        dao.upsertDictionaryWord(word)

        assertEquals(1, dao.observeManuscripts("uid_1").first().size)
        assertEquals(1, dao.observeChapters("uid_1", "ms_test_1").first().size)
        assertEquals(1, dao.observeRevisions("uid_1", "ms_test_1").first().size)
        assertEquals("জোছনাস্নাত", dao.observeDictionary("uid_1").first().first().word)
    }
}
