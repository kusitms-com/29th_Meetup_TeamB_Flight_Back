package com.bamyanggang.apimodule.domain.experience.application.service

import com.bamyanggang.apimodule.common.getAuthenticationPrincipal
import com.bamyanggang.apimodule.domain.experience.application.dto.ExperienceYear
import com.bamyanggang.apimodule.domain.experience.application.dto.GetExperience
import com.bamyanggang.domainmodule.domain.bookmark.enums.BookmarkStatus
import com.bamyanggang.domainmodule.domain.bookmark.service.BookmarkReader
import com.bamyanggang.domainmodule.domain.experience.aggregate.Experience
import com.bamyanggang.domainmodule.domain.experience.aggregate.ExperienceContent
import com.bamyanggang.domainmodule.domain.experience.aggregate.ExperienceStrongPoint
import com.bamyanggang.domainmodule.domain.experience.service.ExperienceReader
import com.bamyanggang.domainmodule.domain.strongpoint.service.StrongPointReader
import com.bamyanggang.domainmodule.domain.tag.service.TagReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class ExperienceGetService(
    private val experienceReader: ExperienceReader,
    private val strongPointReader: StrongPointReader,
    private val tagReader: TagReader,
    private val bookMarkReader: BookmarkReader,
) {
    @Transactional(readOnly = true)
    fun getExperienceDetailById(experienceId: UUID) : GetExperience.DetailExperience {
        val oneExperience = experienceReader.readExperience(experienceId)
        return createExperienceDetailResponse(oneExperience)
    }

    @Transactional(readOnly = true)
    fun getAllYearsByExistExperience(): ExperienceYear.Response {
        val currentUserId = getAuthenticationPrincipal()

        val years = experienceReader.readAllYearsByExistExperience(currentUserId)
        val yearTagInfos = years.map { year ->
            val parentTagIds = experienceReader.readByUserIDAndYearDesc(year, currentUserId)
                .distinctBy { it.parentTagId }
                .map { it.parentTagId }

            val tagDetails = tagReader.readByIds(parentTagIds).map {
                ExperienceYear.TagDetail(
                    id = it.id,
                    name = it.name
                )
            }

            ExperienceYear.YearTagInfo(
                year,
                tagDetails
            )
        }

        return ExperienceYear.Response(
            years,
            yearTagInfos
        )
    }

    @Transactional(readOnly = true)
    fun getExperienceByYearAndParentTag(year: Int, parentTagId: UUID): GetExperience.Response {
        val experiences = experienceReader.readByYearAndParentTagId(year, parentTagId).map {
            createExperienceDetailResponse(it)
        }

        return GetExperience.Response(experiences)
    }

    @Transactional(readOnly = true)
    fun getExperienceByYearAndChildTag(year: Int, childTagId: UUID): GetExperience.Response {
        val experiences = experienceReader.readByChildTagIdAndYear(year, childTagId).map {
            createExperienceDetailResponse(it)
        }

        return GetExperience.Response(experiences)
    }

    @Transactional(readOnly = true)
    fun getAllBookmarkExperiences(jobDescriptionId: UUID): GetExperience.BookmarkResponse {
        val experienceIds = bookMarkReader.readByStatusAndJobDescriptionId(jobDescriptionId, BookmarkStatus.ON).map { it.experienceId }

        val userExperiences = experienceReader.readAllByUserId(getAuthenticationPrincipal())

        val bookmarkExperienceDetails = userExperiences.map {
            when {
                it.id in experienceIds -> createBookmarkExperienceDetailResponse(it, BookmarkStatus.ON)
                else -> createBookmarkExperienceDetailResponse(it, BookmarkStatus.OFF)
            }
        }

        return GetExperience.BookmarkResponse(bookmarkExperienceDetails)
    }

    @Transactional(readOnly = true)
    fun getBookmarkExperienceBySearch(jobDescriptionId: UUID, search: String): GetExperience.BookmarkResponse {
        val currentUserId = getAuthenticationPrincipal()

        val experiencesIds = experienceReader.readByTitleContains(search) +
                experienceReader.readByContentsContains(currentUserId, search) +
                tagReader.readIdsByNameContains(search) +
                strongPointReader.readIdsByNameContains(search)

        val searchExperiences = experienceReader.readByIds(experiencesIds)
        val bookmarkExperienceIds = bookMarkReader.readByExperienceIds(experiencesIds).map { it.experienceId }

        val bookmarkExperienceDetails = searchExperiences.map {
            when {
                it.id in bookmarkExperienceIds -> createBookmarkExperienceDetailResponse(it, BookmarkStatus.ON)
                else -> createBookmarkExperienceDetailResponse(it, BookmarkStatus.OFF)
            }
        }

        return GetExperience.BookmarkResponse(bookmarkExperienceDetails)
    }

    private fun createExperienceDetailResponse(experience: Experience): GetExperience.DetailExperience {
        val detailExperienceContents = convertExperienceContent(experience.contents)
        val strongPointDetails = convertStrongPoints(experience.strongPoints)
        val detailParentTag = convertParentTag(experience.parentTagId)
        val detailChildTag = convertChildTag(experience.childTagId)

        return GetExperience.DetailExperience(
            id = experience.id,
            title = experience.title,
            parentTag = detailParentTag,
            childTag = detailChildTag,
            strongPoints = strongPointDetails,
            contents = detailExperienceContents,
            startedAt = experience.startedAt,
            endedAt = experience.endedAt
        )
    }

    private fun createBookmarkExperienceDetailResponse(experience: Experience, bookmarkStatus: BookmarkStatus): GetExperience.BookmarkDetailExperience {
        val detailExperienceContents = convertExperienceContent(experience.contents)
        val strongPointDetails = convertStrongPoints(experience.strongPoints)
        val detailParentTag = convertParentTag(experience.parentTagId)
        val detailChildTag = convertChildTag(experience.childTagId)

        return GetExperience.BookmarkDetailExperience(
            id = experience.id,
            title = experience.title,
            parentTag = detailParentTag,
            childTag = detailChildTag,
            strongPoints = strongPointDetails,
            contents = detailExperienceContents,
            startedAt = experience.startedAt,
            endedAt = experience.endedAt,
            bookmarked = bookmarkStatus
        )
    }

    private fun convertChildTag(childTagId: UUID) =
        tagReader.readById(childTagId).let {
            GetExperience.DetailTag(
                it.id,
                it.name
            )
        }

    private fun convertParentTag(parentTagId: UUID) =
        tagReader.readById(parentTagId).let {
            GetExperience.DetailTag(
                it.id,
                it.name
            )
        }

    private fun convertExperienceContent(contents: List<ExperienceContent>) =
        contents.map { GetExperience.DetailExperienceContent(
                it.question,
                it.answer
            )
        }

    private fun convertStrongPoints(strongPoints: List<ExperienceStrongPoint>) =
        strongPoints.map { it.strongPointId }.let {
            strongPointReader.readByIds(it).map { strongPoint ->
                GetExperience.DetailStrongPoint(
                    strongPoint.id,
                    strongPoint.name
                )
            }
    }

    fun getAllExperienceByYear(year: Int): GetExperience.Response {
        val experiences = experienceReader.readByYear(year).map {
            createExperienceDetailResponse(it)
        }

        return GetExperience.Response(experiences)
    }
}   
