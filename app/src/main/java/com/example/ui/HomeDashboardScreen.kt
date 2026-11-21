package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.Subject
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.BengaliTextUtils
import com.example.data.local.LocalManuscriptEntity
import com.example.data.model.AnalysisTab
import com.example.data.model.ManuscriptCategory
import com.example.ui.theme.PlayfairDisplayFamily

data class DashboardToolItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)

@Composable
fun HomeDashboardScreen(
    manuscripts: List<LocalManuscriptEntity>,
    isOnline: Boolean,
    pendingSyncCount: Int,
    isSyncing: Boolean,
    onOpenNewWorkspace: (ManuscriptCategory, AnalysisTab) -> Unit,
    onResumeManuscript: (String) -> Unit,
    onOpenProjects: (Boolean) -> Unit, // true = recent history, false = saved projects
    onOpenDictionaryGuide: () -> Unit,
    onOpenSettings: () -> Unit,
    onSyncNow: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // All 11 requested Dashboard tools
    val coreTools = listOf(
        DashboardToolItem(
            id = "tool_new_check",
            title = "নতুন লেখা পরীক্ষা",
            subtitle = "দ্রুত বানান, ব্যাকরণ ও প্রবাহ যাচাই",
            icon = Icons.Rounded.EditNote,
            accentColor = primaryColor,
            onClick = { onOpenNewWorkspace(ManuscriptCategory.GENERAL, AnalysisTab.SPELL) }
        ),
        DashboardToolItem(
            id = "tool_poetry",
            title = "কবিতা সংশোধন",
            subtitle = "কাব্যভাব অক্ষুণ্ণ রেখে ছন্দ, মিল ও শব্দ বিশ্লেষণ",
            icon = Icons.Rounded.AutoAwesome,
            accentColor = secondaryColor,
            onClick = { onOpenNewWorkspace(ManuscriptCategory.POETRY, AnalysisTab.POETRY) }
        ),
        DashboardToolItem(
            id = "tool_story_novel",
            title = "গল্প ও উপন্যাস সংশোধন",
            subtitle = "অধ্যায়, সংলাপ ও চরিত্রের ধারাবাহিকতা",
            icon = Icons.Rounded.AutoStories,
            accentColor = tertiaryColor,
            onClick = { onOpenNewWorkspace(ManuscriptCategory.NOVEL, AnalysisTab.FICTION) }
        ),
        DashboardToolItem(
            id = "tool_full_manuscript",
            title = "সম্পূর্ণ পান্ডুলিপি পরীক্ষা",
            subtitle = "অংশভিত্তিক দীর্ঘ পান্ডুলিপি বিশ্লেষণ",
            icon = Icons.Rounded.Description,
            accentColor = primaryColor,
            onClick = { onOpenNewWorkspace(ManuscriptCategory.MANUSCRIPT, AnalysisTab.SPELL) }
        ),
        DashboardToolItem(
            id = "tool_spell",
            title = "বানান সংশোধন",
            subtitle = "যুক্তাক্ষর, কার-ফলা, ণ-ন ও শ-ষ-স যাচাই",
            icon = Icons.Rounded.Spellcheck,
            accentColor = tertiaryColor,
            onClick = { onOpenNewWorkspace(ManuscriptCategory.GENERAL, AnalysisTab.SPELL) }
        ),
        DashboardToolItem(
            id = "tool_grammar",
            title = "বাক্য গঠন বিশ্লেষণ",
            subtitle = "ন্যূনতম, ব্যাকরণসম্মত ও সাবলীল ৩ বিকল্প",
            icon = Icons.Rounded.Subject,
            accentColor = secondaryColor,
            onClick = { onOpenNewWorkspace(ManuscriptCategory.GENERAL, AnalysisTab.GRAMMAR) }
        ),
        DashboardToolItem(
            id = "tool_prosody",
            title = "ছন্দ ও মাত্রা বিশ্লেষণ",
            subtitle = "অক্ষরবৃত্ত, মাত্রাবৃত্ত ও স্বরবৃত্ত স্ক্যানার",
            icon = Icons.Rounded.MusicNote,
            accentColor = primaryColor,
            onClick = { onOpenNewWorkspace(ManuscriptCategory.POETRY, AnalysisTab.PROSODY) }
        ),
        DashboardToolItem(
            id = "tool_ai_style",
            title = "শব্দ ও বাক্য বিশ্লেষণ",
            subtitle = "AI লিখন লক্ষণ, বৈচিত্র্য ও স্বাভাবিকতা",
            icon = Icons.Rounded.Psychology,
            accentColor = tertiaryColor,
            onClick = { onOpenNewWorkspace(ManuscriptCategory.GENERAL, AnalysisTab.AI_STYLE) }
        )
    )

    val libraryTools = listOf(
        DashboardToolItem(
            id = "tool_previous_manuscripts",
            title = "পূর্ববর্তী পান্ডুলিপি",
            subtitle = "${BengaliTextUtils.toBengaliDigits(manuscripts.size)}টি সংরক্ষিত লেখা ও ইতিহাস",
            icon = Icons.Rounded.History,
            accentColor = secondaryColor,
            onClick = { onOpenProjects(true) }
        ),
        DashboardToolItem(
            id = "tool_saved_projects",
            title = "Saved Projects",
            subtitle = "অধ্যায় ও পিন করা প্রজেক্ট ব্যবস্থাপনা",
            icon = Icons.Rounded.FolderSpecial,
            accentColor = primaryColor,
            onClick = { onOpenProjects(false) }
        ),
        DashboardToolItem(
            id = "tool_dictionary_guide",
            title = "ডিকশনারি ও ব্যবহার নির্দেশিকা",
            subtitle = "নিজস্ব শব্দভাণ্ডার ও শুদ্ধ প্রয়োগের বিধি",
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            accentColor = tertiaryColor,
            onClick = onOpenDictionaryGuide
        ),
        DashboardToolItem(
            id = "tool_settings",
            title = "Settings",
            subtitle = "ফন্ট, থিম, অফলাইন মোড ও ব্যাকআপ",
            icon = Icons.Rounded.Settings,
            accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onOpenSettings
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .testTag("home_dashboard_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Literary Brand Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sobdosuddhi.1.0",
                            fontFamily = PlayfairDisplayFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Developed by Ovi Biswas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Connectivity / Sync status pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isOnline) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        modifier = Modifier
                            .clickable(enabled = isOnline && !isSyncing) { onSyncNow() }
                            .testTag("status_sync_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.Rounded.CloudSync else Icons.Rounded.CloudOff,
                                contentDescription = if (isOnline) "অনলাইন" else "অফলাইন",
                                tint = if (isOnline) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when {
                                    isSyncing -> "সিঙ্ক হচ্ছে..."
                                    !isOnline -> "অফলাইন মোড"
                                    pendingSyncCount > 0 -> "সিঙ্ক (${BengaliTextUtils.toBengaliDigits(pendingSyncCount)})"
                                    else -> "অনলাইন"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOnline) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }
                    }
                }
            }

            // 2. Offline Notice or Pending Cloud Sync Consent Banner
            if (!isOnline) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "অফলাইন মোডে কাজ করছেন",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "লেখা সম্পাদনা, সংরক্ষণ, নিয়মভিত্তিক বানান ও স্থানীয় ছন্দ বিশ্লেষণ চালু আছে। ইন্টারনেট ফিরে এলে আপনার অনুমতিতে সিঙ্ক হবে।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            } else if (pendingSyncCount > 0) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${BengaliTextUtils.toBengaliDigits(pendingSyncCount)}টি পান্ডুলিপি ক্লাউড সিঙ্কের অপেক্ষায়",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "অনুমতি দিলে নিরাপদে ক্লাউডে সংরক্ষিত হবে।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            FilledTonalButton(
                                onClick = onSyncNow,
                                enabled = !isSyncing,
                                modifier = Modifier.testTag("banner_sync_button")
                            ) {
                                Text("সিঙ্ক করুন")
                            }
                        }
                    }
                }
            }

            // 3. Hero Parchment Banner Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_literary_banner_1795249963251),
                            contentDescription = "সাহিত্য পান্ডুলিপি ডেস্ক",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(164.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(0.84f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "শুদ্ধ শব্দে প্রাণ পাক আপনার সাহিত্য",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "বাংলা বানান, ব্যাকরণ, ছন্দ ও পান্ডুলিপি সম্পাদনার নির্ভুল সহকারী।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Button(
                                onClick = { onOpenNewWorkspace(ManuscriptCategory.GENERAL, AnalysisTab.SPELL) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("hero_start_writing_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EditNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("নতুন লেখা শুরু করুন")
                            }
                        }
                    }
                }
            }

            // 4. Recent Manuscript Quick Resume
            val latest = manuscripts.firstOrNull()
            if (latest != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResumeManuscript(latest.id) }
                            .testTag("resume_recent_manuscript_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.LibraryBooks,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "সম্প্রতি সম্পাদিত পান্ডুলিপি",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = latest.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${ManuscriptCategory.fromRaw(latest.category).labelBn} • ${BengaliTextUtils.toBengaliDigits(latest.wordCount)} শব্দ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "খুলুন",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 5. Core Literary Proofreading & Analysis Tools (2-column adaptive grid rows)
            item {
                Text(
                    text = "সম্পাদনা ও বিশ্লেষণ সরঞ্জাম",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(coreTools.chunked(2).size) { rowIndex ->
                val pair = coreTools.chunked(2)[rowIndex]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { tool ->
                        DashboardToolCard(
                            item = tool,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 6. Projects, History, Dictionary & Settings
            item {
                Text(
                    text = "পান্ডুলিপি, ডিকশনারি ও সেটিংস",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(libraryTools.chunked(2).size) { rowIndex ->
                val pair = libraryTools.chunked(2)[rowIndex]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { tool ->
                        DashboardToolCard(
                            item = tool,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DashboardToolCard(
    item: DashboardToolItem,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = item.onClick)
            .testTag(item.id)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = item.accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = item.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
