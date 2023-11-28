/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2023 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.opdata.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.constant.CUSTOM
import com.tencent.bkrepo.common.artifact.constant.PIPELINE
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.opdata.constant.TO_GIGABYTE
import com.tencent.bkrepo.opdata.model.StatDateModel
import com.tencent.bkrepo.opdata.model.TProjectMetrics
import com.tencent.bkrepo.opdata.pojo.ProjectMetrics
import com.tencent.bkrepo.opdata.pojo.ProjectMetricsOption
import com.tencent.bkrepo.opdata.pojo.ProjectMetricsRequest
import com.tencent.bkrepo.opdata.pojo.enums.ProjectType
import com.tencent.bkrepo.opdata.repository.ProjectMetricsRepository
import com.tencent.bkrepo.opdata.util.EasyExcelUtils
import com.tencent.bkrepo.opdata.util.MetricsCacheUtil
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Service
class ProjectMetricsService (
    private val projectMetricsRepository: ProjectMetricsRepository,
    private val statDateModel: StatDateModel
    ){

    fun page(option: ProjectMetricsOption): Page<TProjectMetrics> {
        with(option) {
            val pageRequest = Pages.ofRequest(pageNumber, pageSize)
            val queryResult = if (!projectId.isNullOrEmpty()) {
                projectMetricsRepository.findByProjectIdAndCreatedDateOrderByCreatedDateDesc(
                    projectId!!, createdDate, pageRequest
                )
            } else {
                projectMetricsRepository.findByCreatedDateOrderByCreatedDateDesc(createdDate, pageRequest)
            }
            queryResult.content.forEach {
                it.capSize = it.capSize / TO_GIGABYTE
                it.repoMetrics.forEach {  repo ->
                    repo.size = repo.size / TO_GIGABYTE
                }
            }
            return Pages.ofResponse(pageRequest, queryResult.totalElements, queryResult.content)
        }
    }

    fun list(metricsRequest: ProjectMetricsRequest): List<ProjectMetrics> {
       return getProjectMetrics(metricsRequest)
    }

    fun download(metricsRequest: ProjectMetricsRequest) {
        metricsRequest.showDelta = true
        val records = getProjectMetrics(metricsRequest)
        // 导出
        val includeColumns = mutableSetOf(
            ProjectMetrics::projectId.name,
            ProjectMetrics::capSize.name,
            ProjectMetrics::capSizeOfOneDayBefore.name,
            ProjectMetrics::capSizeOfOneWeekBefore.name,
            ProjectMetrics::capSizeOfOneMonthBefore.name,
            ProjectMetrics::pipelineCapSize.name,
            ProjectMetrics::pCapSizeOfOneDayBefore.name,
            ProjectMetrics::pCapSizeOfOneWeekBefore.name,
            ProjectMetrics::pCapSizeOfOneMonthBefore.name,
            ProjectMetrics::customCapSize.name,
            ProjectMetrics::cCapSizeOfOneDayBefore.name,
            ProjectMetrics::cCapSizeOfOneWeekBefore.name,
            ProjectMetrics::cCapSizeOfOneMonthBefore.name,
            ProjectMetrics::createdDate.name,
            )
        val fileName = "大于${metricsRequest.limitSize/TO_GIGABYTE}GB的项目信息"
        EasyExcelUtils.download(records, fileName, ProjectMetrics::class.java, includeColumns)
    }

    /**
     * 定时将db中的数据更新到缓存中
     */
    @Scheduled(fixedDelay = FIXED_DELAY, initialDelay = INIT_DELAY, timeUnit = TimeUnit.MINUTES)
    fun loadCache() {
        val createdDate = statDateModel.getShedLockInfo()
        val cacheDateList = listOf(
            createdDate,
            createdDate.minusDays(1).toLocalDate().atStartOfDay(),
            createdDate.minusDays(7).toLocalDate().atStartOfDay(),
            createdDate.minusDays(30).toLocalDate().atStartOfDay()
        )
        cacheDateList.forEach {
            MetricsCacheUtil.getProjectMetrics(
                it.format(DateTimeFormatter.ISO_DATE_TIME)
            )
        }

        ProjectType.values().forEach {
            MetricsCacheUtil.gerProjectNodeNum(it.name)
        }

    }

    private fun getProjectMetrics(metricsRequest: ProjectMetricsRequest): List<ProjectMetrics> {
        val createdDate = if (metricsRequest.default) {
            statDateModel.getShedLockInfo()
        } else {
            LocalDate.now().minusDays(metricsRequest.minusDay).atStartOfDay()
        }
        var oneDayBeforeMetrics: List<TProjectMetrics>? = null
        var oneWeekBeforeMetrics: List<TProjectMetrics>? = null
        var oneMonthBeforeMetrics: List<TProjectMetrics>? = null

        if (metricsRequest.showDelta) {
            val oneDayBefore = createdDate.minusDays(1).toLocalDate().atStartOfDay()
            val oneWeekBefore = createdDate.minusDays(7).toLocalDate().atStartOfDay()
            val oneMonthBefore = createdDate.minusDays(30).toLocalDate().atStartOfDay()

            oneDayBeforeMetrics = MetricsCacheUtil.getProjectMetrics(
                oneDayBefore.format(DateTimeFormatter.ISO_DATE_TIME)
            )
            oneWeekBeforeMetrics = MetricsCacheUtil.getProjectMetrics(
                oneWeekBefore.format(DateTimeFormatter.ISO_DATE_TIME)
            )
            oneMonthBeforeMetrics = MetricsCacheUtil.getProjectMetrics(
                oneMonthBefore.format(DateTimeFormatter.ISO_DATE_TIME)
            )
        }


        val currentMetrics = MetricsCacheUtil.getProjectMetrics(
            createdDate.format(DateTimeFormatter.ISO_DATE_TIME)
        )

        return getMetricsResult(
            type = metricsRequest.type,
            limitSize = metricsRequest.limitSize,
            currentMetrics = currentMetrics,
            oneDayBeforeMetrics = oneDayBeforeMetrics,
            oneWeekBeforeMetrics = oneWeekBeforeMetrics,
            oneMonthBeforeMetrics = oneMonthBeforeMetrics,
        )
    }

    private fun getMetricsResult(
        type: String?,
        limitSize: Long,
        currentMetrics: List<TProjectMetrics>,
        oneDayBeforeMetrics: List<TProjectMetrics>? = null,
        oneWeekBeforeMetrics: List<TProjectMetrics>? = null,
        oneMonthBeforeMetrics: List<TProjectMetrics>? = null,
    ): List<ProjectMetrics> {
        val result = mutableListOf<ProjectMetrics>()
        currentMetrics.forEach { current ->
            val projectInfo = getProjectMetrics(
                current = current,
                type = type,
                oneDayBefore = oneDayBeforeMetrics?.firstOrNull { it.projectId == current.projectId },
                oneWeekBefore = oneWeekBeforeMetrics?.firstOrNull { it.projectId == current.projectId },
                oneMonthBefore = oneMonthBeforeMetrics?.firstOrNull { it.projectId == current.projectId },
                )
            if (projectInfo.capSize >= limitSize) {
                result.add(ProjectMetrics(
                    projectId = current.projectId,
                    capSize = projectInfo.capSize / TO_GIGABYTE,
                    capSizeOfOneDayBefore = projectInfo.capSizeOfOneDayBefore / TO_GIGABYTE ,
                    capSizeOfOneWeekBefore = projectInfo.capSizeOfOneWeekBefore / TO_GIGABYTE,
                    capSizeOfOneMonthBefore = projectInfo.capSizeOfOneMonthBefore / TO_GIGABYTE,
                    nodeNum = projectInfo.nodeNum,
                    createdDate = current.createdDate,
                    pipelineCapSize = projectInfo.pipelineCapSize / TO_GIGABYTE,
                    pCapSizeOfOneDayBefore = projectInfo.pCapSizeOfOneDayBefore / TO_GIGABYTE,
                    pCapSizeOfOneWeekBefore = projectInfo.pCapSizeOfOneWeekBefore / TO_GIGABYTE,
                    pCapSizeOfOneMonthBefore = projectInfo.pCapSizeOfOneMonthBefore / TO_GIGABYTE,
                    customCapSize = projectInfo.customCapSize / TO_GIGABYTE,
                    cCapSizeOfOneDayBefore = projectInfo.cCapSizeOfOneDayBefore / TO_GIGABYTE,
                    cCapSizeOfOneWeekBefore = projectInfo.cCapSizeOfOneWeekBefore / TO_GIGABYTE,
                    cCapSizeOfOneMonthBefore = projectInfo.cCapSizeOfOneMonthBefore / TO_GIGABYTE,
                ))
            }
        }
        return result.sortedByDescending { it.capSize }
    }


    private fun getProjectMetrics(
        current: TProjectMetrics,
        type: String?,
        oneDayBefore: TProjectMetrics? = null,
        oneWeekBefore: TProjectMetrics? = null,
        oneMonthBefore: TProjectMetrics? = null,
    ): ProjectMetrics {
        return if (type.isNullOrEmpty()) {

            val pipelineCapSize = filterByRepoName(current, PIPELINE)
            val pCapSizeOfOneDayBefore = filterByRepoName(oneDayBefore, PIPELINE)
            val pCapSizeOfOneWeekBefore = filterByRepoName(oneWeekBefore, PIPELINE)
            val pCapSizeOfOneMonthBefore = filterByRepoName(oneMonthBefore, PIPELINE)


            val customCapSize = filterByRepoName(current, CUSTOM)
            val cCapSizeOfOneDayBefore = filterByRepoName(oneDayBefore, CUSTOM)
            val cCapSizeOfOneWeekBefore = filterByRepoName(oneWeekBefore, CUSTOM)
            val cCapSizeOfOneMonthBefore = filterByRepoName(oneMonthBefore, CUSTOM)

            ProjectMetrics(
                projectId = current.projectId,
                nodeNum = current.nodeNum,
                capSize = current.capSize,
                capSizeOfOneDayBefore = current.capSize - (oneDayBefore?.capSize ?: 0),
                capSizeOfOneWeekBefore = current.capSize - (oneWeekBefore?.capSize ?: 0),
                capSizeOfOneMonthBefore = current.capSize - (oneMonthBefore?.capSize ?: 0),
                createdDate = current.createdDate,
                pipelineCapSize = pipelineCapSize,
                pCapSizeOfOneDayBefore = pipelineCapSize - pCapSizeOfOneDayBefore,
                pCapSizeOfOneWeekBefore = pipelineCapSize - pCapSizeOfOneWeekBefore,
                pCapSizeOfOneMonthBefore = pipelineCapSize - pCapSizeOfOneMonthBefore,
                customCapSize = customCapSize,
                cCapSizeOfOneDayBefore = customCapSize - cCapSizeOfOneDayBefore,
                cCapSizeOfOneWeekBefore = customCapSize - cCapSizeOfOneWeekBefore,
                cCapSizeOfOneMonthBefore = customCapSize - cCapSizeOfOneMonthBefore,
            )
        } else {

            val (sizeOfRepoType, nodeNumOfRepoType) = filterByRepoType(current, type)
            val (sizeOfOneDayBefore, _) = filterByRepoType(oneDayBefore, type)
            val (sizeOfOneWeekBefore, _) = filterByRepoType(oneWeekBefore, type)
            val (sizeOfOneMonthBefore, _) = filterByRepoType(oneMonthBefore, type)

            ProjectMetrics(
                projectId = current.projectId,
                nodeNum = nodeNumOfRepoType,
                capSize = sizeOfRepoType,
                capSizeOfOneDayBefore = sizeOfRepoType - sizeOfOneDayBefore,
                capSizeOfOneWeekBefore = sizeOfRepoType - sizeOfOneWeekBefore,
                capSizeOfOneMonthBefore = sizeOfRepoType - sizeOfOneMonthBefore,
                createdDate = current.createdDate,
                pipelineCapSize = 0,
                customCapSize = 0
            )
        }
    }



    private fun filterByRepoName(metric: TProjectMetrics?, repoName: String): Long {
        return metric?.repoMetrics?.firstOrNull { it.repoName == repoName }?.size ?: 0
    }

    private fun filterByRepoType(metric: TProjectMetrics?, repoType: String): Pair<Long, Long> {
        var sizeOfRepoType: Long = 0
        var nodeNumOfRepoType: Long = 0
        metric?.repoMetrics?.forEach { repo ->
            if (repo.type == repoType) {
                sizeOfRepoType += repo.size
                nodeNumOfRepoType += repo.num
            }
        }
        return Pair(sizeOfRepoType, nodeNumOfRepoType)
    }

    companion object {
        private const val FIXED_DELAY = 1L
        private const val INIT_DELAY = 3L
    }
}