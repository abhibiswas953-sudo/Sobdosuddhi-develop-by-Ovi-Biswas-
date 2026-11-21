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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.BengaliTextUtils
import com.example.data.local.LocalManuscriptEntity
import com.example.data.model.AnalysisTab
import com.example.data.model.ManuscriptCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsAndHistoryScreen(
    manuscripts: List<LocalManuscriptEntity>,
    onOpenManuscript: (String) -> Unit,
    onCreateNew: (ManuscriptCategory, AnalysisTab) -> Unit,
    onRename: (String, String) -> Unit,
    onTogglePin: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ManuscriptCategory?>(null) }
    var renameTarget by remember { mutableStateOf<LocalManuscriptEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<LocalManuscriptEntity?>(null) }

    val filtered = remember(manuscripts, searchQuery, selectedCategory) {
        manuscripts.filter { ms ->
            val matchesQuery = searchQuery.isBlank() ||
                ms.title.contains(searchQuery, ignoreCase = true) ||
                ms.content.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || ms.category == selectedCategory?.name
            matchesQuery && matchesCat
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "সংরক্ষিত পান্ডুলিপি ও প্রজেক্ট",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "মোট ${BengaliTextUtils.toBengaliDigits(manuscripts.size)}টি পান্ডুলিপি • স্বয়ংক্রিয় সংরক্ষণ ও সংস্করণ পুনরুদ্ধার সুবিধা",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("শিরোনাম বা শব্দ দিয়ে খুঁজুন...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("projects_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("সবগুলো") }
                )
                ManuscriptCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.labelBn) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = if (manuscripts.isEmpty()) "কোনো পান্ডুলিপি এখনো সংরক্ষিত নেই" else "অনুসন্ধানের সঙ্গে মিলে এমন কিছু পাওয়া যায়নি",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(
                            onClick = { onCreateNew(ManuscriptCategory.GENERAL, AnalysisTab.SPELL) },
                            modifier = Modifier.testTag("empty_projects_create_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("নতুন পান্ডুলিপি তৈরি করুন")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { ms ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenManuscript(ms.id) }
                                .testTag("manuscript_card_${ms.id}")
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
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = ManuscriptCategory.fromRaw(ms.category).labelBn,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (ms.pendingSync) Icons.Rounded.CloudQueue else Icons.Rounded.CloudDone,
                                            contentDescription = if (ms.pendingSync) "সিঙ্কের অপেক্ষায়" else "সিঙ্ক সম্পন্ন",
                                            tint = if (ms.pendingSync) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        IconButton(onClick = { onTogglePin(ms.id) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.PushPin,
                                                contentDescription = "পিন করুন",
                                                tint = if (ms.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(onClick = {
                                            renameTarget = ms
                                            renameText = ms.title
                                        }) {
                                            Icon(
                                                imageVector = Icons.Rounded.DriveFileRenameOutline,
                                                contentDescription = "নাম পরিবর্তন",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(onClick = { deleteTarget = ms }) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "মুছুন",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = ms.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (ms.content.isNotBlank()) {
                                    Text(
                                        text = ms.content.replace("\n", " "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = "${BengaliTextUtils.toBengaliDigits(ms.wordCount)} শব্দ • ${BengaliTextUtils.toBengaliDigits(ms.paragraphCount)} অনুচ্ছেদ • ${dateFormatter.format(Date(ms.updatedAtMillis))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { onCreateNew(selectedCategory ?: ManuscriptCategory.GENERAL, AnalysisTab.SPELL) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("projects_fab_new")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "নতুন পান্ডুলিপি")
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("পান্ডুলিপির নাম পরিবর্তন") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("নতুন শিরোনাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameText.isNotBlank()) onRename(target.id, renameText)
                    renameTarget = null
                }) { Text("সংরক্ষণ") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("বাতিল") }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("পান্ডুলিপি মুছবেন?") },
            text = { Text("‘${target.title}’ স্থায়ীভাবে মুছে যাবে। আপনি কি নিশ্চিত?") },
            confirmButton = {
                Button(onClick = {
                    onDelete(target.id)
                    deleteTarget = null
                }) { Text("মুছে ফেলুন") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("বাতিল") }
            }
        )
    }
}
