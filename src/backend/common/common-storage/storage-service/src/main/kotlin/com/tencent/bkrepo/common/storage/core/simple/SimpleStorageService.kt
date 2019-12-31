package com.tencent.bkrepo.common.storage.core.simple

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.file.ArtifactFileFactory
import com.tencent.bkrepo.common.storage.core.AbstractStorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import java.io.File
import org.apache.commons.fileupload.util.Streams

/**
 * 存储服务简单实现
 *
 * @author: carrypan
 * @date: 2019/12/26
 */
class SimpleStorageService : AbstractStorageService() {

    override fun doStore(path: String, filename: String, artifactFile: ArtifactFile, credentials: StorageCredentials) {
        // force to write file
        val file = if (artifactFile.isInMemory()) {
            val tempArtifactFile = ArtifactFileFactory.build(0)
            Streams.copy(artifactFile.getInputStream(), tempArtifactFile.getOutputStream(), true)
            tempArtifactFile.getTempFile()
        } else {
            artifactFile.getTempFile()
        }
        fileStorage.store(path, filename, file, credentials)
    }

    override fun doStore(path: String, filename: String, file: File, credentials: StorageCredentials) {
        fileStorage.store(path, filename, file, credentials)
    }

    override fun doLoad(path: String, filename: String, credentials: StorageCredentials): File? {
        val tempArtifactFile = ArtifactFileFactory.build(0)
        return fileStorage.load(path, filename, tempArtifactFile.getTempFile(), credentials)
    }

    override fun doDelete(path: String, filename: String, credentials: StorageCredentials) {
        fileStorage.delete(path, filename, credentials)
    }

    override fun doExist(path: String, filename: String, credentials: StorageCredentials): Boolean {
        return fileStorage.exist(path, filename, credentials)
    }
}
