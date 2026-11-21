package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.BengaliTextUtils
import com.example.core.ExportFormat
import com.example.core.ExportScope
import com.example.core.ManuscriptExporter
import com.example.data.local.LocalChapterEntity
import com.example.data.local.LocalRevisionEntity
import com.example.data.model.AnalysisTab
import com.example.data.model.DictionaryCategory
import com.example.data.model.ManuscriptCategory
import com.example.data.model.MeterType
import com.example.data.model.WritingStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorWorkspaceScreen(
    state: EditorUiState,
    chapters: List<LocalChapterEntity>,
    revisions: List<LocalRevisionEntity>,
    isOnline: Boolean,
    onNavigateBack: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateContent: (String) -> Unit,
    onUpdateCategory: (ManuscriptCategory) -> Unit,
    onUpdateWritingStyle: (WritingStyle) -> Unit,
    onUpdateMeter: (MeterType, Int) -> Unit,
    onSelectTab: (AnalysisTab) -> Unit,
    onRunAnalysis: (Boolean) -> Unit,
    onSaveDraft: () -> Unit,
    onApplySpell: (String) -> Unit,
    onIgnoreSpell: (String) -> Unit,
    onAddCustomWord: (String, DictionaryCategory) -> Unit,
    onApplyGrammar: (String, String, String) -> Unit,
    onIgnoreGrammar: (String) -> Unit,
    onAddChapter: (String) -> Unit,
    onSelectChapter: (LocalChapterEntity) -> Unit,
    onSelectFullManuscript: () -> Unit,
    onDeleteChapter: (String) -> Unit,
    onRestoreRevision: (LocalRevisionEntity) -> Unit,
    onBuildReportText: () -> String,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var showExportSheet by remember { mutableStateOf(false) }
    var showDiffDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var newChapterTitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("editor_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ফিরে যান")
                }
            },
            title = {
                Column {
                    Text(
                        text = state.category.labelBn,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    Text(
                        text = state.lastSavedLabel.ifBlank { "Sobdosuddhi.1.0" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = onSaveDraft,
                    modifier = Modifier.testTag("editor_save_button")
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = "খসড়া সংরক্ষণ করুন")
                }
                IconButton(
                    onClick = { showHistoryDialog = true },
                    modifier = Modifier.testTag("editor_history_button")
                ) {
                    Icon(Icons.Rounded.History, contentDescription = "পূর্ববর্তী সংস্করণ")
                }
                IconButton(
                    onClick = { showDiffDialog = true },
                    modifier = Modifier.testTag("editor_diff_button")
                ) {
                    Icon(Icons.Rounded.CompareArrows, contentDescription = "মূল বনাম সংশোধিত তুলনা")
                }
                IconButton(
                    onClick = { showExportSheet = true },
                    modifier = Modifier.testTag("editor_export_button")
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = "এক্সপোর্ট ও শেয়ার")
                }
            }
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Title & Metadata controls
                item {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = onUpdateTitle,
                        label = { Text("পান্ডুলিপির শিরোনাম") },
                        placeholder = { Text("যেমন: শরতের কবিতা / শেষ বিকেলের গল্প") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_title_input")
                    )
                }

                // Category & Writing Style chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ManuscriptCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = state.category == cat,
                                onClick = { onUpdateCategory(cat) },
                                label = { Text(cat.labelBn) }
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        WritingStyle.entries.forEach { style ->
                            FilterChip(
                                selected = state.writingStyle == style,
                                onClick = { onUpdateWritingStyle(style) },
                                label = { Text(style.labelBn) }
                            )
                        }
                    }
                }

                // 2. Chapter Bar (for Story, Novel, or Manuscript or whenever chapters exist)
                if (state.category in setOf(ManuscriptCategory.STORY, ManuscriptCategory.NOVEL, ManuscriptCategory.MANUSCRIPT) ||
                    chapters.isNotEmpty()
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "অধ্যায় ব্যবস্থাপনা (${BengaliTextUtils.toBengaliDigits(chapters.size)})",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    TextButton(
                                        onClick = { showAddChapterDialog = true },
                                        modifier = Modifier.testTag("add_chapter_button")
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("নতুন অধ্যায়")
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = state.activeChapterId == null,
                                        onClick = onSelectFullManuscript,
                                        label = { Text("সম্পূর্ণ পান্ডুলিপি") }
                                    )
                                    chapters.forEach { chap ->
                                        FilterChip(
                                            selected = state.activeChapterId == chap.id,
                                            onClick = { onSelectChapter(chap) },
                                            label = { Text(chap.title) },
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = { onDeleteChapter(chap.id) },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.DeleteOutline,
                                                        contentDescription = "অধ্যায় মুছুন",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Large Bengali Literary Editor Input
                item {
                    OutlinedTextField(
                        value = state.content,
                        onValueChange = onUpdateContent,
                        label = {
                            Text(
                                if (state.category == ManuscriptCategory.POETRY) {
                                    "এখানে কবিতা লিখুন বা পেস্ট করুন (বাংলা ইউনিকোড)"
                                } else {
                                    "এখানে আপনার বাংলা লেখা লিখুন বা পেস্ট করুন"
                                }
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        shape = RoundedCornerShape(16.dp),
                        minLines = 8,
                        maxLines = 18,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 210.dp)
                            .testTag("editor_content_input")
                    )
                }

                // 4. Live Metrics Strip
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatChip("শব্দ", BengaliTextUtils.toBengaliDigits(state.metrics.wordCount))
                            StatChip("বর্ণ", BengaliTextUtils.toBengaliDigits(state.metrics.charCount))
                            StatChip("অনুচ্ছেদ", BengaliTextUtils.toBengaliDigits(state.metrics.paragraphCount))
                            StatChip("পঙ্ক্তি", BengaliTextUtils.toBengaliDigits(state.metrics.lineCount))
                        }
                    }
                }

                // 5. Analysis Mode Tabs
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnalysisTab.entries.forEach { tab ->
                            FilterChip(
                                selected = state.activeTab == tab,
                                onClick = { onSelectTab(tab) },
                                label = { Text(tab.labelBn) },
                                modifier = Modifier.testTag("analysis_tab_${tab.name}")
                            )
                        }
                    }
                }

                // 6. Primary Analyze CTA + Progress / Error Retry
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onRunAnalysis(false) },
                                enabled = !state.isAnalyzing,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("run_analysis_button")
                            ) {
                                if (state.isAnalyzing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("বিশ্লেষণ চলছে...")
                                } else {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isOnline) "${state.activeTab.labelBn} (AI + নিয়ম) বিশ্লেষণ"
                                        else "${state.activeTab.labelBn} (অফলাইন পরীক্ষা)"
                                    )
                                }
                            }

                            if (!state.isAnalyzing && isOnline) {
                                OutlinedButton(
                                    onClick = { onRunAnalysis(true) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "পুনরায় বিশ্লেষণ")
                                }
                            }
                        }

                        if (state.isAnalyzing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            if (state.analysisProgressLabel.isNotBlank()) {
                                Text(
                                    text = state.analysisProgressLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (state.analysisError != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = state.analysisError,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    if (isOnline) {
                                        TextButton(onClick = { onRunAnalysis(true) }) {
                                            Text("আবার চেষ্টা করুন")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. Active Analysis Panel Results
                item {
                    when (state.activeTab) {
                        AnalysisTab.SPELL -> SpellAnalysisPanel(
                            issues = state.spellIssues,
                            onApply = onApplySpell,
                            onIgnore = onIgnoreSpell,
                            onAddToDictionary = onAddCustomWord
                        )
                        AnalysisTab.GRAMMAR -> GrammarAnalysisPanel(
                            issues = state.grammarIssues,
                            onApplyOption = onApplyGrammar,
                            onIgnore = onIgnoreGrammar
                        )
                        AnalysisTab.PROSODY -> ProsodyAnalysisPanel(
                            selectedMeter = state.preferredMeter,
                            expectedMatra = state.expectedMatra,
                            report = state.prosodyReport,
                            onSelectMeter = onUpdateMeter
                        )
                        AnalysisTab.POETRY -> PoetryReviewPanel(report = state.poetryReport)
                        AnalysisTab.FICTION -> FictionReviewPanel(report = state.fictionReport)
                        AnalysisTab.AI_STYLE -> AiWritingAnalysisPanel(report = state.aiWritingReport)
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    // --- Export & Share Modal Bottom Sheet ---
    if (showExportSheet) {
        var selectedScope by remember { mutableStateOf(ExportScope.CORRECTED_ONLY) }
        ModalBottomSheet(onDismissRequest = { showExportSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("এক্সপোর্ট ও শেয়ার (বাংলা Unicode)", style = MaterialTheme.typography.titleLarge)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportScope.entries.forEach { scope ->
                        FilterChip(
                            selected = selectedScope == scope,
                            onClick = { selectedScope = scope },
                            label = { Text(scope.labelBn) }
                        )
                    }
                }

                val preparedBody = remember(selectedScope, state.title, state.originalContent, state.content) {
                    ManuscriptExporter.buildExportContent(
                        title = state.title,
                        originalText = state.originalContent,
                        correctedText = state.content,
                        analysisSummary = onBuildReportText(),
                        scope = selectedScope
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.entries.forEach { fmt ->
                        FilledTonalButton(
                            onClick = {
                                val res = ManuscriptExporter.exportAndShareFile(
                                    context = context,
                                    title = state.title.ifBlank { "Sobdosuddhi" },
                                    bodyText = preparedBody,
                                    format = fmt
                                )
                                showExportSheet = false
                                onShowMessage(
                                    res.fold(
                                        onSuccess = { "${fmt.label} ফাইল তৈরি ও শেয়ারের জন্য প্রস্তুত হয়েছে।" },
                                        onFailure = { "এক্সপোর্ট ব্যর্থ: ${it.localizedMessage}" }
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_format_${fmt.name}")
                        ) {
                            Text(fmt.extension.uppercase())
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            ManuscriptExporter.copyToClipboard(context, state.title, preparedBody)
                            showExportSheet = false
                            onShowMessage("লেখা ক্লিপবোর্ডে কপি করা হয়েছে।")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("কপি করুন")
                    }
                    Button(
                        onClick = {
                            ManuscriptExporter.sharePlainText(context, state.title, preparedBody)
                            showExportSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("টেক্সট শেয়ার")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- Original vs Corrected Diff Comparison Dialog ---
    if (showDiffDialog) {
        val diffs = remember(state.originalContent, state.content) {
            BengaliTextUtils.buildLineDiff(state.originalContent, state.content)
        }
        AlertDialog(
            onDismissRequest = { showDiffDialog = false },
            title = { Text("মূল বনাম সংশোধিত তুলনা") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(diffs.size) { i ->
                        val d = diffs[i]
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "পঙ্ক্তি ${BengaliTextUtils.toBengaliDigits(d.lineNumber)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (d.changed) {
                                Text("− ${d.original}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                Text("+ ${d.corrected}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            } else {
                                Text(d.corrected, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiffDialog = false }) { Text("বন্ধ করুন") }
            }
        )
    }

    // --- Version History & Restore Dialog ---
    if (showHistoryDialog) {
        val dateFmt = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("পূর্ববর্তী সম্পাদনা ইতিহাস") },
            text = {
                if (revisions.isEmpty()) {
                    Text(
                        text = "এখনো কোনো পূর্ববর্তী সংস্করণ সংরক্ষিত হয়নি। ‘সংরক্ষণ’ বাটন চাপলে নতুন সংস্করণ তৈরি হবে।",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(revisions.size) { idx ->
                            val rev = revisions[idx]
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(rev.summary, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = dateFmt.format(Date(rev.createdAtMillis)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = rev.content.take(90) + if (rev.content.length > 90) "..." else "",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    FilledTonalButton(
                                        onClick = {
                                            onRestoreRevision(rev)
                                            showHistoryDialog = false
                                        }
                                    ) {
                                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("এই সংস্করণ ফিরিয়ে আনুন")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) { Text("বন্ধ করুন") }
            }
        )
    }

    // --- Add Chapter Dialog ---
    if (showAddChapterDialog) {
        AlertDialog(
            onDismissRequest = { showAddChapterDialog = false },
            title = { Text("নতুন অধ্যায় যুক্ত করুন") },
            text = {
                OutlinedTextField(
                    value = newChapterTitle,
                    onValueChange = { newChapterTitle = it },
                    label = { Text("অধ্যায়ের শিরোনাম") },
                    placeholder = { Text("যেমন: প্রথম অধ্যায় — নদীর ওপার") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddChapter(newChapterTitle)
                        newChapterTitle = ""
                        showAddChapterDialog = false
                    }
                ) { Text("যুক্ত করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showAddChapterDialog = false }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
