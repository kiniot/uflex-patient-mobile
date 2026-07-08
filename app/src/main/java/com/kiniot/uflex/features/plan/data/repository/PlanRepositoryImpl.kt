package com.kiniot.uflex.features.plan.data.repository

import com.kiniot.uflex.core.result.AppError
import com.kiniot.uflex.core.result.AppResult
import com.kiniot.uflex.features.plan.data.mapper.toDomain
import com.kiniot.uflex.features.plan.data.remote.datasource.ExerciseRemoteDataSource
import com.kiniot.uflex.features.plan.data.remote.datasource.PlanRemoteDataSource
import com.kiniot.uflex.features.plan.domain.model.PlansOverview
import com.kiniot.uflex.features.plan.domain.repository.PlanRepository
import javax.inject.Inject

class PlanRepositoryImpl @Inject constructor(
    private val planRemoteDataSource: PlanRemoteDataSource,
    private val exerciseRemoteDataSource: ExerciseRemoteDataSource
) : PlanRepository {

    override suspend fun getPlansOverview(): AppResult<PlansOverview> {
        // Catalog is fetched once and used to enrich every series with its exercise identity.
        val catalog = when (val result = exerciseRemoteDataSource.getExercises()) {
            is AppResult.Success -> result.data.associateBy { it.id }
            is AppResult.Error -> return result
        }

        // No active plan (404) is a valid empty state, not an error.
        val active = when (val result = planRemoteDataSource.getActivePlan()) {
            is AppResult.Success -> result.data.toDomain(catalog)
            is AppResult.Error -> if (result.error is AppError.NotFound) null else return result
        }

        val scheduled = when (val result = planRemoteDataSource.getScheduledPlans()) {
            is AppResult.Success -> result.data.map { it.toDomain(catalog) }
            is AppResult.Error -> return result
        }

        return AppResult.Success(PlansOverview(active = active, scheduled = scheduled))
    }
}
