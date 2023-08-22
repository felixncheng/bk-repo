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

package com.tencent.bkrepo.replication.api

import com.tencent.bkrepo.common.api.constant.REPLICATION_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.replication.pojo.remote.request.RemoteRunOnceTaskCreateRequest
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(REPLICATION_SERVICE_NAME, contextId = "ReplicaTaskOperationClient", path = "/replica")
interface ReplicaTaskOperationClient {

    /**
     * 创建一次性分发任务
     */
    @PostMapping("/create/runOnceTask/{projectId}/{repoName}")
    fun createRunOnceTask(
        @ApiParam(value = "仓库ID")
        @PathVariable projectId: String,
        @ApiParam(value = "项目ID")
        @PathVariable repoName: String,
        @RequestBody requests: RemoteRunOnceTaskCreateRequest
    ): Response<Void>

    /**
     * 手动调用一次性执行任务
     */
    @PostMapping("/execute/runOnceTask/{projectId}/{repoName}")
    fun executeRunOnceTask(
        @ApiParam(value = "仓库ID")
        @PathVariable projectId: String,
        @ApiParam(value = "项目ID")
        @PathVariable repoName: String,
        @RequestParam name: String,
    ): Response<Void>
}
