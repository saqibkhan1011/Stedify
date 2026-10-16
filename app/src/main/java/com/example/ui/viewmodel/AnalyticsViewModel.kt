package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.DailyLogEntity
import com.example.data.repository.StedifyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ChartTimeframe(val days: Int, val label: String) {
    SEVEN_DAYS(7, "7 Days"),
    THIRTY_DAYS(30, "30 Days")
}

data class DayCompletionStat(
    val dateString: String,
    val dayLabel: String,
    val completed: Int,
    val total: Int,
    val completionRate: Float // 0f to 1f
)

data class CalendarDayModel(
    val dayOfMonth: Int,
    val dateString: String,
    val completed: Int,
    val total: Int,
    val completionPercentage: Float,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

data class AnalyticsUiState(
    val timeframe: ChartTimeframe = ChartTimeframe.SEVEN_DAYS,
    val chartData: List<DayCompletionStat> = emptyList(),
    val currentMonthDisplay: String = "",
    val calendarDays: List<CalendarDayModel> = emptyList(),
    val currentStreak: Int = 0,
    val rollingConsistencyScore: Float = 100f,
    val totalCompletedAllTime: Int = 0,
    val totalTasksAllTime: Int = 0,
    val selectedDayDetails: CalendarDayModel? = null
)

class AnalyticsViewModel(
    private val repository: StedifyRepository
) : ViewModel() {

    private val _timeframe = MutableStateFlow(ChartTimeframe.SEVEN_DAYS)
    val timeframe: StateFlow<ChartTimeframe> = _timeframe.asStateFlow()

    private val _currentCalendarMonth = MutableStateFlow(Calendar.getInstance())
    private val _selectedDayDetails = MutableStateFlow<CalendarDayModel?>(null)

    private val allLogsFlow = repository.getAllDailyLogs()
    private val streakFlow = repository.observeStreakCount()
    private val consistencyFlow = repository.observe14DayConsistencyScore()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val shortDayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    // Combine repository-driven analytics data (3 flows)
    private val analyticsCoreDataFlow = combine(allLogsFlow, streakFlow, consistencyFlow) { logs, streak, consistency ->
        Triple(logs, streak, consistency)
    }

    // Combine UI control state (3 flows)
    private val uiControlStateFlow = combine(_timeframe, _currentCalendarMonth, _selectedDayDetails) { tf, month, selectedDay ->
        Triple(tf, month, selectedDay)
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        uiControlStateFlow,
        analyticsCoreDataFlow
    ) { (tf, calMonth, selectedDay), (logs, streak, consistency) ->
        val logMap = logs.associateBy { it.dateString }

        // 1. Compute Chart Data
        val chartStats = mutableListOf<DayCompletionStat>()
        val chartCal = Calendar.getInstance()
        chartCal.add(Calendar.DAY_OF_YEAR, -(tf.days - 1))

        for (i in 0 until tf.days) {
            val dateStr = dateFormat.format(chartCal.time)
            val dayLabel = if (tf == ChartTimeframe.SEVEN_DAYS) {
                shortDayFormat.format(chartCal.time)
            } else {
                "${chartCal.get(Calendar.DAY_OF_MONTH)}"
            }

            val log = logMap[dateStr]
            val completed = log?.tasksCompleted ?: 0
            val total = log?.tasksTotal ?: 0
            val rate = if (total > 0) (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

            chartStats.add(
                DayCompletionStat(
                    dateString = dateStr,
                    dayLabel = dayLabel,
                    completed = completed,
                    total = total,
                    completionRate = rate
                )
            )
            chartCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 2. Compute Calendar Matrix for calMonth
        val monthDisplay = monthYearFormat.format(calMonth.time)
        val calendarDays = computeCalendarGrid(calMonth, logMap)

        // 3. All time stats
        val totalCompleted = logs.sumOf { it.tasksCompleted }
        val totalAll = logs.sumOf { it.tasksTotal }

        AnalyticsUiState(
            timeframe = tf,
            chartData = chartStats,
            currentMonthDisplay = monthDisplay,
            calendarDays = calendarDays,
            currentStreak = streak,
            rollingConsistencyScore = consistency,
            totalCompletedAllTime = totalCompleted,
            totalTasksAllTime = totalAll,
            selectedDayDetails = selectedDay
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun setTimeframe(tf: ChartTimeframe) {
        _timeframe.value = tf
    }

    fun changeMonth(offset: Int) {
        val newCal = Calendar.getInstance().apply {
            time = _currentCalendarMonth.value.time
            add(Calendar.MONTH, offset)
        }
        _currentCalendarMonth.value = newCal
    }

    fun selectDay(day: CalendarDayModel) {
        _selectedDayDetails.value = if (_selectedDayDetails.value?.dateString == day.dateString) null else day
    }

    private fun computeCalendarGrid(
        monthCal: Calendar,
        logMap: Map<String, DailyLogEntity>
    ): List<CalendarDayModel> {
        val days = mutableListOf<CalendarDayModel>()
        val cal = Calendar.getInstance().apply {
            time = monthCal.time
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val targetMonth = cal.get(Calendar.MONTH)
        val todayStr = dateFormat.format(Date())

        var firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        if (firstDayOfWeek <= 0) firstDayOfWeek = 7
        val prefixDays = firstDayOfWeek - 1

        cal.add(Calendar.DAY_OF_MONTH, -prefixDays)

        for (i in 0 until 35) {
            val dateStr = dateFormat.format(cal.time)
            val isCurrentMonth = cal.get(Calendar.MONTH) == targetMonth
            val isToday = dateStr == todayStr

            val log = logMap[dateStr]
            val completed = log?.tasksCompleted ?: 0
            val total = log?.tasksTotal ?: 0
            val score = log?.consistencyScore ?: 0f

            days.add(
                CalendarDayModel(
                    dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                    dateString = dateStr,
                    completed = completed,
                    total = total,
                    completionPercentage = score,
                    isCurrentMonth = isCurrentMonth,
                    isToday = isToday
                )
            )

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return days
    }

    companion object {
        fun provideFactory(repository: StedifyRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AnalyticsViewModel(repository) as T
                }
            }
    }
}
