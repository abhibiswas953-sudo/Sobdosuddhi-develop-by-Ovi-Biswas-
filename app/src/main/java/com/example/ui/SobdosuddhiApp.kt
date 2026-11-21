package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.core.NetworkMonitor
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.local.UserSettings
import com.example.data.model.AppThemeMode
import com.example.data.model.BengaliFontOption
import com.example.data.remote.FirestoreRepository
import com.example.data.remote.SobdosuddhiAiService
import com.example.data.repository.ManuscriptSyncRepository
import com.example.ui.theme.SobdosuddhiTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private enum class AppDestination(val labelBn: String, val icon: ImageVector) {
    HOME("হোম", Icons.Rounded.Home),
    EDITOR("সম্পাদনা", Icons.Rounded.EditNote),
    PROJECTS("পান্ডুলিপি", Icons.Rounded.FolderSpecial),
    DICTIONARY("ডিকশনারি", Icons.AutoMirrored.Rounded.MenuBook),
    SETTINGS("সেটিংস", Icons.Rounded.Settings)
}

private enum class SecondaryOverlay {
    NONE,
    PRIVACY_POLICY,
    TERMS_OF_USE
}

@Composable
fun SobdosuddhiApp() {
    val context = LocalContext.current
    val settingsStore = remember(context) { SettingsDataStore(context) }
    val userSettings by settingsStore.settingsFlow.collectAsStateWithLifecycle(initialValue = UserSettings())

    val darkTheme = when (userSettings.themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    SobdosuddhiTheme(
        darkTheme = darkTheme,
        useSerifFont = userSettings.bengaliFont == BengaliFontOption.NOTO_SERIF,
        fontScale = userSettings.fontScale
    ) {
        var splashDone by remember { mutableStateOf(false) }

        if (!splashDone) {
            SobdosuddhiSplashScreen(onFinished = { splashDone = true })
        } else {
            val auth = Firebase.auth
            val currentUser by auth.authStateFlow()
                .collectAsStateWithLifecycle(initialValue = auth.currentUser)
            val user = currentUser

            if (user == null) {
                SobdosuddhiAuthScreen(
                    onAuthSuccess = { /* AuthStateListener drives navigation */ }
                )
            } else {
                // Key composition and ViewModel by UID for clean isolation on account switch / sign-out
                key(user.uid) {
                    AuthenticatedSobdosuddhiShell(
                        currentUserId = user.uid,
                        userEmail = user.email
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedSobdosuddhiShell(
    currentUserId: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: SobdosuddhiViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                val remoteRepo = FirestoreRepository(firestore)
                val localDb = AppDatabase.getInstance(app)
                val syncRepo = ManuscriptSyncRepository(localDb.dao(), remoteRepo)
                val aiService = SobdosuddhiAiService()
                val settingsDataStore = SettingsDataStore(app)
                val networkMonitor = NetworkMonitor(app)
                SobdosuddhiViewModel(
                    currentUserId = currentUserId,
                    repository = syncRepo,
                    aiService = aiService,
                    settingsDataStore = settingsDataStore,
                    networkMonitor = networkMonitor
                )
            }
        }
    )

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isOnline by viewModel.isEffectivelyOnline.collectAsStateWithLifecycle()
    val manuscripts by viewModel.manuscripts.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val dictionaryWords by viewModel.dictionaryWords.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val revisions by viewModel.revisions.collectAsStateWithLifecycle()

    var currentDest by remember { mutableStateOf(AppDestination.HOME) }
    var overlay by remember { mutableStateOf(SecondaryOverlay.NONE) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(editorState.snackbarMessage) {
        val msg = editorState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    // Back handling for secondary overlays and sub-destinations
    BackHandler(enabled = overlay != SecondaryOverlay.NONE || currentDest != AppDestination.HOME) {
        if (overlay != SecondaryOverlay.NONE) {
            overlay = SecondaryOverlay.NONE
        } else {
            currentDest = AppDestination.HOME
        }
    }

    if (overlay == SecondaryOverlay.PRIVACY_POLICY) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { pad ->
            Box(modifier = Modifier.padding(pad)) {
                LegalDocumentScreen(
                    title = "Privacy Policy",
                    sections = PrivacyPolicySections,
                    onBack = { overlay = SecondaryOverlay.NONE }
                )
            }
        }
        return
    }

    if (overlay == SecondaryOverlay.TERMS_OF_USE) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { pad ->
            Box(modifier = Modifier.padding(pad)) {
                LegalDocumentScreen(
                    title = "Terms of Use",
                    sections = TermsOfUseSections,
                    onBack = { overlay = SecondaryOverlay.NONE }
                )
            }
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        AppDestination.entries.forEach { dest ->
                            NavigationBarItem(
                                selected = currentDest == dest,
                                onClick = { currentDest = dest },
                                icon = { Icon(dest.icon, contentDescription = dest.labelBn) },
                                label = { Text(dest.labelBn, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${dest.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        AppDestination.entries.forEach { dest ->
                            NavigationRailItem(
                                selected = currentDest == dest,
                                onClick = { currentDest = dest },
                                icon = { Icon(dest.icon, contentDescription = dest.labelBn) },
                                label = { Text(dest.labelBn, maxLines = 1) },
                                modifier = Modifier.testTag("rail_${dest.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentDest) {
                        AppDestination.HOME -> HomeDashboardScreen(
                            manuscripts = manuscripts,
                            isOnline = isOnline,
                            pendingSyncCount = pendingSyncCount,
                            isSyncing = editorState.isSyncing,
                            onOpenNewWorkspace = { category, tab ->
                                viewModel.startNewWorkspace(category, tab)
                                currentDest = AppDestination.EDITOR
                            },
                            onResumeManuscript = { id ->
                                viewModel.openManuscript(id)
                                currentDest = AppDestination.EDITOR
                            },
                            onOpenProjects = {
                                currentDest = AppDestination.PROJECTS
                            },
                            onOpenDictionaryGuide = {
                                currentDest = AppDestination.DICTIONARY
                            },
                            onOpenSettings = {
                                currentDest = AppDestination.SETTINGS
                            },
                            onSyncNow = { viewModel.syncNowWithPermission() }
                        )

                        AppDestination.EDITOR -> EditorWorkspaceScreen(
                            state = editorState,
                            chapters = chapters,
                            revisions = revisions,
                            isOnline = isOnline,
                            onNavigateBack = { currentDest = AppDestination.HOME },
                            onUpdateTitle = viewModel::updateTitle,
                            onUpdateContent = viewModel::updateContent,
                            onUpdateCategory = viewModel::updateCategory,
                            onUpdateWritingStyle = viewModel::updateWritingStyle,
                            onUpdateMeter = viewModel::updateMeterSettings,
                            onSelectTab = viewModel::selectAnalysisTab,
                            onRunAnalysis = viewModel::runAnalysisForActiveTab,
                            onSaveDraft = { viewModel.saveDraftManually() },
                            onApplySpell = viewModel::applySpellSuggestion,
                            onIgnoreSpell = viewModel::ignoreSpellIssue,
                            onAddCustomWord = { word, cat -> viewModel.addCustomWord(word, cat, "সম্পাদক থেকে সংরক্ষিত") },
                            onApplyGrammar = viewModel::applyGrammarOption,
                            onIgnoreGrammar = viewModel::ignoreGrammarIssue,
                            onAddChapter = viewModel::addChapter,
                            onSelectChapter = viewModel::selectChapter,
                            onSelectFullManuscript = viewModel::selectFullManuscriptView,
                            onDeleteChapter = viewModel::deleteChapter,
                            onRestoreRevision = viewModel::restoreRevision,
                            onBuildReportText = viewModel::buildCompleteAnalysisReportText,
                            onShowMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        )

                        AppDestination.PROJECTS -> ProjectsAndHistoryScreen(
                            manuscripts = manuscripts,
                            onOpenManuscript = { id ->
                                viewModel.openManuscript(id)
                                currentDest = AppDestination.EDITOR
                            },
                            onCreateNew = { category, tab ->
                                viewModel.startNewWorkspace(category, tab)
                                currentDest = AppDestination.EDITOR
                            },
                            onRename = viewModel::renameManuscript,
                            onTogglePin = viewModel::togglePinManuscript,
                            onDelete = viewModel::deleteManuscript
                        )

                        AppDestination.DICTIONARY -> DictionaryAndGuideScreen(
                            dictionaryWords = dictionaryWords,
                            onAddWord = viewModel::addCustomWord,
                            onDeleteWord = viewModel::deleteCustomWord
                        )

                        AppDestination.SETTINGS -> SettingsScreen(
                            settings = settings,
                            userEmail = userEmail,
                            pendingSyncCount = pendingSyncCount,
                            isOnline = isOnline,
                            isSyncing = editorState.isSyncing,
                            onSetThemeMode = viewModel::setThemeMode,
                            onSetFont = viewModel::setBengaliFont,
                            onSetFontScale = viewModel::setFontScale,
                            onSetWritingStyle = viewModel::setDefaultWritingStyle,
                            onSetAutoSave = viewModel::setAutoSave,
                            onSetOfflineMode = viewModel::setOfflineModeForced,
                            onSetPreserveVoice = viewModel::setPreserveLiteraryVoice,
                            onSetChunkSize = viewModel::setAiChunkSize,
                            onSyncNow = viewModel::syncNowWithPermission,
                            onClearCache = viewModel::clearAnalysisCache,
                            onDeleteAllData = viewModel::deleteAllUserData,
                            onOpenPrivacyPolicy = { overlay = SecondaryOverlay.PRIVACY_POLICY },
                            onOpenTermsOfUse = { overlay = SecondaryOverlay.TERMS_OF_USE },
                            onSignOut = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
