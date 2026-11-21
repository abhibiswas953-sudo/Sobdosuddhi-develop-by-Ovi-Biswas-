package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.BengaliTextUtils
import com.example.data.model.AiWritingReport
import com.example.data.model.DictionaryCategory
import com.example.data.model.FictionContinuityReport
import com.example.data.model.GrammarIssue
import com.example.data.model.MeterType
import com.example.data.model.PoetryReviewReport
import com.example.data.model.ProsodyReport
import com.example.data.model.SpellIssue

@Composable
fun SpellAnalysisPanel(
    issues: List<SpellIssue>,
    onApply: (String) -> Unit,
    onIgnore: (String) -> Unit,
    onAddToDictionary: (String, DictionaryCategory) -> Unit
) {
    val activeIssues = issues.filter { !it.ignored }
    if (activeIssues.isEmpty()) {
        EmptyAnalysisCard(
            title = "কোনো বানান ত্রুটি শনাক্ত হয়নি",
            subtitle = "স্থানীয় নিয়মভিত্তিক পরীক্ষায় কোনো স্পষ্ট ভুল পাওয়া যায়নি। গভীর সাহিত্যিক বানান যাচাইয়ের জন্য ‘AI বিশ্লেষণ চালান’ চাপুন।"
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "শনাক্তকৃত পরামর্শ (${BengaliTextUtils.toBengaliDigits(activeIssues.size)}টি)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        activeIssues.forEach { issue ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spell_issue_card_${issue.id}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = issue.issueType,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (issue.source == "AI") "AI বিশ্লেষণ" else "নিয়মভিত্তিক",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "মূল: ${issue.original}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(text = "→", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "প্রস্তাব: ${issue.suggestion}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Text(
                        text = "কারণ: ${issue.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (issue.applied) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "সংশোধন প্রয়োগ করা হয়েছে",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onApply(issue.id) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("apply_spell_${issue.original}")
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("প্রয়োগ করুন")
                            }
                            OutlinedButton(
                                onClick = { onIgnore(issue.id) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("উপেক্ষা")
                            }
                            TextButton(
                                onClick = { onAddToDictionary(issue.original, DictionaryCategory.CUSTOM) }
                            ) {
                                Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("সংরক্ষণ")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GrammarAnalysisPanel(
    issues: List<GrammarIssue>,
    onApplyOption: (String, String, String) -> Unit,
    onIgnore: (String) -> Unit
) {
    val active = issues.filter { !it.ignored }
    if (active.isEmpty()) {
        EmptyAnalysisCard(
            title = "বাক্য ও ব্যাকরণ বিশ্লেষণ",
            subtitle = "প্রতিটি বাক্যের জন্য A (ন্যূনতম), B (ব্যাকরণসম্মত) ও C (আরও সাবলীল) — এই তিন স্তরের পরামর্শ পেতে ‘বিশ্লেষণ চালান’ চাপুন।"
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        active.forEach { issue ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = issue.issueType,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        TextButton(onClick = { onIgnore(issue.id) }) {
                            Text("উপেক্ষা")
                        }
                    }

                    Text(
                        text = "মূল বাক্য: ${issue.originalSentence}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "বিশ্লেষণ: ${issue.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    GrammarOptionRow(
                        tier = "A. ন্যূনতম সংশোধন",
                        text = issue.minimalCorrection,
                        applied = issue.appliedOption == "A",
                        onApply = { onApplyOption(issue.id, "A", issue.minimalCorrection) }
                    )
                    GrammarOptionRow(
                        tier = "B. ব্যাকরণসম্মত সংশোধন",
                        text = issue.grammaticalCorrection,
                        applied = issue.appliedOption == "B",
                        onApply = { onApplyOption(issue.id, "B", issue.grammaticalCorrection) }
                    )
                    GrammarOptionRow(
                        tier = "C. আরও সাবলীল বিকল্প",
                        text = issue.fluentAlternative,
                        applied = issue.appliedOption == "C",
                        onApply = { onApplyOption(issue.id, "C", issue.fluentAlternative) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GrammarOptionRow(
    tier: String,
    text: String,
    applied: Boolean,
    onApply: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (applied) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tier,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                FilledTonalButton(
                    onClick = onApply,
                    enabled = !applied
                ) {
                    Text(if (applied) "প্রয়োগকৃত" else "প্রয়োগ")
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ProsodyAnalysisPanel(
    selectedMeter: MeterType,
    expectedMatra: Int,
    report: ProsodyReport?,
    onSelectMeter: (MeterType, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Meter Selector Row
        Text(
            text = "ছন্দ নির্বাচন করুন (অক্ষরবৃত্ত, মাত্রাবৃত্ত ও স্বরবৃত্তের পৃথক নিয়ম):",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MeterType.entries.forEach { meter ->
                FilterChip(
                    selected = selectedMeter == meter,
                    onClick = { onSelectMeter(meter, expectedMatra) },
                    label = { Text(meter.labelBn) },
                    modifier = Modifier.testTag("meter_chip_${meter.name}")
                )
            }
        }

        // Expected Matra override chips
        val matraPresets = listOf(0, 10, 12, 14, 16, 18)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "প্রত্যাশিত মাত্রা:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            matraPresets.forEach { preset ->
                FilterChip(
                    selected = expectedMatra == preset,
                    onClick = { onSelectMeter(selectedMeter, preset) },
                    label = {
                        Text(if (preset == 0) "স্বয়ংক্রিয়" else BengaliTextUtils.toBengaliDigits(preset))
                    }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedMeter.shortRuleBn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(10.dp)
            )
        }

        if (report == null || report.lineResults.isEmpty()) {
            EmptyAnalysisCard(
                title = "ছন্দ ও মাত্রা বিশ্লেষক প্রস্তুত",
                subtitle = "কবিতার পঙ্ক্তি লিখুন — তাৎক্ষণিকভাবে স্থানীয় ছন্দ ইঞ্জিনে মাত্রা, পর্ববিভাজন ও অন্ত্যমিল দেখা যাবে।"
            )
            return
        }

        // Summary Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "নির্বাচিত/শনাক্ত ছন্দ: ${report.detectedMeter.labelBn}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "মিল: ${report.rhymeScheme}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = report.continuitySummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (report.aiPoeticAdvice.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "AI ছন্দ-পরামর্শ: ${report.aiPoeticAdvice}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Per-line Detailed Scan Results
        report.lineResults.forEach { line ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (line.possibleBreak) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "পঙ্ক্তি ${BengaliTextUtils.toBengaliDigits(line.lineNumber)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (line.possibleBreak) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer
                            }
                        ) {
                            Text(
                                text = "প্রাপ্ত ${BengaliTextUtils.toBengaliDigits(line.actualMatra)} / প্রত্যাশিত ${BengaliTextUtils.toBengaliDigits(line.expectedMatra)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (line.possibleBreak) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = line.lineText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "পর্ব বিভাজন: ${line.parbaBreakdown}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "ফলাফল: ${line.statusLabel} • অন্ত্যধ্বনি: ‘${line.endRhymeKey}’",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (line.possibleBreak) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                    )

                    if (line.possibleBreak) {
                        Text(
                            text = "অবস্থান: ${line.breakLocation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "পরামর্শ: ${line.suggestion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PoetryReviewPanel(report: PoetryReviewReport?) {
    if (report == null) {
        EmptyAnalysisCard(
            title = "কবিতা সম্পাদনা সহায়ক",
            subtitle = "কবিতার নিজস্ব ভাব, উপমা ও আবেগ অক্ষুণ্ণ রেখে অন্ত্যমিল, শব্দ পুনরাবৃত্তি, দুর্বল শব্দপ্রয়োগ ও পঙ্ক্তির প্রবাহ বিশ্লেষণ করতে ‘AI বিশ্লেষণ চালান’ চাপুন।"
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReportSectionCard(title = "কাব্যস্বর ও ভাবমূল্যায়ন", body = report.toneAndVoiceSummary)
        ReportSectionCard(title = "অন্ত্যমিল ও ধ্বনিসাম্য", body = report.rhymeAnalysis)

        if (report.repeatedWords.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("শব্দ পুনরাবৃত্তি", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        report.repeatedWords.forEach { word ->
                            AssistChip(onClick = {}, label = { Text(word) })
                        }
                    }
                }
            }
        }

        if (report.weakWordNotes.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("বিকল্প কাব্যশব্দ পরামর্শ", style = MaterialTheme.typography.titleSmall)
                    report.weakWordNotes.forEach { note ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "‘${note.originalWord}’ → ${note.alternatives.joinToString(", ")}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (note.contextLine.isNotBlank()) {
                                Text("পঙ্ক্তি: ${note.contextLine}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(note.reason, style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }

        if (report.lineFlowFeedback.isNotEmpty()) {
            BulletListCard(title = "পঙ্ক্তির প্রবাহ বিশ্লেষণ", items = report.lineFlowFeedback)
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = report.preservationNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun FictionReviewPanel(report: FictionContinuityReport?) {
    if (report == null) {
        EmptyAnalysisCard(
            title = "গল্প ও উপন্যাস ধারাবাহিকতা বিশ্লেষণ",
            subtitle = "সংলাপের স্বাভাবিকতা, চরিত্রের নাম, সময় ও ঘটনার ধারাবাহিকতা, কাল-পুরুষের সামঞ্জস্য এবং অনুচ্ছেদ বিভাজন পরীক্ষা করতে ‘AI বিশ্লেষণ চালান’ চাপুন।"
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReportSectionCard(
            title = "সামগ্রিক অনুচ্ছেদ প্রবাহ (${BengaliTextUtils.toBengaliDigits(report.chunksAnalyzed)} অংশে বিশ্লেষিত)",
            body = report.overallFlowSummary
        )
        if (report.dialogueFeedback.isNotEmpty()) {
            BulletListCard("সংলাপের স্বাভাবিকতা", report.dialogueFeedback)
        }
        if (report.characterConsistencyNotes.isNotEmpty()) {
            BulletListCard("চরিত্রের নামের ধারাবাহিকতা", report.characterConsistencyNotes)
        }
        if (report.timelineAndContinuityNotes.isNotEmpty()) {
            BulletListCard("সময় ও ঘটনার ধারাবাহিকতা", report.timelineAndContinuityNotes)
        }
        if (report.tenseAndPersonNotes.isNotEmpty()) {
            BulletListCard("কাল ও পুরুষের সামঞ্জস্য", report.tenseAndPersonNotes)
        }
        if (report.redundancyNotes.isNotEmpty()) {
            BulletListCard("তথ্যের অপ্রয়োজনীয় পুনরাবৃত্তি", report.redundancyNotes)
        }
        if (report.paragraphStructureTips.isNotEmpty()) {
            BulletListCard("অনুচ্ছেদ বিভাজন পরামর্শ", report.paragraphStructureTips)
        }
    }
}

@Composable
fun AiWritingAnalysisPanel(report: AiWritingReport?) {
    if (report == null) {
        EmptyAnalysisCard(
            title = "AI লিখন ও শব্দবৈচিত্র্য বিশ্লেষণ",
            subtitle = "বাক্যগঠনের একঘেয়েমি, পুনরাবৃত্তি, টেমপ্লেট-ধাঁচের ভাষা ও স্বাভাবিকতা পরীক্ষা করতে ‘বিশ্লেষণ চালান’ চাপুন।"
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("সম্ভাব্য AI-ধাঁচের লক্ষণ সূচক", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "${BengaliTextUtils.toBengaliDigits(report.aiSignalConfidencePercent)}%",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { report.aiSignalConfidencePercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = report.signalBandLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBadge(
                        label = "স্বাভাবিকতা",
                        value = "${BengaliTextUtils.toBengaliDigits(report.naturalnessScore)}%",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "শব্দবৈচিত্র্য",
                        value = "${BengaliTextUtils.toBengaliDigits(report.lexicalDiversityScore)}%",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        ReportSectionCard("বাক্যগঠনের বৈচিত্র্য", report.sentenceUniformityNote)
        if (report.repetitionSignals.isNotEmpty()) {
            BulletListCard("অস্বাভাবিক পুনরাবৃত্তি লক্ষণ", report.repetitionSignals)
        }
        if (report.templatePhrasingSignals.isNotEmpty()) {
            BulletListCard("টেমপ্লেট-ধাঁচের বাক্যাংশ", report.templatePhrasingSignals)
        }
        if (report.detailedObservations.isNotEmpty()) {
            BulletListCard("বিস্তারিত পর্যবেক্ষণ", report.detailedObservations)
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = report.disclaimer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun MetricBadge(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ReportSectionCard(title: String, body: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(body.ifBlank { "পর্যাপ্ত তথ্য পাওয়া যায়নি।" }, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BulletListCard(title: String, items: List<String>) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            items.forEach { item ->
                Text("• $item", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun EmptyAnalysisCard(title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
