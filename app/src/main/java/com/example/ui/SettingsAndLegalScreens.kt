package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.BengaliTextUtils
import com.example.data.local.UserSettings
import com.example.data.model.AppThemeMode
import com.example.data.model.BengaliFontOption
import com.example.data.model.WritingStyle
import com.example.ui.theme.PlayfairDisplayFamily

@Composable
fun SettingsScreen(
    settings: UserSettings,
    userEmail: String?,
    pendingSyncCount: Int,
    isOnline: Boolean,
    isSyncing: Boolean,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetFont: (BengaliFontOption) -> Unit,
    onSetFontScale: (Float) -> Unit,
    onSetWritingStyle: (WritingStyle) -> Unit,
    onSetAutoSave: (Boolean) -> Unit,
    onSetOfflineMode: (Boolean) -> Unit,
    onSetPreserveVoice: (Boolean) -> Unit,
    onSetChunkSize: (Int) -> Unit,
    onSyncNow: () -> Unit,
    onClearCache: () -> Unit,
    onDeleteAllData: (Boolean) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTermsOfUse: () -> Unit,
    onSignOut: () -> Unit
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }

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
                .testTag("settings_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "সেটিংস ও পছন্দসমূহ",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = "সাইন ইন অ্যাকাউন্ট: $userEmail",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Appearance & Bengali Typography
            item {
                SettingsSectionCard(title = "দৃশ্যমানতা ও বাংলা ফন্ট") {
                    Text("থিম নির্বাচন (Light / Dark Mode)", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { onSetThemeMode(mode) },
                                label = { Text(mode.labelBn) },
                                modifier = Modifier.testTag("theme_chip_${mode.name}")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text("বাংলা ফন্ট নির্বাচন", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BengaliFontOption.entries.forEach { font ->
                            FilterChip(
                                selected = settings.bengaliFont == font,
                                onClick = { onSetFont(font) },
                                label = { Text(font.labelBn) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ফন্টের আকার", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "${BengaliTextUtils.toBengaliDigits((settings.fontScale * 100).toInt())}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = settings.fontScale,
                        onValueChange = onSetFontScale,
                        valueRange = 0.85f..1.35f,
                        steps = 4
                    )
                }
            }

            // 2. Writing, Auto Save & Offline Mode
            item {
                SettingsSectionCard(title = "লেখা, অটো-সেভ ও অফলাইন মোড") {
                    Text("ডিফল্ট বাংলা লেখার রীতি", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WritingStyle.entries.forEach { style ->
                            FilterChip(
                                selected = settings.defaultWritingStyle == style,
                                onClick = { onSetWritingStyle(style) },
                                label = { Text(style.labelBn) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    SettingsToggleRow(
                        title = "স্বয়ংক্রিয় সংরক্ষণ (Auto Save)",
                        subtitle = "টাইপিং থামার ২ সেকেন্ড পর লেখা নিরাপদে লোকাল ডেটাবেজে সংরক্ষিত হবে।",
                        checked = settings.autoSaveEnabled,
                        onCheckedChange = onSetAutoSave,
                        testTag = "toggle_auto_save"
                    )

                    SettingsToggleRow(
                        title = "অফলাইন মোড চালু রাখুন",
                        subtitle = "চালু থাকলে কোনো AI বা ক্লাউড কল হবে না; কেবল স্থানীয় বানান ও ছন্দ ইঞ্জিন চলবে।",
                        checked = settings.offlineModeForced,
                        onCheckedChange = onSetOfflineMode,
                        testTag = "toggle_offline_mode"
                    )
                }
            }

            // 3. AI Analysis Settings
            item {
                SettingsSectionCard(title = "AI বিশ্লেষণ সেটিংস") {
                    SettingsToggleRow(
                        title = "লেখকের স্বকীয় কণ্ঠস্বর ও কাব্যস্বাধীনতা রক্ষা",
                        subtitle = "AI যেন কবিতা বা গল্পের আবেগ, উপমা ও আঞ্চলিকতাকে কৃত্রিম ভাষায় বদলে না দেয়।",
                        checked = settings.preserveLiteraryVoice,
                        onCheckedChange = onSetPreserveVoice,
                        testTag = "toggle_preserve_voice"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("দীর্ঘ পান্ডুলিপি Chunk সীমা", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "${BengaliTextUtils.toBengaliDigits(settings.aiChunkSizeChars)} বর্ণ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = settings.aiChunkSizeChars.toFloat(),
                        onValueChange = { onSetChunkSize(it.toInt()) },
                        valueRange = 1200f..3600f,
                        steps = 3
                    )

                    OutlinedButton(
                        onClick = onClearCache,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("AI বিশ্লেষণ ক্যাশ পরিষ্কার করুন")
                    }
                }
            }

            // 4. Data Backup, Cloud Sync & Privacy Control
            item {
                SettingsSectionCard(title = "ডেটা ব্যাকআপ, সিঙ্ক ও নিয়ন্ত্রণ") {
                    Text(
                        text = if (pendingSyncCount > 0) {
                            "${BengaliTextUtils.toBengaliDigits(pendingSyncCount)}টি পান্ডুলিপি ক্লাউড ব্যাকআপের অপেক্ষায় রয়েছে।"
                        } else {
                            "সব পান্ডুলিপি নিরাপদে সংরক্ষিত আছে।"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onSyncNow,
                        enabled = isOnline && !isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_sync_button")
                    ) {
                        Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSyncing) "সিঙ্ক হচ্ছে..." else "এখনই নিরাপদ ক্লাউড ব্যাকআপ / সিঙ্ক করুন")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteAllDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("সব ডেটা মুছুন")
                        }
                        FilledTonalButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("সাইন আউট")
                        }
                    }
                }
            }

            // 5. Legal: Privacy Policy & Terms of Use
            item {
                SettingsSectionCard(title = "গোপনীয়তা ও ব্যবহারের শর্তাবলি") {
                    NavigationRowItem(
                        icon = Icons.Rounded.PrivacyTip,
                        title = "Privacy Policy (গোপনীয়তা নীতিমালা)",
                        onClick = onOpenPrivacyPolicy,
                        testTag = "open_privacy_policy"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationRowItem(
                        icon = Icons.Rounded.Description,
                        title = "Terms of Use (ব্যবহারের শর্তাবলি)",
                        onClick = onOpenTermsOfUse,
                        testTag = "open_terms_of_use"
                    )
                }
            }

            // 6. About App & Developer Info
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_app_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("About App & Developer Information", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "Sobdosuddhi.1.0",
                            fontFamily = PlayfairDisplayFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Developed by: Ovi Biswas",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Description: AI-powered Bengali Writing and Manuscript Proofreading Assistant.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "বাংলা কবি, গল্পকার, ঔপন্যাসিক ও সাহিত্য সম্পাদকদের জন্য নির্মিত — Unicode বাংলা বানান, ব্যাকরণ, অক্ষরবৃত্ত-মাত্রাবৃত্ত-স্বরবৃত্ত ছন্দ এবং নিরাপদ পান্ডুলিপি ব্যবস্থাপনা।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("সমস্ত পান্ডুলিপি ও ডেটা মুছবেন?") },
            text = {
                Text("আপনার ডিভাইস ও ক্লাউডে সংরক্ষিত সকল পান্ডুলিপি, অধ্যায়, সংস্করণ ইতিহাস এবং কাস্টম ডিকশনারি স্থায়ীভাবে মুছে যাবে।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllData(true)
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("হ্যাঁ, সব মুছে ফেলুন") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("বাতিল") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentScreen(
    title: String,
    sections: List<Pair<String, String>>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("legal_back_button")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ফিরে যান")
                }
            },
            title = { Text(title, style = MaterialTheme.typography.titleLarge) }
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Sobdosuddhi.1.0 • Developed by Ovi Biswas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                sections.forEach { (heading, body) ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(heading, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Text(body, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

val PrivacyPolicySections: List<Pair<String, String>> = listOf(
    "১. পান্ডুলিপির মালিকানা ও গোপনীয়তা" to
        "আপনার রচিত কবিতা, গল্প, উপন্যাস ও পান্ডুলিপি সম্পূর্ণ আপনার ব্যক্তিগত মেধাস্বত্ব। আপনার স্পষ্ট অনুরোধ ছাড়া কোনো লেখা তৃতীয় পক্ষের সঙ্গে শেয়ার করা হয় না।",
    "২. ডেটা সংরক্ষণ ও নিরাপদ ট্রান্সমিশন" to
        "অফলাইন মোডে আপনার সকল লেখা আপনার ডিভাইসের স্থানীয় এনক্রিপ্টেড অ্যাপ স্টোরেজে সংরক্ষিত থাকে। ক্লাউড ব্যাকআপ ও সিঙ্ক চালু করলে প্রতিটি পান্ডুলিপি কেবল আপনার ব্যক্তিগত Google অ্যাকাউন্টের অধীনে Firebase Firestore-এ কঠোর অ্যাক্সেস নিয়ন্ত্রণের মাধ্যমে সংরক্ষিত হয়।",
    "৩. AI বিশ্লেষণ ও অনুমতি" to
        "যখন আপনি ‘AI বিশ্লেষণ’ বাটন চাপেন, কেবল তখনই নির্বাচিত অংশটুকু নিরাপদ Firebase AI Logic সংযোগের মাধ্যমে প্রক্রিয়াকরণ করা হয়। অফলাইন মোডে কোনো লেখা সার্ভারে পাঠানো হয় না।",
    "৪. সম্পূর্ণ ডেটা নিয়ন্ত্রণ (এক্সপোর্ট ও ডিলিট)" to
        "আপনি যেকোনো সময় আপনার পান্ডুলিপি TXT, DOCX বা PDF ফরম্যাটে এক্সপোর্ট করতে পারেন এবং Settings থেকে স্থানীয় ও ক্লাউডের সকল ডেটা স্থায়ীভাবে মুছে ফেলতে পারেন।"
)

val TermsOfUseSections: List<Pair<String, String>> = listOf(
    "১. সহায়ক সম্পাদনা নীতি" to
        "Sobdosuddhi.1.0 একটি সাহিত্য সহায়ক ও প্রুফরিডিং টুল। ব্যবহারকারীর স্পষ্ট অনুমোদন (Apply Suggestion) ছাড়া এটি কখনোই মূল পান্ডুলিপি স্বয়ংক্রিয়ভাবে পরিবর্তন করে না।",
    "২. ছন্দ ও AI বিশ্লেষণের সীমাবদ্ধতা" to
        "বাংলা ছন্দবিচারে আবৃত্তির টান ও যতিভেদে ভিন্নতা থাকতে পারে; তাই ছন্দের অসামঞ্জস্যকে ‘সম্ভাব্য মাত্রার হেরফের’ হিসেবে প্রদর্শন করা হয়। একইভাবে AI Writing Analysis কেবল সম্ভাব্য শৈলীগত ইঙ্গিত দেয়, শতভাগ নিশ্চিত রায় নয়।",
    "৩. নির্মাতা ও ক্রেডিট" to
        "App Name: Sobdosuddhi.1.0\nDeveloped by: Ovi Biswas\nDescription: AI-powered Bengali Writing and Manuscript Proofreading Assistant."
)

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun NavigationRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
