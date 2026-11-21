package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.BengaliTextUtils
import com.example.core.NetworkMonitor
import com.example.core.TextMetrics
import com.example.data.local.LocalChapterEntity
import com.example.data.local.LocalDictionaryEntity
import com.example.data.local.LocalManuscriptEntity
import com.example.data.local.LocalRevisionEntity
import com.example.data.local.SettingsDataStore
import com.example.data.local.UserSettings
import com.example.data.model.AiWritingReport
import com.example.data.model.AnalysisTab
import com.example.data.model.AppThemeMode
import com.example.data.model.BengaliFontOption
import com.example.data.model.DictionaryCategory
import com.example.data.model.FictionContinuityReport
import com.example.data.model.GrammarIssue
import com.example.data.model.ManuscriptCategory
import com.example.data.model.MeterType
import com.example.data.model.PoetryReviewReport
import com.example.data.model.ProsodyReport
import com.example.data.model.SpellIssue
import com.example.data.model.WritingStyle
import com.example.data.remote.SobdosuddhiAiService
import com.example.data.repository.ManuscriptSyncRepository
import com.example.engine.BengaliProsodyEngine
import com.example.engine.BengaliRuleEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val TAG = "SobdosuddhiVM"

data class EditorUiState(
    val manuscriptId: String? = null,
    val title: String = "",
    val content: String = "",
    val originalContent: String = "",
    val category: ManuscriptCategory = ManuscriptCategory.GENERAL,
    val writingStyle: WritingStyle = WritingStyle.CHOLITO,
    val preferredMeter: MeterType = MeterType.AUTO,
    val expectedMatra: Int = 0,
    val pinned: Boolean = false,
    val activeChapterId: String? = null,
    val activeTab: AnalysisTab = AnalysisTab.SPELL,
    val metrics: TextMetrics = TextMetrics(0, 0, 0, 0, 0, 0, 0, 0),
    val spellIssues: List<SpellIssue> = emptyList(),
    val grammarIssues: List<GrammarIssue> = emptyList(),
    val prosodyReport: ProsodyReport? = null,
    val poetryReport: PoetryReviewReport? = null,
    val fictionReport: FictionContinuityReport? = null,
    val aiWritingReport: AiWritingReport? = null,
    val isAnalyzing: Boolean = false,
    val analysisProgressLabel: String = "",
    val analysisError: String? = null,
    val lastSavedLabel: String = "",
    val snackbarMessage: String? = null,
    val isSyncing: Boolean = false
)

class SobdosuddhiViewModel(
    private val currentUserId: String,
    private val repository: ManuscriptSyncRepository,
    private val aiService: SobdosuddhiAiService,
    private val settingsDataStore: SettingsDataStore,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsDataStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UserSettings()
    )

    private val rawOnlineState: StateFlow<Boolean> = networkMonitor.observeOnlineState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = networkMonitor.isCurrentlyOnline()
    )

    val isEffectivelyOnline: StateFlow<Boolean> = combine(rawOnlineState, settings) { online, prefs ->
        online && !prefs.offlineModeForced
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = networkMonitor.isCurrentlyOnline()
    )

    val manuscripts: StateFlow<List<LocalManuscriptEntity>> = repository.observeManuscripts(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val pendingSyncCount: StateFlow<Int> = repository.observePendingSyncCount(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )

    val dictionaryWords: StateFlow<List<LocalDictionaryEntity>> = repository.observeDictionary(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _chapters = MutableStateFlow<List<LocalChapterEntity>>(emptyList())
    val chapters: StateFlow<List<LocalChapterEntity>> = _chapters.asStateFlow()

    private val _revisions = MutableStateFlow<List<LocalRevisionEntity>>(emptyList())
    val revisions: StateFlow<List<LocalRevisionEntity>> = _revisions.asStateFlow()

    private val _editorState = MutableStateFlow(EditorUiState())
    val editorState: StateFlow<EditorUiState> = _editorState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var ruleDebounceJob: Job? = null
    private var chaptersObserverJob: Job? = null
    private var revisionsObserverJob: Job? = null

    init {
        // Trigger initial pull if user previously consented to cloud sync & online
        viewModelScope.launch {
            delay(500L)
            if (isEffectivelyOnline.value && settings.value.cloudSyncConsentGranted) {
                repository.syncWithCloud(currentUserId)
            }
        }
    }

    fun consumeSnackbar() {
        _editorState.update { it.copy(snackbarMessage = null) }
    }

    fun dismissAnalysisError() {
        _editorState.update { it.copy(analysisError = null) }
    }

    fun startNewWorkspace(
        category: ManuscriptCategory,
        initialTab: AnalysisTab,
        initialTitle: String = "",
        initialText: String = ""
    ) {
        val defaultStyle = if (category == ManuscriptCategory.POETRY) {
            WritingStyle.POETIC
        } else {
            settings.value.defaultWritingStyle
        }
        val metrics = BengaliTextUtils.computeMetrics(initialText)
        chaptersObserverJob?.cancel()
        revisionsObserverJob?.cancel()
        _chapters.value = emptyList()
        _revisions.value = emptyList()

        _editorState.value = EditorUiState(
            manuscriptId = null,
            title = initialTitle,
            content = initialText,
            originalContent = initialText,
            category = category,
            writingStyle = defaultStyle,
            preferredMeter = if (category == ManuscriptCategory.POETRY) MeterType.AUTO else MeterType.AUTO,
            expectedMatra = 0,
            activeTab = initialTab,
            metrics = metrics,
            lastSavedLabel = "নতুন খসড়া"
        )
        if (initialText.isNotBlank()) {
            scheduleQuickLocalScan(initialText)
        }
    }

    fun openManuscript(manuscriptId: String, targetTab: AnalysisTab? = null) {
        viewModelScope.launch {
            val ms = repository.getManuscript(manuscriptId) ?: return@launch
            val cat = ManuscriptCategory.fromRaw(ms.category)
            val style = WritingStyle.fromRaw(ms.writingStyle)
            val meter = MeterType.fromRaw(ms.preferredMeter)
            val tab = targetTab ?: when (cat) {
                ManuscriptCategory.POETRY -> AnalysisTab.POETRY
                ManuscriptCategory.STORY, ManuscriptCategory.NOVEL -> AnalysisTab.FICTION
                else -> AnalysisTab.SPELL
            }

            _editorState.value = EditorUiState(
                manuscriptId = ms.id,
                title = ms.title,
                content = ms.content,
                originalContent = ms.originalContent,
                category = cat,
                writingStyle = style,
                preferredMeter = meter,
                expectedMatra = ms.expectedMatra,
                pinned = ms.pinned,
                activeTab = tab,
                metrics = BengaliTextUtils.computeMetrics(ms.content),
                lastSavedLabel = if (ms.pendingSync) "স্থানীয়ভাবে সংরক্ষিত" else "সংরক্ষিত"
            )
            observeSubcollections(ms.id)
            scheduleQuickLocalScan(ms.content)
        }
    }

    private fun observeSubcollections(manuscriptId: String) {
        chaptersObserverJob?.cancel()
        revisionsObserverJob?.cancel()

        chaptersObserverJob = viewModelScope.launch {
            repository.observeChapters(currentUserId, manuscriptId).collect { list ->
                _chapters.value = list
            }
        }
        revisionsObserverJob = viewModelScope.launch {
            repository.observeRevisions(currentUserId, manuscriptId).collect { list ->
                _revisions.value = list
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _editorState.update { it.copy(title = newTitle) }
        scheduleAutoSaveIfEnabled()
    }

    fun updateContent(newContent: String) {
        val metrics = BengaliTextUtils.computeMetrics(newContent)
        _editorState.update { state ->
            val orig = if (state.originalContent.isEmpty() && state.manuscriptId == null && newContent.length > 20) {
                newContent
            } else {
                state.originalContent
            }
            state.copy(
                content = newContent,
                originalContent = orig,
                metrics = metrics
            )
        }
        scheduleAutoSaveIfEnabled()
        scheduleQuickLocalScan(newContent)
    }

    fun updateCategory(category: ManuscriptCategory) {
        _editorState.update { it.copy(category = category) }
        scheduleAutoSaveIfEnabled()
    }

    fun updateWritingStyle(style: WritingStyle) {
        _editorState.update { it.copy(writingStyle = style) }
        scheduleQuickLocalScan(_editorState.value.content)
        scheduleAutoSaveIfEnabled()
    }

    fun updateMeterSettings(meter: MeterType, expectedMatra: Int) {
        _editorState.update {
            it.copy(
                preferredMeter = meter,
                expectedMatra = expectedMatra.coerceIn(0, 64)
            )
        }
        runOfflineProsodyScan()
        scheduleAutoSaveIfEnabled()
    }

    fun selectAnalysisTab(tab: AnalysisTab) {
        _editorState.update { it.copy(activeTab = tab) }
        if (tab == AnalysisTab.PROSODY && _editorState.value.prosodyReport == null) {
            runOfflineProsodyScan()
        }
    }

    private fun scheduleAutoSaveIfEnabled() {
        if (!settings.value.autoSaveEnabled) return
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(2000L)
            val state = _editorState.value
            if (state.title.isNotBlank() || state.content.isNotBlank()) {
                persistCurrentManuscript(createRevision = false, silent = true)
            }
        }
    }

    private fun scheduleQuickLocalScan(text: String) {
        ruleDebounceJob?.cancel()
        ruleDebounceJob = viewModelScope.launch {
            delay(450L)
            val state = _editorState.value
            val customWords = dictionaryWords.value.map { it.word }.toSet()
            val (ruleSpell, ruleGrammar, localProsody) = withContext(Dispatchers.Default) {
                val spells = BengaliRuleEngine.analyzeOfflineSpellAndRules(
                    text = text,
                    customDictionary = customWords,
                    writingStyle = state.writingStyle,
                    category = state.category
                )
                val grammars = BengaliRuleEngine.analyzeOfflineGrammar(
                    text = text,
                    writingStyle = state.writingStyle,
                    category = state.category
                )
                val prosody = if (state.category == ManuscriptCategory.POETRY || state.activeTab == AnalysisTab.PROSODY) {
                    BengaliProsodyEngine.analyzePoem(
                        poemText = text,
                        selectedMeter = state.preferredMeter,
                        userExpectedMatra = state.expectedMatra
                    )
                } else null
                Triple(spells, grammars, prosody)
            }

            _editorState.update { cur ->
                // Preserve any active AI issues while refreshing local rule-based checks
                val aiSpellOnly = cur.spellIssues.filter { it.source == "AI" && !it.ignored && !it.applied }
                val mergedSpell = (ruleSpell + aiSpellOnly).distinctBy { "${it.original}_${it.suggestion}" }
                val aiGrammarOnly = cur.grammarIssues.filter { it.source == "AI" && !it.ignored }
                val mergedGrammar = (ruleGrammar + aiGrammarOnly).distinctBy { it.originalSentence }
                cur.copy(
                    spellIssues = mergedSpell,
                    grammarIssues = mergedGrammar,
                    prosodyReport = localProsody ?: cur.prosodyReport
                )
            }
        }
    }

    fun runOfflineProsodyScan() {
        val state = _editorState.value
        viewModelScope.launch {
            val report = withContext(Dispatchers.Default) {
                BengaliProsodyEngine.analyzePoem(
                    poemText = state.content,
                    selectedMeter = state.preferredMeter,
                    userExpectedMatra = state.expectedMatra
                )
            }
            _editorState.update { it.copy(prosodyReport = report) }
        }
    }

    fun saveDraftManually(revisionSummary: String = "ম্যানুয়াল সংরক্ষণ") {
        viewModelScope.launch {
            persistCurrentManuscript(createRevision = true, silent = false, revisionSummary = revisionSummary)
        }
    }

    private suspend fun persistCurrentManuscript(
        createRevision: Boolean,
        silent: Boolean,
        revisionSummary: String = "স্বয়ংক্রিয় সংরক্ষণ"
    ) {
        val state = _editorState.value
        val onlineSyncAllowed = isEffectivelyOnline.value && settings.value.cloudSyncConsentGranted

        // If editing a specific chapter, update the chapter and keep manuscript updated
        val activeChapId = state.activeChapterId
        if (activeChapId != null && state.manuscriptId != null) {
            val existingChap = _chapters.value.firstOrNull { it.id == activeChapId }
            if (existingChap != null) {
                repository.updateChapter(
                    existingChap.copy(content = state.content),
                    onlineAndAllowedToSync = onlineSyncAllowed
                )
            }
        }

        val currentId = state.manuscriptId
        val saved = if (currentId == null) {
            val created = repository.createManuscript(
                userId = currentUserId,
                title = state.title,
                content = state.content,
                category = state.category,
                writingStyle = state.writingStyle,
                preferredMeter = state.preferredMeter,
                expectedMatra = state.expectedMatra,
                onlineAndAllowedToSync = onlineSyncAllowed
            )
            observeSubcollections(created.id)
            created
        } else {
            val existing = repository.getManuscript(currentId)
            val entityToSave = (existing ?: LocalManuscriptEntity(
                id = currentId,
                userId = currentUserId,
                title = state.title,
                content = state.content,
                originalContent = state.originalContent,
                category = state.category.name,
                writingStyle = state.writingStyle.name,
                preferredMeter = state.preferredMeter.name,
                expectedMatra = state.expectedMatra,
                wordCount = state.metrics.wordCount,
                charCount = state.metrics.charCount,
                paragraphCount = state.metrics.paragraphCount,
                pinned = state.pinned,
                createdAtMillis = System.currentTimeMillis(),
                updatedAtMillis = System.currentTimeMillis(),
                pendingSync = true
            )).copy(
                title = state.title.ifBlank { existing?.title ?: "শিরোনামহীন পান্ডুলিপি" },
                content = state.content,
                originalContent = state.originalContent.ifBlank { state.content },
                category = state.category.name,
                writingStyle = state.writingStyle.name,
                preferredMeter = state.preferredMeter.name,
                expectedMatra = state.expectedMatra,
                pinned = state.pinned
            )
            repository.saveManuscript(entityToSave, onlineAndAllowedToSync = onlineSyncAllowed)
        }

        if (createRevision && saved.content.isNotBlank()) {
            repository.saveRevisionSnapshot(
                userId = currentUserId,
                manuscriptId = saved.id,
                title = saved.title,
                content = saved.content,
                summary = revisionSummary,
                onlineAndAllowedToSync = onlineSyncAllowed
            )
        }

        _editorState.update {
            it.copy(
                manuscriptId = saved.id,
                title = saved.title,
                lastSavedLabel = if (onlineSyncAllowed) "ক্লাউড ও লোকাল সংরক্ষিত" else "স্থানীয়ভাবে সংরক্ষিত",
                snackbarMessage = if (!silent) "পান্ডুলিপি সফলভাবে সংরক্ষিত হয়েছে।" else it.snackbarMessage
            )
        }
    }

    // --- Chapter Management ---
    fun addChapter(title: String) {
        viewModelScope.launch {
            if (_editorState.value.manuscriptId == null) {
                persistCurrentManuscript(createRevision = false, silent = true)
            }
            val msId = _editorState.value.manuscriptId ?: return@launch
            val onlineSyncAllowed = isEffectivelyOnline.value && settings.value.cloudSyncConsentGranted
            val created = repository.addChapter(
                userId = currentUserId,
                manuscriptId = msId,
                title = title,
                content = "",
                onlineAndAllowedToSync = onlineSyncAllowed
            )
            selectChapter(created)
            _editorState.update { it.copy(snackbarMessage = "নতুন অধ্যায় ‘${created.title}’ যুক্ত হয়েছে।") }
        }
    }

    fun selectChapter(chapter: LocalChapterEntity) {
        _editorState.update {
            it.copy(
                activeChapterId = chapter.id,
                content = chapter.content,
                metrics = BengaliTextUtils.computeMetrics(chapter.content)
            )
        }
        scheduleQuickLocalScan(chapter.content)
    }

    fun selectFullManuscriptView() {
        val msId = _editorState.value.manuscriptId ?: return
        viewModelScope.launch {
            val allChaps = _chapters.value
            val combined = if (allChaps.isNotEmpty()) {
                allChaps.joinToString("\n\n") { "【${it.title}】\n${it.content}" }
            } else {
                repository.getManuscript(msId)?.content ?: _editorState.value.content
            }
            _editorState.update {
                it.copy(
                    activeChapterId = null,
                    content = combined,
                    metrics = BengaliTextUtils.computeMetrics(combined)
                )
            }
        }
    }

    fun deleteChapter(chapterId: String) {
        val msId = _editorState.value.manuscriptId ?: return
        viewModelScope.launch {
            val onlineSyncAllowed = isEffectivelyOnline.value && settings.value.cloudSyncConsentGranted
            repository.deleteChapter(msId, chapterId, onlineSyncAllowed)
            if (_editorState.value.activeChapterId == chapterId) {
                _editorState.update { it.copy(activeChapterId = null) }
            }
        }
    }

    // --- Revision Restore ---
    fun restoreRevision(revision: LocalRevisionEntity) {
        viewModelScope.launch {
            // Snapshot current state before restoring so no text is ever lost
            persistCurrentManuscript(createRevision = true, silent = true, revisionSummary = "পূর্ববর্তী সংস্করণ ফেরানোর আগে সংরক্ষিত")
            _editorState.update {
                it.copy(
                    title = revision.title,
                    content = revision.content,
                    metrics = BengaliTextUtils.computeMetrics(revision.content),
                    snackbarMessage = "পূর্ববর্তী সংস্করণ পুনরুদ্ধার করা হয়েছে।"
                )
            }
            persistCurrentManuscript(createRevision = false, silent = true)
            scheduleQuickLocalScan(revision.content)
        }
    }

    // --- Project Management actions ---
    fun renameManuscript(id: String, newTitle: String) {
        viewModelScope.launch {
            val existing = repository.getManuscript(id) ?: return@launch
            val onlineSyncAllowed = isEffectivelyOnline.value && settings.value.cloudSyncConsentGranted
            repository.saveManuscript(existing.copy(title = newTitle.trim()), onlineSyncAllowed)
            if (_editorState.value.manuscriptId == id) {
                _editorState.update { it.copy(title = newTitle.trim()) }
            }
        }
    }

    fun togglePinManuscript(id: String) {
        viewModelScope.launch {
            val existing = repository.getManuscript(id) ?: return@launch
            val onlineSyncAllowed = isEffectivelyOnline.value && settings.value.cloudSyncConsentGranted
            repository.saveManuscript(existing.copy(pinned = !existing.pinned), onlineSyncAllowed)
        }
    }

    fun deleteManuscript(id: String) {
        viewModelScope.launch {
            val onlineSyncAllowed = isEffectivelyOnline.value && settings.value.cloudSyncConsentGranted
            repository.deleteManuscript(id, onlineSyncAllowed)
            if (_editorState.value.manuscriptId == id) {
                startNewWorkspace(ManuscriptCategory.GENERAL, AnalysisTab.SPELL)
            }
            _editorState.update { it.copy(snackbarMessage = "পান্ডুলিপি মুছে ফেলা হয়েছে।") }
        }
    }

    // --- Custom Dictionary ---
    fun addCustomWord(word: String, category: DictionaryCategory, note: String) {
        val clean = word.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val onlineSyncAllowed = isEffectivelyOnline.value && settings.value.cloudSyncConsentGranted
            repository.addDictionaryWord(
                userId = currentUserId,
                word = clean,
                category = category,
                note = note,
                onlineAndAllowedToSync = onlineSyncAllowed
            )
            // Remove any spell flags matching this protected word immediately
            _editorState.update { state ->
                state.copy(
                    spellIssues = state.spellIssues.filterNot { it.original == clean },
                    snackbarMessage = "‘$clean’ আপনার কাস্টম ডিকশনারিতে সংরক্ষিত হয়েছে।"
                )
            }
        }
    }

    fun deleteCustomWord(wordId: String) {
        viewModelScope.launch {
            val onlineSyncAllowed = isEffectivelyOnline.value && settings.value.cloudSyncConsentGranted
            repository.deleteDictionaryWord(wordId, onlineSyncAllowed)
        }
    }

    // --- Non-destructive Apply / Ignore Actions (Never alters text without explicit click) ---
    fun applySpellSuggestion(issueId: String) {
        val state = _editorState.value
        val issue = state.spellIssues.firstOrNull { it.id == issueId } ?: return
        if (issue.applied || issue.ignored) return

        val updatedText = replaceOccurrenceSafely(state.content, issue.original, issue.suggestion, issue.startIndex)
        _editorState.update { cur ->
            cur.copy(
                content = updatedText,
                metrics = BengaliTextUtils.computeMetrics(updatedText),
                spellIssues = cur.spellIssues.map {
                    if (it.id == issueId) it.copy(applied = true) else it
                },
                snackbarMessage = "‘${issue.original}’ → ‘${issue.suggestion}’ প্রয়োগ করা হয়েছে।"
            )
        }
        scheduleAutoSaveIfEnabled()
    }

    fun ignoreSpellIssue(issueId: String) {
        _editorState.update { cur ->
            cur.copy(
                spellIssues = cur.spellIssues.map {
                    if (it.id == issueId) it.copy(ignored = true) else it
                }
            )
        }
    }

    fun applyGrammarOption(issueId: String, optionLabel: String, replacement: String) {
        val state = _editorState.value
        val issue = state.grammarIssues.firstOrNull { it.id == issueId } ?: return
        val updatedText = state.content.replaceFirst(issue.originalSentence, replacement)
        _editorState.update { cur ->
            cur.copy(
                content = updatedText,
                metrics = BengaliTextUtils.computeMetrics(updatedText),
                grammarIssues = cur.grammarIssues.map {
                    if (it.id == issueId) it.copy(appliedOption = optionLabel) else it
                },
                snackbarMessage = "বাক্য সংশোধন ($optionLabel) প্রয়োগ করা হয়েছে।"
            )
        }
        scheduleAutoSaveIfEnabled()
    }

    fun ignoreGrammarIssue(issueId: String) {
        _editorState.update { cur ->
            cur.copy(
                grammarIssues = cur.grammarIssues.map {
                    if (it.id == issueId) it.copy(ignored = true) else it
                }
            )
        }
    }

    private fun replaceOccurrenceSafely(
        text: String,
        target: String,
        replacement: String,
        hintIndex: Int
    ): String {
        if (target.isEmpty()) return text
        if (hintIndex >= 0 && hintIndex + target.length <= text.length &&
            text.substring(hintIndex, hintIndex + target.length) == target
        ) {
            return text.substring(0, hintIndex) + replacement + text.substring(hintIndex + target.length)
        }
        val idx = text.indexOf(target)
        return if (idx >= 0) {
            text.substring(0, idx) + replacement + text.substring(idx + target.length)
        } else text
    }

    // --- Deep Analysis Orchestrator (Rule-based + Cached Firebase AI Logic Gemini) ---
    fun runAnalysisForActiveTab(forceRefresh: Boolean = false) {
        val state = _editorState.value
        val text = state.content.trim()
        if (text.isEmpty()) {
            _editorState.update { it.copy(analysisError = "বিশ্লেষণ করার জন্য প্রথমে কিছু বাংলা লেখা টাইপ বা পেস্ট করুন।") }
            return
        }

        val customWords = dictionaryWords.value.map { it.word }.toSet()

        // Always update instant local analysis first so user gets immediate value
        if (state.activeTab == AnalysisTab.PROSODY) {
            runOfflineProsodyScan()
        } else if (state.activeTab == AnalysisTab.AI_STYLE) {
            val offlineAi = BengaliRuleEngine.analyzeOfflineAiSignals(text)
            _editorState.update { it.copy(aiWritingReport = offlineAi) }
        } else if (state.activeTab == AnalysisTab.SPELL) {
            val localSpell = BengaliRuleEngine.analyzeOfflineSpellAndRules(text, customWords, state.writingStyle, state.category)
            _editorState.update { it.copy(spellIssues = localSpell) }
        }

        if (!isEffectivelyOnline.value) {
            _editorState.update {
                it.copy(
                    analysisError = "আপনি এখন অফলাইন মোডে আছেন। স্থানীয় নিয়মভিত্তিক বানান ও ছন্দ বিশ্লেষণ সম্পন্ন হয়েছে; উন্নত AI বিশ্লেষণের জন্য ইন্টারনেট সংযোগ চালু করুন।"
                )
            }
            return
        }

        viewModelScope.launch {
            _editorState.update {
                it.copy(
                    isAnalyzing = true,
                    analysisError = null,
                    analysisProgressLabel = "Gemini AI বিশ্লেষণ প্রস্তুত হচ্ছে..."
                )
            }

            try {
                val chunkSize = settings.value.aiChunkSizeChars
                when (state.activeTab) {
                    AnalysisTab.SPELL -> runCachedSpellAi(text, customWords, state, chunkSize, forceRefresh)
                    AnalysisTab.GRAMMAR -> runCachedGrammarAi(text, state, chunkSize, forceRefresh)
                    AnalysisTab.PROSODY -> runProsodyAi(text, state)
                    AnalysisTab.POETRY -> runPoetryAi(text)
                    AnalysisTab.FICTION -> runFictionAi(text, chunkSize)
                    AnalysisTab.AI_STYLE -> runAiWritingStyleAi(text)
                }
                _editorState.update { it.copy(isAnalyzing = false, analysisProgressLabel = "") }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI analysis timed out", e)
                _editorState.update {
                    it.copy(
                        isAnalyzing = false,
                        analysisProgressLabel = "",
                        analysisError = "AI বিশ্লেষণ সময়সীমা অতিক্রম করেছে (৬০ সেকেন্ড)। অনুগ্রহ করে পুনরায় চেষ্টা করুন।"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI analysis failed", e)
                _editorState.update {
                    it.copy(
                        isAnalyzing = false,
                        analysisProgressLabel = "",
                        analysisError = "AI বিশ্লেষণে সমস্যা হয়েছে: ${e.localizedMessage ?: "নেটওয়ার্ক বা সার্ভার ত্রুটি"}। আপনি পুনরায় চেষ্টা করতে পারেন।"
                    )
                }
            }
        }
    }

    private suspend fun runCachedSpellAi(
        text: String,
        customWords: Set<String>,
        state: EditorUiState,
        chunkSize: Int,
        forceRefresh: Boolean
    ) {
        val localIssues = withContext(Dispatchers.Default) {
            BengaliRuleEngine.analyzeOfflineSpellAndRules(text, customWords, state.writingStyle, state.category)
        }
        val cacheKey = BengaliTextUtils.sha256("SPELL|${state.writingStyle.name}|${state.category.name}|$text")
        val cached = if (!forceRefresh) repository.getCachedJson(cacheKey) else null

        val aiIssues = if (cached != null) {
            deserializeSpellIssues(cached, text, customWords)
        } else {
            val fetched = aiService.analyzeSpellingWithAi(
                text = text,
                customDictionary = customWords,
                writingStyle = state.writingStyle,
                category = state.category,
                chunkSize = chunkSize
            ) { idx, total ->
                _editorState.update {
                    it.copy(
                        analysisProgressLabel = "বানান বিশ্লেষণ চলছে (অংশ ${BengaliTextUtils.toBengaliDigits(idx)}/${BengaliTextUtils.toBengaliDigits(total)})..."
                    )
                }
            }
            repository.putCachedJson(cacheKey, "SPELL", serializeSpellIssues(fetched))
            fetched
        }

        val merged = (localIssues + aiIssues).distinctBy { "${it.original}_${it.suggestion}" }
        _editorState.update { it.copy(spellIssues = merged) }
    }

    private suspend fun runCachedGrammarAi(
        text: String,
        state: EditorUiState,
        chunkSize: Int,
        forceRefresh: Boolean
    ) {
        val localGrammar = withContext(Dispatchers.Default) {
            BengaliRuleEngine.analyzeOfflineGrammar(text, state.writingStyle, state.category)
        }
        val cacheKey = BengaliTextUtils.sha256("GRAMMAR|${state.writingStyle.name}|${state.category.name}|$text")
        val cached = if (!forceRefresh) repository.getCachedJson(cacheKey) else null

        val aiIssues = if (cached != null) {
            deserializeGrammarIssues(cached)
        } else {
            val fetched = aiService.analyzeGrammarWithAi(
                text = text,
                writingStyle = state.writingStyle,
                category = state.category,
                preserveVoice = settings.value.preserveLiteraryVoice,
                chunkSize = chunkSize
            ) { idx, total ->
                _editorState.update {
                    it.copy(
                        analysisProgressLabel = "বাক্য ও ব্যাকরণ বিশ্লেষণ চলছে (${BengaliTextUtils.toBengaliDigits(idx)}/${BengaliTextUtils.toBengaliDigits(total)})..."
                    )
                }
            }
            repository.putCachedJson(cacheKey, "GRAMMAR", serializeGrammarIssues(fetched))
            fetched
        }

        val merged = (aiIssues + localGrammar).distinctBy { it.originalSentence }
        _editorState.update { it.copy(grammarIssues = merged) }
    }

    private suspend fun runProsodyAi(text: String, state: EditorUiState) {
        _editorState.update { it.copy(analysisProgressLabel = "ছন্দ, পর্ববিন্যাস ও কাব্যপরামর্শ বিশ্লেষণ চলছে...") }
        val localReport = withContext(Dispatchers.Default) {
            BengaliProsodyEngine.analyzePoem(text, state.preferredMeter, state.expectedMatra)
        }
        val (overallAdvice, lineSuggestions) = aiService.enrichProsodyWithAi(
            poemText = text,
            meter = localReport.detectedMeter,
            expectedMatra = localReport.expectedMatra
        )
        val enrichedLines = localReport.lineResults.map { line ->
            val aiHint = lineSuggestions[line.lineNumber]
            if (!aiHint.isNullOrBlank()) {
                line.copy(suggestion = aiHint)
            } else line
        }
        _editorState.update {
            it.copy(
                prosodyReport = localReport.copy(
                    lineResults = enrichedLines,
                    aiPoeticAdvice = overallAdvice,
                    fromAi = true
                )
            )
        }
    }

    private suspend fun runPoetryAi(text: String) {
        _editorState.update { it.copy(analysisProgressLabel = "কবিতার ভাব, অন্ত্যমিল ও শব্দপ্রয়োগ বিশ্লেষণ চলছে...") }
        val report = aiService.analyzePoetryWithAi(text)
        _editorState.update { it.copy(poetryReport = report) }
    }

    private suspend fun runFictionAi(text: String, chunkSize: Int) {
        val report = aiService.analyzeFictionWithAi(text, chunkSize) { idx, total ->
            _editorState.update {
                it.copy(
                    analysisProgressLabel = "গল্প/উপন্যাসের ধারাবাহিকতা বিশ্লেষণ (${BengaliTextUtils.toBengaliDigits(idx)}/${BengaliTextUtils.toBengaliDigits(total)})..."
                )
            }
        }
        _editorState.update { it.copy(fictionReport = report) }
    }

    private suspend fun runAiWritingStyleAi(text: String) {
        _editorState.update { it.copy(analysisProgressLabel = "ভাষার স্বাভাবিকতা ও শৈলীগত লক্ষণ বিশ্লেষণ চলছে...") }
        val report = aiService.analyzeAiWritingSignalsWithAi(text)
        _editorState.update { it.copy(aiWritingReport = report) }
    }

    // --- Cloud Sync with Explicit User Permission ---
    fun syncNowWithPermission() {
        if (!isEffectivelyOnline.value) {
            _editorState.update { it.copy(snackbarMessage = "ইন্টারনেট সংযোগ নেই। অনলাইন হলে পুনরায় চেষ্টা করুন।") }
            return
        }
        viewModelScope.launch {
            settingsDataStore.setCloudSyncConsent(true)
            _editorState.update { it.copy(isSyncing = true) }
            val result = repository.syncWithCloud(currentUserId)
            _editorState.update { state ->
                state.copy(
                    isSyncing = false,
                    snackbarMessage = result.fold(
                        onSuccess = { count ->
                            "নিরাপদ ক্লাউড সিঙ্ক সম্পন্ন (${BengaliTextUtils.toBengaliDigits(count)}টি আইটেম হালনাগাদ)।"
                        },
                        onFailure = { err ->
                            "ক্লাউড সিঙ্ক ব্যর্থ হয়েছে: ${err.localizedMessage ?: "অজানা ত্রুটি"}"
                        }
                    )
                )
            }
        }
    }

    // --- Settings Actions ---
    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    fun setBengaliFont(font: BengaliFontOption) = viewModelScope.launch { settingsDataStore.setBengaliFont(font) }
    fun setFontScale(scale: Float) = viewModelScope.launch { settingsDataStore.setFontScale(scale) }
    fun setDefaultWritingStyle(style: WritingStyle) = viewModelScope.launch { settingsDataStore.setDefaultWritingStyle(style) }
    fun setAutoSave(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setAutoSaveEnabled(enabled) }
    fun setOfflineModeForced(forced: Boolean) = viewModelScope.launch { settingsDataStore.setOfflineModeForced(forced) }
    fun setPreserveLiteraryVoice(preserve: Boolean) = viewModelScope.launch { settingsDataStore.setPreserveLiteraryVoice(preserve) }
    fun setAiChunkSize(size: Int) = viewModelScope.launch { settingsDataStore.setAiChunkSize(size) }

    fun clearAnalysisCache() = viewModelScope.launch {
        repository.clearAnalysisCache()
        _editorState.update { it.copy(snackbarMessage = "বিশ্লেষণ ক্যাশ পরিষ্কার করা হয়েছে।") }
    }

    fun deleteAllUserData(includeCloud: Boolean) = viewModelScope.launch {
        val result = repository.deleteAllUserData(currentUserId, includeCloud && isEffectivelyOnline.value)
        startNewWorkspace(ManuscriptCategory.GENERAL, AnalysisTab.SPELL)
        _editorState.update {
            it.copy(
                snackbarMessage = if (result.isSuccess) {
                    "আপনার সকল পান্ডুলিপি ও তথ্য স্থায়ীভাবে মুছে ফেলা হয়েছে।"
                } else {
                    "তথ্য মুছতে আংশিক সমস্যা হয়েছে: ${result.exceptionOrNull()?.localizedMessage}"
                }
            )
        }
    }

    // --- Export Report Builder ---
    fun buildCompleteAnalysisReportText(): String {
        val state = _editorState.value
        return buildString {
            appendLine("পরিসংখ্যান: শব্দ ${BengaliTextUtils.toBengaliDigits(state.metrics.wordCount)}, বর্ণ ${BengaliTextUtils.toBengaliDigits(state.metrics.charCount)}, অনুচ্ছেদ ${BengaliTextUtils.toBengaliDigits(state.metrics.paragraphCount)}")
            appendLine()
            if (state.spellIssues.isNotEmpty()) {
                appendLine("১. বানান ও বিরামচিহ্ন পরামর্শ (${BengaliTextUtils.toBengaliDigits(state.spellIssues.size)}টি):")
                state.spellIssues.forEach { s ->
                    appendLine("• [${s.issueType}] ${s.original} → ${s.suggestion} (${s.reason})")
                }
                appendLine()
            }
            if (state.grammarIssues.isNotEmpty()) {
                appendLine("২. বাক্য গঠন ও ব্যাকরণ পরামর্শ (${BengaliTextUtils.toBengaliDigits(state.grammarIssues.size)}টি):")
                state.grammarIssues.forEach { g ->
                    appendLine("• মূল: ${g.originalSentence}")
                    appendLine("  কারণ: ${g.reason}")
                    appendLine("  A. ন্যূনতম: ${g.minimalCorrection}")
                    appendLine("  B. ব্যাকরণসম্মত: ${g.grammaticalCorrection}")
                    appendLine("  C. সাবলীল: ${g.fluentAlternative}")
                }
                appendLine()
            }
            state.prosodyReport?.let { p ->
                appendLine("৩. ছন্দ ও মাত্রা বিশ্লেষণ (${p.detectedMeter.labelBn}):")
                appendLine("প্রত্যাশিত মাত্রা: ${BengaliTextUtils.toBengaliDigits(p.expectedMatra)} | অন্ত্যমিল: ${p.rhymeScheme}")
                appendLine(p.continuitySummary)
                p.lineResults.forEach { l ->
                    appendLine("পঙ্ক্তি ${BengaliTextUtils.toBengaliDigits(l.lineNumber)} [${BengaliTextUtils.toBengaliDigits(l.actualMatra)} মাত্রা]: ${l.lineText}")
                    appendLine("  পর্ব: ${l.parbaBreakdown} — ${l.statusLabel}")
                }
                if (p.aiPoeticAdvice.isNotBlank()) {
                    appendLine("কাব্যপরামর্শ: ${p.aiPoeticAdvice}")
                }
                appendLine()
            }
            state.poetryReport?.let { pr ->
                appendLine("৪. কবিতা সম্পাদনা মূল্যায়ন:")
                appendLine(pr.toneAndVoiceSummary)
                appendLine("অন্ত্যমিল: ${pr.rhymeAnalysis}")
                appendLine()
            }
            state.fictionReport?.let { fr ->
                appendLine("৫. গল্প ও উপন্যাস ধারাবাহিকতা বিশ্লেষণ:")
                appendLine(fr.overallFlowSummary)
                appendLine()
            }
            state.aiWritingReport?.let { ar ->
                appendLine("৬. AI লিখন শৈলী বিশ্লেষণ:")
                appendLine("সম্ভাব্য AI লক্ষণ: ${BengaliTextUtils.toBengaliDigits(ar.aiSignalConfidencePercent)}% (${ar.signalBandLabel})")
                appendLine(ar.disclaimer)
            }
        }
    }

    // --- Lightweight JSON Cache Helpers ---
    private fun serializeSpellIssues(list: List<SpellIssue>): String {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("original", item.original)
                put("issueType", item.issueType)
                put("suggestion", item.suggestion)
                put("reason", item.reason)
            })
        }
        return arr.toString()
    }

    private fun deserializeSpellIssues(
        json: String,
        fullText: String,
        customDict: Set<String>
    ): List<SpellIssue> = runCatching {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val orig = o.optString("original")
                if (orig.isNotBlank() && orig !in customDict) {
                    add(
                        SpellIssue(
                            id = UUID.randomUUID().toString(),
                            original = orig,
                            issueType = o.optString("issueType"),
                            suggestion = o.optString("suggestion"),
                            reason = o.optString("reason"),
                            source = "AI",
                            startIndex = fullText.indexOf(orig)
                        )
                    )
                }
            }
        }
    }.getOrDefault(emptyList())

    private fun serializeGrammarIssues(list: List<GrammarIssue>): String {
        val arr = JSONArray()
        list.forEach { g ->
            arr.put(JSONObject().apply {
                put("originalSentence", g.originalSentence)
                put("issueType", g.issueType)
                put("reason", g.reason)
                put("minimalCorrection", g.minimalCorrection)
                put("grammaticalCorrection", g.grammaticalCorrection)
                put("fluentAlternative", g.fluentAlternative)
            })
        }
        return arr.toString()
    }

    private fun deserializeGrammarIssues(json: String): List<GrammarIssue> = runCatching {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    GrammarIssue(
                        id = UUID.randomUUID().toString(),
                        originalSentence = o.optString("originalSentence"),
                        issueType = o.optString("issueType"),
                        reason = o.optString("reason"),
                        minimalCorrection = o.optString("minimalCorrection"),
                        grammaticalCorrection = o.optString("grammaticalCorrection"),
                        fluentAlternative = o.optString("fluentAlternative"),
                        source = "AI"
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}
