package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.BadgeEntity
import com.example.data.model.UserGamificationEntity
import com.example.data.repository.GamificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BadgeFilter {
    ALL, UNLOCKED, IN_PROGRESS
}

data class AchievementsUiState(
    val gamification: UserGamificationEntity = UserGamificationEntity(),
    val badges: List<BadgeEntity> = emptyList(),
    val filteredBadges: List<BadgeEntity> = emptyList(),
    val currentFilter: BadgeFilter = BadgeFilter.ALL,
    val currentLevelMinXp: Int = 0,
    val nextLevelMinXp: Int = 200,
    val progressWithinLevel: Float = 0f,
    val xpNeededForNextLevel: Int = 200,
    val unlockedCount: Int = 0,
    val totalBadgesCount: Int = 0,
    val selectedBadge: BadgeEntity? = null
)

class AchievementsViewModel(
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(BadgeFilter.ALL)
    private val _selectedBadge = MutableStateFlow<BadgeEntity?>(null)

    val uiState: StateFlow<AchievementsUiState> = combine(
        gamificationRepository.userGamification,
        gamificationRepository.allBadges,
        _selectedFilter,
        _selectedBadge
    ) { gamification, badges, filter, selectedBadge ->
        val (minXp, nextXp) = GamificationRepository.getXpThresholdsForLevel(gamification.currentLevel)
        val xpInLevel = (gamification.totalXp - minXp).coerceAtLeast(0)
        val levelSpan = (nextXp - minXp).coerceAtLeast(1)
        val progress = (xpInLevel.toFloat() / levelSpan.toFloat()).coerceIn(0f, 1f)
        val xpNeeded = (nextXp - gamification.totalXp).coerceAtLeast(0)

        val unlocked = badges.count { it.isUnlocked }

        val filtered = when (filter) {
            BadgeFilter.ALL -> badges
            BadgeFilter.UNLOCKED -> badges.filter { it.isUnlocked }
            BadgeFilter.IN_PROGRESS -> badges.filter { !it.isUnlocked }
        }

        AchievementsUiState(
            gamification = gamification,
            badges = badges,
            filteredBadges = filtered,
            currentFilter = filter,
            currentLevelMinXp = minXp,
            nextLevelMinXp = nextXp,
            progressWithinLevel = progress,
            xpNeededForNextLevel = xpNeeded,
            unlockedCount = unlocked,
            totalBadgesCount = badges.size,
            selectedBadge = selectedBadge
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AchievementsUiState()
    )

    init {
        refreshAchievements()
    }

    fun setFilter(filter: BadgeFilter) {
        _selectedFilter.value = filter
    }

    fun selectBadge(badge: BadgeEntity?) {
        _selectedBadge.value = badge
    }

    fun refreshAchievements() {
        viewModelScope.launch {
            gamificationRepository.evaluateGamificationState()
        }
    }

    companion object {
        fun provideFactory(
            gamificationRepository: GamificationRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AchievementsViewModel(gamificationRepository) as T
            }
        }
    }
}
