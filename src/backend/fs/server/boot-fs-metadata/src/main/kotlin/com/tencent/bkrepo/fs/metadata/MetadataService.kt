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

package com.tencent.bkrepo.fs.metadata

import com.tencent.bkrepo.grpc.server.annotation.GrpcService
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeListOption

@GrpcService
class MetadataService(val nodeClient: NodeClient) : MetadataServiceGrpcKt.MetadataServiceCoroutineImplBase() {

    override suspend fun getNode(request: GetNodeRequest): Node {
        with(request) {
            return nodeClient.getNodeDetail(
                projectId = projectId,
                repoName = repoName,
                fullPath = fullPath
            ).data?.toNode() ?: node {}
        }
    }

    override suspend fun listNodes(request: ListNodesRequest): Nodes {
        with(request) {
            val listOption = NodeListOption(
                includeFolder = includeFolder,
                includeMetadata = includeMetadata,
                deep = deep,
                sort = sort
            )
            val nodes = nodeClient.listNodePage(
                path = path,
                projectId = projectId,
                repoName = repoName,
                option = listOption
            ).data?.records?.map {
                it.toNode()
            }?.toList()
            return Nodes.newBuilder().addAllNodes(nodes).build()
        }
    }

    private fun NodeDetail.toNode(): Node {
        val builder = Node.newBuilder()
            .setLastModifiedDate(lastModifiedDate)
            .setName(name)
            .setSize(size)
            .setFolder(folder)
            .setFullPath(fullPath)
        md5?.let { builder.setMd5(md5) }
        sha256?.let { builder.setSha256(sha256) }
        return builder.build()
    }
    private fun NodeInfo.toNode(): Node {
        return NodeDetail(this).toNode()
    }
}
