package com.tencent.bkrepo.repository.service.node.impl

import com.tencent.bkrepo.archive.api.ArchiveClient
import com.tencent.bkrepo.archive.request.ArchiveFileRequest
import com.tencent.bkrepo.archive.request.UncompressFileRequest
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.pojo.node.service.NodeArchiveRestoreRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeArchiveRequest
import com.tencent.bkrepo.repository.service.node.NodeArchiveOperation
import com.tencent.bkrepo.repository.util.NodeQueryHelper
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Update

class NodeArchiveSupport(
    private val nodeBaseService: NodeBaseService,
    private val archiveClient: ArchiveClient,
) : NodeArchiveOperation {
    val nodeDao: NodeDao = nodeBaseService.nodeDao

    override fun archiveNode(nodeArchiveRequest: NodeArchiveRequest) {
        with(nodeArchiveRequest) {
            val query = NodeQueryHelper.nodeQuery(projectId, repoName, fullPath)
            val update = Update().set(TNode::archived.name, true)
            nodeDao.updateFirst(query, update)
            logger.info("Archive node $projectId/$repoName/$fullPath.")
        }
    }

    override fun restoreNode(nodeArchiveRequest: NodeArchiveRequest) {
        with(nodeArchiveRequest) {
            val query = NodeQueryHelper.nodeQuery(projectId, repoName, fullPath)
            val update = Update().set(TNode::archived.name, false)
                .set(TNode::lastAccessDate.name, LocalDateTime.now())
            nodeDao.updateFirst(query, update)
            logger.info("Restore node $projectId/$repoName/$fullPath.")
        }
    }

    override fun restoreNode(nodeRestoreRequest: NodeArchiveRestoreRequest): List<String> {
        with(nodeRestoreRequest) {
            val query = NodeQueryHelper.queryArchiveNode(projectId, repoName, path, metadata)
            query.limit(limit)
            val nodes = nodeDao.find(query)
            logger.info("Find ${nodes.size} nodes to restore.")
            if (nodes.isEmpty()) {
                return emptyList()
            }
            val repoId = ArtifactContextHolder.RepositoryId(projectId, repoName)
            val repo = ArtifactContextHolder.getRepoDetail(repoId)
            val storageCredentialsKey = repo.storageCredentials?.key
            return nodes.map {
                val sha256 = it.sha256!!
                if (it.archived == true) {
                    val req = ArchiveFileRequest(sha256, storageCredentialsKey, operator)
                    archiveClient.restore(req)
                } else {
                    val req = UncompressFileRequest(sha256, storageCredentialsKey, operator)
                    archiveClient.uncompress(req)
                }
                logger.info("Restoring node $$projectId/$repoName/${it.fullPath}.")
                it.fullPath
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NodeArchiveSupport::class.java)
    }
}
