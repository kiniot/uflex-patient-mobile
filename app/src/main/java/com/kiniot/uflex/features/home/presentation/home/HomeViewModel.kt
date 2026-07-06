package com.kiniot.uflex.features.home.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.core.result.toUserMessage
import com.kiniot.uflex.features.plan.domain.model.TreatmentPlan
import com.kiniot.uflex.features.plan.domain.usecase.GetMyPlansOverviewUseCase
import com.kiniot.uflex.features.profile.domain.usecase.GetMyPatientProfileUseCase
import com.kiniot.uflex.features.therapy.domain.model.ScheduleResolution
import com.kiniot.uflex.features.therapy.domain.usecase.GetDailyScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMyPatientProfileUseCase: GetMyPatientProfileUseCase,
    private val getMyPlansOverviewUseCase: GetMyPlansOverviewUseCase,
    private val getDailyScheduleUseCase: GetDailyScheduleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Fan out the three reads concurrently; the shell reloads Home on each entry.
            val profileDef = async { getMyPatientProfileUseCase() }
            val plansDef = async { getMyPlansOverviewUseCase() }
            val scheduleDef = async { getDailyScheduleUseCase() }

            val profile = profileDef.await()
            val plans = plansDef.await()
            val schedule = scheduleDef.await()

            // The plan is the primary content — only its failure blanks the screen (with retry).
            // Profile / schedule failures degrade gracefully (name-less greeting, unknown routine).
            if (plans is AppResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = plans.error.toUserMessage()) }
                return@launch
            }
            val activePlan = (plans as AppResult.Success).data.active

            val firstName = (profile as? AppResult.Success)?.data?.firstName?.takeIf { it.isNotBlank() }

            val dailySchedule = (schedule as? AppResult.Success)?.data
            val resolution = when {
                activePlan == null -> ScheduleResolution.NoActivePlan
                dailySchedule != null -> dailySchedule.resolution
                else -> ScheduleResolution.Unknown
            }
            val todaysExercises = activePlan?.routines
                ?.firstOrNull { it.id == dailySchedule?.routineId }
                ?.exercises
                .orEmpty()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    firstName = firstName,
                    greeting = greetingForHour(LocalTime.now().hour),
                    activePlan = activePlan,
                    planProgress = activePlan?.let(::computeProgress),
                    todayResolution = resolution,
                    todaysExercises = todaysExercises,
                    todayTotalSeries = dailySchedule?.totalSeries,
                    todayEstMinutes = dailySchedule?.estimatedDurationMinutes,
                    errorMessage = null
                )
            }
        }
    }
}

private fun greetingForHour(hour: Int): Greeting = when (hour) {
    in 5..11 -> Greeting.Morning
    in 12..18 -> Greeting.Afternoon
    else -> Greeting.Evening
}

/**
 * Elapsed fraction of the plan's period. Needs both dates with `endsAt > startsAt`; otherwise null
 * (the UI shows a date-less fallback). Day counting is inclusive so 100% aligns with the last day.
 */
private fun computeProgress(plan: TreatmentPlan): PlanProgress? {
    val start = plan.period?.startsAt ?: return null
    val end = plan.period?.endsAt ?: return null
    if (!end.isAfter(start)) return null

    val today = LocalDate.now()
    val span = ChronoUnit.DAYS.between(start, end).toFloat()
    val elapsed = ChronoUnit.DAYS.between(start, today).toFloat()
    val fraction = (elapsed / span).coerceIn(0f, 1f)
    val totalDays = span.toInt() + 1
    val dayIndex = (elapsed.toInt() + 1).coerceIn(1, totalDays)
    return PlanProgress(
        fraction = fraction,
        percent = (fraction * 100).roundToInt(),
        dayIndex = dayIndex,
        totalDays = totalDays
    )
}
