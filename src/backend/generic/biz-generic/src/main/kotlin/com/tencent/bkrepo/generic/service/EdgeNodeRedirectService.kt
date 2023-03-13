/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.generic.service

import com.tencent.bkrepo.auth.pojo.token.TemporaryTokenCreateRequest
import com.tencent.bkrepo.auth.pojo.token.TokenType
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.constant.ensureSuffix
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.service.cluster.ClusterProperties
import com.tencent.bkrepo.replication.api.ClusterNodeClient
import com.tencent.bkrepo.replication.exception.ReplicationMessageCode
import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeInfo
import java.time.Duration
import org.springframework.stereotype.Service

/**
 * 边缘节点重定向服务
 * */
@Service
class EdgeNodeRedirectService(
    private val clusterProperties: ClusterProperties,
    private val clusterNodeClient: ClusterNodeClient,
    private val temporaryAccessService: TemporaryAccessService,
) {

    /**
     * 重定向到默认集群节点
     * */
    fun redirectToDefaultCluster(downloadContext: ArtifactDownloadContext) {
        getEdgeClusterName(downloadContext.artifactInfo)?.let {
            redirectToSpecificCluster(downloadContext, it)
        }
    }

    /**
     * 重定向到指定节点
     * */
    fun redirectToSpecificCluster(downloadContext: ArtifactDownloadContext, clusterName: String) {
        // 节点来自其他集群，重定向到其他节点。
        val clusterInfo = clusterNodeClient.getCluster(clusterName).data
            ?: throw ErrorCodeException(ReplicationMessageCode.CLUSTER_NODE_NOT_FOUND, clusterName)
        val edgeDomain = getEdgeDomain(clusterInfo)
        val requestPath = downloadContext.request.requestURI
        val queryString = downloadContext.request.queryString
        val token = createTempToken(downloadContext)
        val redirectUrl = "$edgeDomain$GENERIC_SERVICE_NAME/temporary/download$requestPath?token=$token&$queryString"
        downloadContext.response.sendRedirect(redirectUrl)
    }

    /**
     * 获取边缘节点名称
     * */
    fun getEdgeClusterName(artifactInfo: ArtifactInfo): String? {
        val node = ArtifactContextHolder.getNodeDetail()
            ?: throw NodeNotFoundException(artifactInfo.getArtifactFullPath())
        return node.clusterNames?.firstOrNull { it != clusterProperties.self.name }
    }

    /**
     * 获取边缘节点域名
     * */
    private fun getEdgeDomain(clusterInfo: ClusterNodeInfo): String {
        val url = clusterInfo.url
        if (url.endsWith(StringPool.SLASH)) {
            url.removeSuffix(StringPool.SLASH)
        }
        return url.removeSuffix(REPLICATION_SERVICE_NAME).ensureSuffix(StringPool.SLASH)
    }

    private fun createTempToken(downloadContext: ArtifactDownloadContext): String {
        with(downloadContext) {
            val createTokenRequest = TemporaryTokenCreateRequest(
                projectId = projectId,
                repoName = repoName,
                fullPathSet = setOf(artifactInfo.getArtifactFullPath()),
                expireSeconds = Duration.ofMinutes(5).seconds,
                type = TokenType.DOWNLOAD,
            )
            return temporaryAccessService.createToken(createTokenRequest).first().token
        }
    }

    companion object {
        const val REPLICATION_SERVICE_NAME = "replication"
        const val GENERIC_SERVICE_NAME = "generic"
    }
}