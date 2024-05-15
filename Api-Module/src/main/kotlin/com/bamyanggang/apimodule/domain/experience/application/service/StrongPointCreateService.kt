package com.bamyanggang.apimodule.domain.experience.application.service

import com.bamyanggang.apimodule.common.getAuthenticationPrincipal
import com.bamyanggang.apimodule.domain.experience.application.dto.CreateStrongPoint
import com.bamyanggang.apimodule.domain.experience.application.exception.StrongPointException
import com.bamyanggang.domainmodule.domain.strongpoint.aggregate.StrongPoint
import com.bamyanggang.domainmodule.domain.strongpoint.service.StrongPointAppender
import com.bamyanggang.domainmodule.domain.strongpoint.service.StrongPointReader
import org.springframework.stereotype.Service

@Service
class StrongPointCreateService(
    val strongPointAppender: StrongPointAppender,
    val strongPointReader: StrongPointReader,
) {
    fun createStrongPoint(request: CreateStrongPoint.Request): CreateStrongPoint.Response {
        val accessUserId = getAuthenticationPrincipal()

        val userStrongPoints = strongPointReader.findAllByUserId(accessUserId)
        validateDuplicatedName(userStrongPoints, request.name)

        val strongPointId = strongPointAppender.appendStrongPoint(request.name, accessUserId)

        return CreateStrongPoint.Response(strongPointId)
    }

    private fun validateDuplicatedName(userStrongPoints: List<StrongPoint>, name: String) {
        userStrongPoints.forEach {
            if (name == it.name) {
                throw StrongPointException.DuplicatedStrongPointName()
            }
        }
    }
}