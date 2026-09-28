package dev.kiritoxd.miaopu.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dev.kiritoxd.miaopu.BuildConfig
import dev.kiritoxd.miaopu.data.AdapterResult
import dev.kiritoxd.miaopu.data.AdapterStatus
import dev.kiritoxd.miaopu.data.CommentPage
import dev.kiritoxd.miaopu.data.Esport
import dev.kiritoxd.miaopu.data.EsportCatalog
import dev.kiritoxd.miaopu.data.EsportSubscriptionStore
import dev.kiritoxd.miaopu.data.HupuAdapter
import dev.kiritoxd.miaopu.data.HupuCookieSession
import dev.kiritoxd.miaopu.data.HupuUrls
import dev.kiritoxd.miaopu.data.GitHubRelease
import dev.kiritoxd.miaopu.data.GitHubReleaseAdapter
import dev.kiritoxd.miaopu.data.MatchSummary
import dev.kiritoxd.miaopu.data.RatingDetail
import dev.kiritoxd.miaopu.data.RatingStage
import dev.kiritoxd.miaopu.data.RatingTarget
import dev.kiritoxd.miaopu.data.Schedule
import dev.kiritoxd.miaopu.data.StageRatingDetail
import dev.kiritoxd.miaopu.data.mergeCommentsByHeat
import dev.kiritoxd.miaopu.data.isNewerVersion
import dev.kiritoxd.miaopu.widget.WidgetUpdates
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Ready<T>(val value: T) : LoadState<T>
    data class Failed(val message: String, val retryable: Boolean) : LoadState<Nothing>
}

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data class UpToDate(val latestTag: String) : UpdateCheckState
    data class Available(val release: GitHubRelease) : UpdateCheckState
    data class Failed(val message: String) : UpdateCheckState
}

enum class MainSection {
    HOME,
    EVENTS,
    PROFILE,
}

class MiaopuViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val cookieSession = HupuCookieSession(application).also { it.restore() }
    private val adapter = HupuAdapter(cookieSession)
    private val releaseAdapter = GitHubReleaseAdapter()
    internal val matchDetail = MatchDetailController(viewModelScope, adapter::getStageRatingDetail)
    internal val commentReplies = CommentRepliesController(viewModelScope, adapter)
    private val subscriptionStore = EsportSubscriptionStore(application)
    private val initialSubscriptions = subscriptionStore.orderedSubscriptions()
    private var navigator: MiaopuNavigator? = null
    private var pendingWidgetDestination: Pair<MatchSummary?, Esport?>? = null
    private val schedules = mutableMapOf<Esport, Schedule>()
    private val scheduleViewports = mutableMapOf<Esport, ScheduleViewportSnapshot>()
    private val stageViewports = mutableMapOf<String, StageViewportSnapshot>()
    private val publishCommentGate = TargetRequestGate()
    private val commentPaginationGate = TargetRequestGate()

    internal val screen: AppScreen get() = navigator?.currentScreen ?: AppScreen.Schedule
    var subscribedEsports: List<Esport> by mutableStateOf(initialSubscriptions)
        private set
    var selectedEsport: Esport by mutableStateOf(subscriptionStore.selected(initialSubscriptions))
        private set
    var selectedMainSection: MainSection by mutableStateOf<MainSection>(
        savedStateHandle.get<String>(SELECTED_MAIN_SECTION_KEY)
            ?.let { saved -> MainSection.entries.firstOrNull { it.name == saved } }
            ?: MainSection.HOME,
    )
        private set
    private var scheduleStates: Map<Esport, LoadState<Schedule>> by mutableStateOf(emptyMap())
    var ratingState: LoadState<RatingDetail> by mutableStateOf(LoadState.Loading)
        private set
    var stageRatingState: LoadState<StageRatingDetail> by mutableStateOf(LoadState.Loading)
        private set
    var commentState: LoadState<CommentPage> by mutableStateOf(LoadState.Loading)
        private set
    var isLoggedIn: Boolean by mutableStateOf(cookieSession.isAuthenticated())
        private set
    var isLoadingMoreComments: Boolean by mutableStateOf(false)
        private set
    var commentPaginationError: String? by mutableStateOf(null)
        private set
    var commentDraft: String by mutableStateOf(savedStateHandle[COMMENT_DRAFT_KEY] ?: "")
        private set
    var isPublishingComment: Boolean by mutableStateOf(false)
        private set
    var lastCommentSubmissionSucceeded: Boolean by mutableStateOf(false)
        private set
    var message: String? by mutableStateOf(null)
        private set
    var updateCheckState: UpdateCheckState by mutableStateOf(UpdateCheckState.Idle)
        private set

    val currentVersion: String get() = BuildConfig.VERSION_NAME
    val repositoryUrl: String get() = GitHubReleaseAdapter.REPOSITORY_URL

    private val scheduleJobs = mutableMapOf<Esport, Job>()
    private var ratingJob: Job? = null
    private var stageRatingJob: Job? = null
    private var commentJob: Job? = null
    private var moreCommentsJob: Job? = null
    private var publishCommentJob: Job? = null

    init {
        loadSubscribedSchedules()
    }

    internal fun attachNavigator(nextNavigator: MiaopuNavigator) {
        if (navigator === nextNavigator) return
        navigator = nextNavigator
        restoreNavigationState()
        pendingWidgetDestination?.let { (match, sport) -> openWidgetDestination(match, sport) }
    }

    internal fun detachNavigator(detachedNavigator: MiaopuNavigator) {
        if (navigator === detachedNavigator) navigator = null
    }

    private fun restoreNavigationState() {
        val screens = navigator?.backStack?.map { it as AppScreen }.orEmpty()
        val stageScreen = screens.filterIsInstance<AppScreen.Stage>().lastOrNull()
        val ratingsMatch = screens.filterIsInstance<AppScreen.Ratings>().lastOrNull()?.match
            ?: stageScreen?.match
        if (ratingState is LoadState.Loading) {
            ratingsMatch?.toModel()?.let { loadRatings(it) }
        }
        if (stageRatingState is LoadState.Loading) {
            stageScreen?.let { loadStageRating(it.match.toModel(), it.stage.toModel()) }
        }
        if (commentState is LoadState.Loading) {
            (screens.lastOrNull() as? AppScreen.Comments)?.let { loadComments(it.target.toModel()) }
        }
    }

    fun selectEsport(esport: Esport) {
        if (esport !in subscribedEsports) return
        if (selectedEsport == esport) return
        selectedEsport = esport
        subscriptionStore.saveSelected(esport)
        if (scheduleStateFor(esport) !is LoadState.Ready) loadSchedule(esport = esport)
    }

    internal fun scheduleStateFor(esport: Esport): LoadState<Schedule> =
        scheduleStates[esport]
            ?: schedules[esport]?.let { LoadState.Ready(it) }
            ?: LoadState.Loading

    fun openSubscriptions() {
        navigator?.push(AppScreen.Subscriptions)
    }

    fun toggleSubscription(esport: Esport) {
        if (esport in subscribedEsports && subscribedEsports.size == 1) {
            message = "至少保留一个赛事订阅"
            return
        }

        val next = if (esport in subscribedEsports) subscribedEsports - esport else subscribedEsports + esport
        val removed = subscribedEsports - next
        removed.forEach { removedEsport -> scheduleJobs.remove(removedEsport)?.cancel() }
        subscribedEsports = next
        scheduleStates = scheduleStates.filterKeys { it in next }
        subscriptionStore.saveSubscriptions(next)
        viewModelScope.launch { WidgetUpdates.subscriptionsChanged(getApplication()) }

        if (selectedEsport !in next) {
            selectedEsport = next.first()
            subscriptionStore.saveSelected(selectedEsport)
        }
        loadSubscribedSchedules()
    }

    fun moveSubscription(esport: Esport, direction: Int) {
        val from = subscribedEsports.indexOf(esport)
        val to = from + direction
        if (from < 0 || to !in subscribedEsports.indices) return
        subscribedEsports = subscribedEsports.toMutableList().apply { add(to, removeAt(from)) }
        subscriptionStore.saveSubscriptions(subscribedEsports)
    }

    fun selectMainSection(section: MainSection) {
        selectedMainSection = section
        savedStateHandle[SELECTED_MAIN_SECTION_KEY] = section.name
    }

    fun retry() {
        when (val current = screen) {
            AppScreen.Schedule -> if (selectedMainSection == MainSection.HOME) {
                refreshHomeSchedules()
            } else {
                refreshSchedule()
            }
            AppScreen.Subscriptions -> Unit
            is AppScreen.Ratings -> loadRatings(current.match.toModel())
            is AppScreen.Stage -> loadStageRating(current.match.toModel(), current.stage.toModel())
            is AppScreen.Comments -> loadComments(current.target.toModel())
            is AppScreen.Web -> Unit
        }
    }

    fun refreshSchedule() {
        adapter.invalidateSharedScheduleCache()
        loadSchedule(force = true)
    }

    fun refreshHomeSchedules() {
        adapter.invalidateSharedScheduleCache()
        loadSubscribedSchedules(force = true)
    }

    fun checkForUpdates() {
        if (updateCheckState == UpdateCheckState.Checking) return
        updateCheckState = UpdateCheckState.Checking
        viewModelScope.launch {
            val result = releaseAdapter.latest()
            val release = result.data
            updateCheckState = when {
                release != null && isNewerVersion(release.tagName, currentVersion) ->
                    UpdateCheckState.Available(release)
                release != null -> UpdateCheckState.UpToDate(release.tagName)
                else -> UpdateCheckState.Failed(result.error?.message ?: "检查更新失败")
            }
        }
    }

    internal fun scheduleViewport(esport: Esport): ScheduleViewportSnapshot? = scheduleViewports[esport]

    internal fun saveScheduleViewport(esport: Esport, snapshot: ScheduleViewportSnapshot) {
        scheduleViewports[esport] = snapshot
    }

    internal fun stageViewport(match: MatchSummary, stage: RatingStage): StageViewportSnapshot? =
        stageViewports[stageViewportKey(match, stage)]

    internal fun saveStageViewport(
        match: MatchSummary,
        stage: RatingStage,
        snapshot: StageViewportSnapshot,
    ) {
        stageViewports[stageViewportKey(match, stage)] = snapshot
    }

    fun openMatch(match: MatchSummary) {
        val ratings = AppScreen.Ratings(match.toRoute())
        navigator?.push(ratings)
        loadRatings(match)
    }

    internal fun openWidgetDestination(match: MatchSummary?, sport: Esport?) {
        val currentNavigator = navigator
        if (currentNavigator == null) {
            pendingWidgetDestination = match to sport
            return
        }
        pendingWidgetDestination = null
        while (currentNavigator.backStack.size > 1) goBack()
        sport?.let(::selectEsport)
        selectMainSection(MainSection.EVENTS)
        if (match != null) openMatch(match)
    }

    fun openStage(match: MatchSummary, stage: RatingStage, stageNumber: Int) {
        stageViewports.remove(stageViewportKey(match, stage))
        navigator?.push(
            AppScreen.Stage(
                match = match.toRoute(),
                stage = stage.toRoute(),
                stageNumber = stageNumber,
                returnToStagePicker = true,
            ),
        )
        loadStageRating(match, stage)
    }

    fun openComments(target: RatingTarget) {
        publishCommentJob?.cancel()
        publishCommentGate.invalidate()
        isPublishingComment = false
        saveCommentDraft("")
        navigator?.push(AppScreen.Comments(target.toRoute()))
        loadComments(target)
    }

    fun loadMoreComments(target: RatingTarget) {
        if (isLoadingMoreComments) return
        val current = (commentState as? LoadState.Ready)?.value ?: return
        val cursor = current.nextPublishTime ?: return
        val targetKey = target.key
        if ((screen as? AppScreen.Comments)?.target?.key != targetKey) return
        moreCommentsJob?.cancel()
        commentPaginationError = null
        isLoadingMoreComments = true
        val token = commentPaginationGate.begin(targetKey)
        moreCommentsJob = viewModelScope.launch {
            try {
                val result = adapter.getComments(target, cursor)
                if (!commentPaginationGate.isCurrent(token)) return@launch
                if ((screen as? AppScreen.Comments)?.target?.key != targetKey) return@launch
                if (result.status == AdapterStatus.SUCCESS && result.data != null) {
                    val next = result.data
                    val merged = withContext(Dispatchers.Default) {
                        mergeCommentsByHeat(current.comments, next.comments)
                    }
                    commentState = LoadState.Ready(
                        next.copy(
                            comments = merged,
                            totalCount = maxOf(current.totalCount, next.totalCount),
                            hottestComments = current.hottestComments,
                        ),
                    )
                } else {
                    commentPaginationError = result.error?.message ?: "加载更多评论失败"
                }
            } finally {
                if (commentPaginationGate.complete(token)) {
                    isLoadingMoreComments = false
                    moreCommentsJob = null
                }
            }
        }
    }

    fun updateCommentDraft(value: String) {
        saveCommentDraft(value.take(500))
    }

    fun publishComment(target: RatingTarget, score: Int? = null) {
        if (isPublishingComment) return
        val content = commentDraft.trim()
        if (content.isEmpty() && score == null) {
            message = "请输入评论或选择新的评分"
            return
        }
        if (!isLoggedIn) {
            message = "请先在“我的”中登录虎扑"
            return
        }
        lastCommentSubmissionSucceeded = false
        isPublishingComment = true
        val targetKey = target.key
        val token = publishCommentGate.begin(targetKey)
        publishCommentJob = viewModelScope.launch {
            try {
                if (content.isEmpty()) {
                    val scoreToSubmit = requireNotNull(score)
                    val scoreResult = adapter.submitScore(target, scoreToSubmit)
                    if (!publishCommentGate.isCurrent(token)) return@launch
                    if ((screen as? AppScreen.Comments)?.target?.key != targetKey) return@launch
                    if (scoreResult.status == AdapterStatus.SUCCESS) {
                        updateUserScore(target, scoreToSubmit)
                        lastCommentSubmissionSucceeded = true
                        message = "评分提交成功"
                    } else {
                        if (scoreResult.status == AdapterStatus.AUTH_REQUIRED) isLoggedIn = false
                        message = scoreResult.error?.message ?: "评分提交失败"
                    }
                    return@launch
                }
                val result = adapter.publishComment(target, content)
                if (!publishCommentGate.isCurrent(token)) return@launch
                if ((screen as? AppScreen.Comments)?.target?.key != targetKey) return@launch
                if (result.status == AdapterStatus.SUCCESS) {
                    val scoreResult = score?.let { adapter.submitScore(target, it) }
                    if (!publishCommentGate.isCurrent(token)) return@launch
                    if ((screen as? AppScreen.Comments)?.target?.key != targetKey) return@launch
                    if (scoreResult?.status == AdapterStatus.SUCCESS) {
                        score?.let { updateUserScore(target, it) }
                    }
                    saveCommentDraft("")
                    lastCommentSubmissionSucceeded = true
                    message = if (scoreResult != null && scoreResult.status != AdapterStatus.SUCCESS) {
                        if (scoreResult.status == AdapterStatus.AUTH_REQUIRED) isLoggedIn = false
                        "评论已发布，但${scoreResult.error?.message ?: "评分提交失败"}"
                    } else {
                        "评论发布成功"
                    }
                    loadComments(target)
                } else {
                    if (result.status == AdapterStatus.AUTH_REQUIRED) isLoggedIn = false
                    message = result.error?.message ?: "评论发布失败"
                }
            } finally {
                if (publishCommentGate.complete(token)) {
                    isPublishingComment = false
                    publishCommentJob = null
                }
            }
        }
    }

    fun latestRatingTarget(target: RatingTarget): RatingTarget {
        matchDetail.latestTarget(target)?.let { return it }
        val detail = (ratingState as? LoadState.Ready)?.value ?: return target
        return detail.stages
            .asSequence()
            .flatMap { it.targets.asSequence() }
            .firstOrNull { it.key == target.key }
            ?: target
    }

    fun openLogin() {
        navigator?.push(
            AppScreen.Web(
                title = "登录虎扑",
                url = HupuUrls.loginUrl(),
                login = true,
            ),
        )
    }

    fun finishLogin(): Boolean {
        val authenticated = cookieSession.capture()
        isLoggedIn = authenticated
        if (!authenticated) {
            message = "尚未检测到有效的虎扑登录 Cookie"
            return false
        }
        message = "虎扑登录成功"
        navigator?.pop()
        return true
    }

    fun logout() {
        cookieSession.clear {
            isLoggedIn = false
            message = "已清除本机虎扑登录信息"
        }
    }

    fun goBack() {
        val current = screen
        if (current is AppScreen.Comments) {
            publishCommentJob?.cancel()
            publishCommentJob = null
            publishCommentGate.invalidate()
            commentJob?.cancel()
            moreCommentsJob?.cancel()
            moreCommentsJob = null
            commentPaginationGate.invalidate()
            commentReplies.clear()
            isLoadingMoreComments = false
            commentPaginationError = null
            isPublishingComment = false
            saveCommentDraft("")
        }
        if (current is AppScreen.Ratings) {
            ratingJob?.cancel()
            matchDetail.close()
        }
        if (current is AppScreen.Stage) stageRatingJob?.cancel()
        navigator?.pop()
    }

    fun dismissMessage() {
        message = null
    }

    private fun loadSubscribedSchedules(force: Boolean = false) {
        subscribedEsports.forEach { esport -> loadSchedule(esport = esport, force = force) }
    }

    private fun loadSchedule(esport: Esport = selectedEsport, force: Boolean = false) {
        if (!force) schedules[esport]?.let {
            updateScheduleState(esport, LoadState.Ready(it))
            return
        }
        if (!force && scheduleJobs[esport]?.isActive == true) return
        scheduleJobs.remove(esport)?.cancel()
        updateScheduleState(esport, LoadState.Loading)
        val scheduleJob = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val startedAt = System.currentTimeMillis()
            val nextState = adapter.getSchedule(esport).toLoadState()
            if (scheduleJobs[esport] !== coroutineContext[Job]) return@launch
            (nextState as? LoadState.Ready)?.value?.let { schedules[esport] = it }
            updateScheduleState(esport, nextState)
            scheduleJobs.remove(esport)
            WidgetUpdates.publish(getApplication(), esport, (nextState as? LoadState.Ready)?.value, startedAt)
        }
        scheduleJobs[esport] = scheduleJob
        scheduleJob.start()
    }

    private fun updateScheduleState(esport: Esport, state: LoadState<Schedule>) {
        scheduleStates = scheduleStates + (esport to state)
    }

    private fun loadRatings(match: MatchSummary) {
        matchDetail.bind(match)
        ratingJob?.cancel()
        ratingState = LoadState.Loading
        ratingJob = viewModelScope.launch {
            val nextState = adapter.getRatings(match).toLoadState()
            if (navigator?.containsMatch(match.id) == true) ratingState = nextState
        }
    }

    private fun loadStageRating(match: MatchSummary, stage: RatingStage) {
        stageRatingJob?.cancel()
        stageRatingState = LoadState.Loading
        val key = "${stage.outBizType ?: match.outBizType}:${stage.outBizNo ?: match.outBizNo}"
        val expectedScreen = AppScreen.Stage(
            match = match.toRoute(),
            stage = stage.toRoute(),
            stageNumber = 0,
            returnToStagePicker = false,
        )
        stageRatingJob = viewModelScope.launch {
            val nextState = adapter.getStageRatingDetail(match, stage).toLoadState()
            val currentKey = (screen as? AppScreen.Stage)?.let {
                "${it.stage.outBizType ?: it.match.outBizType}:${it.stage.outBizNo ?: it.match.outBizNo}"
            }
            if (currentKey == key || navigator?.contains(expectedScreen) == true) {
                stageRatingState = nextState
            }
        }
    }

    private fun loadComments(target: RatingTarget) {
        commentJob?.cancel()
        moreCommentsJob?.cancel()
        moreCommentsJob = null
        commentPaginationGate.invalidate()
        commentReplies.clear()
        isLoadingMoreComments = false
        commentPaginationError = null
        commentState = LoadState.Loading
        val targetKey = target.key
        commentJob = viewModelScope.launch {
            val hottestDeferred = async { adapter.getHottestComments(target) }
            val pageResult = adapter.getComments(target)
            val hottestResult = hottestDeferred.await()
            if ((screen as? AppScreen.Comments)?.target?.key != targetKey) return@launch

            val page = pageResult.data
            commentState = if (page != null) {
                LoadState.Ready(
                    page.copy(
                        hottestComments = hottestResult.data.orEmpty(),
                    ),
                )
            } else {
                pageResult.toLoadState()
            }
        }
    }

    private fun updateUserScore(target: RatingTarget, score: Int) {
        fun update(candidate: RatingTarget): RatingTarget =
            if (candidate.outBizType == target.outBizType && candidate.outBizNo == target.outBizNo) {
                candidate.copy(userScore = score)
            } else {
                candidate
            }

        matchDetail.updateTargets(::update)
        (ratingState as? LoadState.Ready)?.value?.let { detail ->
            ratingState = LoadState.Ready(
                detail.copy(
                    stages = detail.stages.map { stage ->
                        stage.copy(targets = stage.targets.map(::update))
                    },
                ),
            )
        }
        (stageRatingState as? LoadState.Ready)?.value?.let { stageDetail ->
            stageRatingState = LoadState.Ready(
                stageDetail.copy(
                    targets = stageDetail.targets.map(::update),
                    groups = stageDetail.groups.map { group -> group.copy(targets = group.targets.map(::update)) },
                ),
            )
        }
    }

    private fun saveCommentDraft(value: String) {
        commentDraft = value
        savedStateHandle[COMMENT_DRAFT_KEY] = value
    }

    private fun <T> AdapterResult<T>.toLoadState(): LoadState<T> = data?.let(LoadState<T>::Ready)
        ?: LoadState.Failed(
            message = error?.message ?: "未知错误",
            retryable = error?.retryable == true,
        )

    private val RatingTarget.key: String
        get() = "$outBizType:$outBizNo"

    private fun stageViewportKey(match: MatchSummary, stage: RatingStage): String =
        "${match.id}:${stage.outBizType ?: match.outBizType}:${stage.outBizNo ?: match.outBizNo}"

    private companion object {
        const val SELECTED_MAIN_SECTION_KEY = "selected_main_section"
        const val COMMENT_DRAFT_KEY = "comment_draft"
    }
}
