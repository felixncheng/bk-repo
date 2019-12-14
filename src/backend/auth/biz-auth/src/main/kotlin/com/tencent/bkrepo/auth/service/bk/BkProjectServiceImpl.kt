package com.tencent.bkrepo.auth.service.bk

import com.tencent.bkrepo.auth.model.TProject
import com.tencent.bkrepo.auth.pojo.CreateProjectRequest
import com.tencent.bkrepo.auth.pojo.Project
import com.tencent.bkrepo.auth.repository.ProjectRepository
import com.tencent.bkrepo.auth.service.ProjectService
import com.tencent.bkrepo.auth.util.TransferUtils
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "bk")
class BkProjectServiceImpl @Autowired constructor(
    private val projectRepository: ProjectRepository
) : ProjectService {
    override fun getByName(name: String): Project? {
        var project = projectRepository.findOneByName(name)
        // 加锁
        if (project == null && (name == "pipeline" || name == "custom" || name == "report")) {
            logger.info("project($name) not exist, create it")
            val insertProject = TProject(
                id = null,
                name = name,
                displayName = name,
                description = ""
            )
            val tProject = projectRepository.insert(TProject(
                id = null,
                name = name,
                displayName = name,
                description = ""
            ))
            return TransferUtils.transferProject(tProject)
        } else {
            return null
        }
    }

    override fun listProject(): List<Project> {
        throw ErrorCodeException(CommonMessageCode.OPERATION_UNSUPPORTED)
    }

    override fun createProject(request: CreateProjectRequest) {
        projectRepository.insert(
            TProject(
                id = null,
                name = request.name,
                displayName = request.displayName,
                description = request.description
            )
        )
    }

    override fun deleteByName(name: String) {
        throw ErrorCodeException(CommonMessageCode.OPERATION_UNSUPPORTED)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkProjectServiceImpl::class.java)
    }
}