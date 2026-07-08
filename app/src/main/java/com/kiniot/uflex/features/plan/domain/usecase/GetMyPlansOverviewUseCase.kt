package com.kiniot.uflex.features.plan.domain.usecase

import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.plan.domain.model.PlansOverview
import com.kiniot.uflex.features.plan.domain.repository.PlanRepository
import javax.inject.Inject

class GetMyPlansOverviewUseCase @Inject constructor(
    private val repository: PlanRepository
) {
    suspend operator fun invoke(): AppResult<PlansOverview> = repository.getPlansOverview()
}
