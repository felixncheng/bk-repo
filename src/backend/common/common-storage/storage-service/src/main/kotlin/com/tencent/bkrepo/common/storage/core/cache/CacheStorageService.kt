package com.tencent.bkrepo.common.storage.core.cache

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.storage.core.AbstractStorageService
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.filesystem.FileSystemClient
import org.springframework.beans.factory.annotation.Autowired
import java.io.File

/**
 * 支持缓存的存储服务
 *
 * @author: carrypan
 * @date: 2019/12/26
 */
class CacheStorageService : AbstractStorageService() {

    @Autowired
    private lateinit var storageProperties: StorageProperties

    private val cacheClient: FileSystemClient by lazy { FileSystemClient(storageProperties.cache.path) }

    override fun doStore(path: String, filename: String, artifactFile: ArtifactFile, credentials: StorageCredentials) {
        val cachedFile = cacheClient.store(path, filename, artifactFile.getInputStream())
        fileStorage.store(path, filename, cachedFile, credentials)
    }

    override fun doStore(path: String, filename: String, file: File, credentials: StorageCredentials) {
        fileStorage.store(path, filename, file, credentials)
    }

    override fun doLoad(path: String, filename: String, credentials: StorageCredentials): File? {
        return cacheClient.load(path, filename) ?: run {
            val cachedFile = cacheClient.touch(path, filename)
            fileStorage.load(path, filename, cachedFile, credentials)
        }
    }

    override fun doDelete(path: String, filename: String, credentials: StorageCredentials) {
        fileStorage.delete(path, filename, credentials)
        cacheClient.delete(path, filename)
    }

    override fun doExist(path: String, filename: String, credentials: StorageCredentials): Boolean {
        return fileStorage.exist(path, filename, credentials)
    }
}
