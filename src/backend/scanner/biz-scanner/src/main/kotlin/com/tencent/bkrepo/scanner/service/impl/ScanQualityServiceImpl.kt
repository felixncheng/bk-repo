/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.service.impl

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.scanner.pojo.scanner.CveOverviewKey
import com.tencent.bkrepo.common.scanner.pojo.scanner.constant.SCANCODE_TOOLKIT
import com.tencent.bkrepo.scanner.component.ScannerPermissionCheckHandler
import com.tencent.bkrepo.scanner.dao.ScanPlanDao
import com.tencent.bkrepo.scanner.pojo.request.ScanQualityUpdateRequest
import com.tencent.bkrepo.scanner.pojo.response.ScanQualityCheckedDetail
import com.tencent.bkrepo.scanner.pojo.response.ScanQualityCheckedDetail.ScanQualityCheckedStatus
import com.tencent.bkrepo.scanner.pojo.response.ScanQualityResponse
import com.tencent.bkrepo.scanner.service.LicenseScanQualityService
import com.tencent.bkrepo.scanner.service.ScanQualityService
import org.springframework.stereotype.Service

@Service
class ScanQualityServiceImpl(
    private val permissionCheckHandler: ScannerPermissionCheckHandler,
    private val scanPlanDao: ScanPlanDao,
    private val licenseScanQualityService: LicenseScanQualityService
) : ScanQualityService {
    override fun getScanQuality(planId: String): ScanQualityResponse {
        val scanPlan = scanPlanDao.get(planId)
        permissionCheckHandler.checkProjectPermission(scanPlan.projectId, PermissionAction.MANAGE)
        return ScanQualityResponse.create(scanPlan.scanQuality)
    }

    override fun updateScanQuality(planId: String, request: ScanQualityUpdateRequest): Boolean {
        val scanPlan = scanPlanDao.get(planId)
        permissionCheckHandler.checkProjectPermission(scanPlan.projectId, PermissionAction.MANAGE)
        val qualityMap = request.toMap().ifEmpty { return true }
        scanPlanDao.updateScanPlanQuality(planId, qualityMap)
        return true
    }

    override fun checkScanQualityRedLine(planId: String, scanResultOverview: Map<String, Number>): Boolean {
        val tScanPlan = scanPlanDao.get(planId)
        // 获取方案质量规则
        val scanQuality = scanPlanDao.get(planId).scanQuality
        if (tScanPlan.scanner == SCANCODE_TOOLKIT){
            return licenseScanQualityService.checkLicenseScanQualityRedLine(scanQuality, scanResultOverview)
        }
        return checkScanQualityRedLine(scanQuality, scanResultOverview)
    }

    override fun checkScanQualityRedLine(
        scanQuality: Map<String, Any>,
        scanResultOverview: Map<String, Number>
    ): Boolean {
        // 检查质量规则是否通过
        CveOverviewKey.values().forEach { overviewKey ->
            if (checkRedLine(overviewKey, scanResultOverview, scanQuality)) {
                return false
            }
        }
        return true
    }

    override fun checkScanQualityRedLineDetail(
        planId: String,
        scanResultOverview: Map<String, Number>
    ): ScanQualityCheckedDetail {
        val scanPlan = scanPlanDao.get(planId)
        permissionCheckHandler.checkProjectPermission(scanPlan.projectId, PermissionAction.MANAGE)
        val scanQuality = scanPlan.scanQuality
        if (scanQuality[ScanQualityResponse::forbidQualityUnPass.name] == false) {
            return ScanQualityCheckedDetail(qualityStatus = true)
        }

        val detailsMap = HashMap<String, ScanQualityCheckedStatus>(CveOverviewKey.values().size)
        var qualityStatus = true
        CveOverviewKey.values().forEach { overviewKey ->
            val cveCount = scanResultOverview[overviewKey.key]?.toLong()
            val redLine = scanQuality[overviewKey.level.levelName] as Long?
            if (cveCount != null && redLine != null) {
                val status = ScanQualityCheckedStatus(
                    status = cveCount <= redLine,
                    require = redLine,
                    actual = cveCount
                )
                qualityStatus = qualityStatus && status.status
                detailsMap[overviewKey.key] = status
            }
        }

        return ScanQualityCheckedDetail(
            criticalStatus = detailsMap[CveOverviewKey.CVE_CRITICAL_COUNT.key],
            highStatus = detailsMap[CveOverviewKey.CVE_HIGH_COUNT.key],
            mediumStatus = detailsMap[CveOverviewKey.CVE_MEDIUM_COUNT.key],
            lowStatus = detailsMap[CveOverviewKey.CVE_LOW_COUNT.key],
            qualityStatus = qualityStatus
        )
    }

    /**
     * 判断[overviewKey]对应的CVE数量是否超过[quality]中指定的红线
     *
     * @return true 超过， false 未超过
     */
    private fun checkRedLine(
        overviewKey: CveOverviewKey,
        overview: Map<String, Number>,
        quality: Map<String, Any>
    ): Boolean {
        val cveCount = overview[overviewKey.key]?.toLong() ?: 0L
        val redLine = quality[overviewKey.level.levelName] as Long? ?: Long.MAX_VALUE
        return cveCount > redLine
    }
}
