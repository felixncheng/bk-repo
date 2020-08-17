package com.tencent.bkrepo.common.artifact.repository.core

import com.tencent.bkrepo.common.artifact.constant.ATTRIBUTE_MD5MAP
import com.tencent.bkrepo.common.artifact.constant.ATTRIBUTE_OCTET_STREAM_MD5
import com.tencent.bkrepo.common.artifact.constant.ATTRIBUTE_OCTET_STREAM_SHA256
import com.tencent.bkrepo.common.artifact.constant.ATTRIBUTE_SHA256MAP
import com.tencent.bkrepo.common.artifact.constant.OCTET_STREAM
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.common.artifact.exception.ArtifactValidateException
import com.tencent.bkrepo.common.artifact.exception.UnsupportedMethodException
import com.tencent.bkrepo.common.artifact.metrics.ArtifactMetrics
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactListContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactMigrateContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactTransferContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.util.response.ArtifactResourceWriter
import com.tencent.bkrepo.repository.util.NodeUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * 构件仓库抽象类
 */
@Suppress("TooGenericExceptionCaught")
abstract class AbstractArtifactRepository : ArtifactRepository {

    @Autowired
    lateinit var artifactMetrics: ArtifactMetrics

    override fun upload(context: ArtifactUploadContext) {
        try {
            this.onUploadBefore(context)
            this.onUploadValidate(context)
            this.onUpload(context)
            this.onUploadSuccess(context)
        } catch (validateException: ArtifactValidateException) {
            this.onValidateFailed(context, validateException)
        } catch (exception: Exception) {
            this.onUploadFailed(context, exception)
        } finally {
            this.onUploadFinished(context)
        }
    }

    override fun download(context: ArtifactDownloadContext) {
        try {
            this.onDownloadBefore(context)
            this.onDownloadValidate(context)
            val artifactResponse = this.onDownload(context)
                ?: throw ArtifactNotFoundException("Artifact[${context.artifactInfo}] not found")
            ArtifactResourceWriter.write(artifactResponse)
            this.onDownloadSuccess(context)
        } catch (validateException: ArtifactValidateException) {
            this.onValidateFailed(context, validateException)
        } catch (exception: Exception) {
            this.onDownloadFailed(context, exception)
        } finally {
            this.onDownloadFinished(context)
        }
    }

    override fun search(context: ArtifactSearchContext): Any? {
        throw UnsupportedMethodException()
    }

    override fun list(context: ArtifactListContext): Any? {
        throw UnsupportedMethodException()
    }

    override fun remove(context: ArtifactRemoveContext) {
        throw UnsupportedMethodException()
    }

    override fun migrate(context: ArtifactMigrateContext): Any? {
        throw UnsupportedMethodException()
    }

    open fun determineArtifactName(context: ArtifactTransferContext): String {
        val artifactUri = context.artifactInfo.artifactUri
        return artifactUri.substring(artifactUri.lastIndexOf(NodeUtils.FILE_SEPARATOR) + 1)
    }

    /**
     * 验证构件
     */
    @Throws(ArtifactValidateException::class)
    open fun onUploadValidate(context: ArtifactUploadContext) {
        val sha256Map = mutableMapOf<String, String>()
        val md5Map = mutableMapOf<String, String>()
        // 计算sha256和md5
        context.artifactFileMap.entries.forEach { (name, file) ->
            sha256Map[name] = file.getFileSha256()
            md5Map[name] = file.getFileMd5()
            if (name == OCTET_STREAM) {
                context.contextAttributes[ATTRIBUTE_OCTET_STREAM_SHA256] = sha256Map[name] as String
                context.contextAttributes[ATTRIBUTE_OCTET_STREAM_MD5] = md5Map[name] as String
            }
        }
        context.contextAttributes[ATTRIBUTE_SHA256MAP] = sha256Map
        context.contextAttributes[ATTRIBUTE_MD5MAP] = md5Map
    }

    /**
     * 上传前回调
     */
    open fun onUploadBefore(context: ArtifactUploadContext) {
        artifactMetrics.uploadingCount.incrementAndGet()
    }

    /**
     * 上传构件
     */
    open fun onUpload(context: ArtifactUploadContext) {
        throw UnsupportedMethodException()
    }

    /**
     * 上传成功回调
     */
    open fun onUploadSuccess(context: ArtifactUploadContext) {
        artifactMetrics.uploadedCounter.increment()
        val artifactInfo = context.artifactInfo
        val userId = context.userId
        logger.info("User[$userId] upload artifact[$artifactInfo] success")
    }

    /**
     * 上传失败回调
     */
    open fun onUploadFailed(context: ArtifactUploadContext, exception: Exception) {
        // 默认向上抛异常，由全局异常处理器处理
        throw exception
    }

    /**
     * 下载验证
     */
    @Throws(ArtifactValidateException::class)
    open fun onDownloadValidate(context: ArtifactDownloadContext) {
    }

    /**
     * 下载前回调
     */
    open fun onDownloadBefore(context: ArtifactDownloadContext) {
        artifactMetrics.downloadingCount.incrementAndGet()
    }

    /**
     * 下载构件
     */
    open fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        throw UnsupportedMethodException()
    }

    /**
     * 下载成功回调
     */
    open fun onDownloadSuccess(context: ArtifactDownloadContext) {
        artifactMetrics.downloadedCounter.increment()

        val artifactInfo = context.artifactInfo
        val userId = context.userId
        logger.info("User[$userId] download artifact[$artifactInfo] success")
    }

    /**
     * 下载失败回调
     */
    open fun onDownloadFailed(context: ArtifactDownloadContext, exception: Exception) {
        // 默认向上抛异常，由全局异常处理器处理
        throw exception
    }

    /**
     * 验证失败回调
     */
    open fun onValidateFailed(context: ArtifactTransferContext, validateException: ArtifactValidateException) {
        // 默认向上抛异常，由全局异常处理器处理
        throw validateException
    }

    /**
     * 上传结束回调
     */
    open fun onUploadFinished(context: ArtifactUploadContext) {
        artifactMetrics.uploadingCount.decrementAndGet()
    }

    /**
     * 下载结束回调
     */
    open fun onDownloadFinished(context: ArtifactDownloadContext) {
        artifactMetrics.downloadingCount.decrementAndGet()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractArtifactRepository::class.java)
    }
}
