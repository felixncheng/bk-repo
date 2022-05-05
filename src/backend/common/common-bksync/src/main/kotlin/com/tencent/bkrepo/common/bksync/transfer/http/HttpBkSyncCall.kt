package com.tencent.bkrepo.common.bksync.transfer.http

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.hash.HashCode
import com.tencent.bkrepo.common.api.net.speedtest.NetSpeedTest
import com.tencent.bkrepo.common.api.net.speedtest.SpeedTestSettings
import com.tencent.bkrepo.common.bksync.BkSync
import com.tencent.bkrepo.common.bksync.transfer.exception.PatchRequestException
import com.tencent.bkrepo.common.bksync.transfer.exception.SignRequestException
import com.tencent.bkrepo.common.api.util.HumanReadable
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.api.util.executeAndMeasureNanoTime
import com.tencent.bkrepo.common.bksync.DiffResult
import com.tencent.bkrepo.common.bksync.transfer.exception.ReportSpeedException
import com.tencent.bkrepo.common.bksync.transfer.exception.UploadSignFileException
import okhttp3.HttpUrl
import kotlin.system.measureNanoTime
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.internal.sse.RealEventSource
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch

/**
 * bksync http实现
 * */
class HttpBkSyncCall(
    // http客户端
    private val client: OkHttpClient,
    // 重复率阈值，只有大于该阈值时才会使用增量上传
    private val reuseThreshold: Float = DEFAULT_THRESHOLD,
    // 只有小于该带宽时才会增量上传,单位MB
    private val allowUseMaxBandwidth: Int = -1,
    private val speedTestSettings: SpeedTestSettings?
) {

    @Volatile
    private var patchSuccess = true

    /**
     * 增量上传
     * 在重复率较低或者发生异常时，转为普通上传
     * */
    fun upload(request: UploadRequest) {
        try {
            if (allowUseMaxBandwidth > 0 && speedTestSettings != null) {
                if (!checkSpeed(request, speedTestSettings) && request.genericUrl != null) {
                    logger.info("Faster internet,use common generic upload.")
                    commonUpload(request)
                }
            }
            val nanos = measureNanoTime { doUpload(request) }
            logger.info("Upload[${request.file}] success,elapsed ${HumanReadable.time(nanos)}.")
            afterUpload(request)
        } catch (e: Exception) {
            if (e is SignRequestException) {
                logger.debug("Upload failed: ${e.message}")
            } else {
                logger.debug("Upload failed: ", e)
            }
            request.genericUrl?.let {
                commonUpload(request)
                afterUpload(request)
            }
        }
    }

    /**
     * 检查网络速度
     * @return true为低于允许使用的最大带宽，反之则大于允许的最大带宽
     * */
    private fun checkSpeed(
        request: UploadRequest,
        speedTestSettings: SpeedTestSettings
    ): Boolean {
        val speed = getSpeed(request.speedReportUrl)
        val avgMb = if (speed == -1) {
            val speedTest = NetSpeedTest(speedTestSettings)
            val avgBytes = speedTest.uploadTest()
            logger.debug("Internet speed is measured as ${HumanReadable.size(avgBytes)}/s")
            val base = 1024 * 1024
            val avgMb = avgBytes / base
            reportSpeed(request.speedReportUrl, avgMb.toInt())
            avgMb.toInt()
        } else speed
        if (avgMb > allowUseMaxBandwidth) {
            return false
        }
        return true
    }

    /**
     * 上传结束后行为
     * */
    private fun afterUpload(request: UploadRequest) {
        try {
            val existNewFileSign = existNewFileSign(request)
            if (!existNewFileSign) {
                uploadNewSignFile(request)
            }
        } catch (e: Exception) {
            logger.debug("Upload sign file error.", e)
        }
    }

    private fun reportSpeed(url: String, speed: Int) {
        try {
            val reportUrl = HttpUrl.get(url).newBuilder()
                .addQueryParameter("speed", "$speed")
                .addQueryParameter("action", UPLOAD_ACTION).build()
            val request = Request.Builder()
                .url(reportUrl)
                .put(RequestBody.create(null, ByteArray(0)))
                .build()
            val response = client.newCall(request).execute()
            response.use {
                if (!it.isSuccessful) {
                    throw ReportSpeedException("Report speed failed:${response.message()}")
                }
            }
        } catch (e: Exception) {
            logger.debug("Report speed error", e)
        }
    }

    private fun getSpeed(url: String): Int {
        val reportUrl = HttpUrl.get(url).newBuilder()
            .addQueryParameter("action", UPLOAD_ACTION).build()
        val request = Request.Builder().url(reportUrl).build()
        val response = client.newCall(request).execute()
        response.use {
            if (it.isSuccessful) {
                val byteStream = it.body()?.byteStream()!!
                val result = JsonUtils.objectMapper.readValue(
                    byteStream,
                    object : TypeReference<com.tencent.bkrepo.common.api.pojo.Response<Int>>() {}
                )
                val data = result.data!!
                logger.debug("Get speed from server: $data MB/s.")
                return data
            }
        }
        return -1
    }

    /**
     * 上传具体实现
     * 1. 请求sign
     * 2. 计算diff并且patch
     * */
    private fun doUpload(request: UploadRequest) {
        with(request) {
            // 请求sign
            logger.info("Request sign")
            val signResponse = downloadSign()
            signResponse.use {
                val signStream = signResponse.body()?.byteStream() ?: let {
                    throw SignRequestException("Sign stream broken: ${signResponse.message()}.")
                }
                patch(signStream.buffered())
            }
        }
    }

    /**
     * 上传新文件的sign file
     * */
    @Suppress("UnstableApiUsage")
    private fun uploadNewSignFile(request: UploadRequest) {
        with(request) {
            logger.info("Start sign file.")
            val md5DigestInputStream = DigestInputStream(file.inputStream(), MessageDigest.getInstance("MD5"))
            val byteOutputStream = ByteArrayOutputStream()
            BkSync(BLOCK_SIZE).checksum(md5DigestInputStream, byteOutputStream)
            val md5Data = md5DigestInputStream.messageDigest.digest()
            val md5 = HashCode.fromBytes(md5Data).toString()
            logger.info("End sign file.")
            val newSignFileData = byteOutputStream.toByteArray()
            logger.info("Start upload sign file.")
            val signFileBody = RequestBody.create(MediaType.get(APPLICATION_OCTET_STREAM), newSignFileData)
            val uploadUrl = HttpUrl.parse(newFileSignUrl)!!.newBuilder().addQueryParameter(
                QUERY_PARAM_MD5, md5
            ).build()
            val signRequest = Request.Builder()
                .url(uploadUrl)
                .put(signFileBody)
                .headers(headers)
                .build()
            val response = client.newCall(signRequest).execute()
            response.use {
                if (!it.isSuccessful) {
                    throw UploadSignFileException(response.message())
                }
            }
            logger.info("Upload[$file] sign file success.")
        }
    }

    /**
     * 判断新文件的签名文件是否存在
     * */
    private fun existNewFileSign(request: UploadRequest): Boolean {
        with(request) {
            val req = Request.Builder()
                .url(newFileSignUrl)
                .head()
                .headers(headers)
                .build()
            val resp = client.newCall(req).execute()
            resp.use {
                if (it.isSuccessful) {
                    logger.info("Sign file already existed.")
                    return true
                }
            }
            return false
        }
    }

    /**
     * 获取sign数据流
     * */
    private fun UploadRequest.downloadSign(): Response {
        val signRequest = Request.Builder()
            .url(signUrl)
            .headers(headers)
            .build()
        val response = client.newCall(signRequest).execute()
        if (!response.isSuccessful) {
            response.close()
            throw SignRequestException("Request sign error: ${response.message()}.")
        }
        return response
    }

    /**
     * 根据传人的sign数据流，进行diff计算，并且发起patch请求
     * */
    private fun UploadRequest.patch(signStream: InputStream) {
        val deltaFile = createTempFile()
        try {
            deltaFile.outputStream().buffered().use {
                logger.info("Detecting diff")
                val result = detecting(file, signStream, it)
                if (result.hitRate < reuseThreshold) {
                    logger.info(
                        "Current reuse hit rate[${result.hitRate}]" +
                            " less than threshold[$reuseThreshold],use common upload."
                    )
                    commonUpload(this)
                    return
                }
            }
            logger.info("Start upload delta file.")
            val body = RequestBody.create(MediaType.get(APPLICATION_OCTET_STREAM), deltaFile)
            val patchRequest = Request.Builder()
                .url(deltaUrl)
                .headers(headers)
                .header(X_BKREPO_OLD_FILE_PATH, oldFilePath)
                .patch(body)
                .build()
            val nanos = measureNanoTime {
                val countDownLatch = CountDownLatch(1)
                var errorMsg: String? = null
                val errorCallback = object : PatchErrorCallback {
                    override fun onFailure(msg: String) {
                        patchSuccess = false
                        errorMsg = msg
                    }
                }
                val eventListener = PatchEventListener(countDownLatch, errorCallback)
                val realEventSource = RealEventSource(patchRequest, eventListener)
                realEventSource.connect(client)
                countDownLatch.await()
                if (!patchSuccess) {
                    throw PatchRequestException("Delta upload failed: ${errorMsg.orEmpty()}")
                }
            }
            logger.info("Delta data upload success,elapsed ${HumanReadable.time(nanos)}.")
        } finally {
            deltaFile.delete()
            logger.info("Delete temp deltaFile [$deltaFile] success.")
        }
    }

    /**
     * 检测文件增量
     * */
    private fun detecting(
        file: File,
        signInputStream: InputStream,
        deltaOutputStream: OutputStream
    ): DiffResult {
        with(HumanReadable) {
            val (result, nanos) = executeAndMeasureNanoTime {
                BkSync(BLOCK_SIZE).diff(file, signInputStream, deltaOutputStream)
            }
            val bytes = file.length()
            logger.info(
                "Detecting file[$file] success, " +
                    "size: ${size(bytes)}, elapse: ${time(nanos)}, average: ${throughput(bytes, nanos)}."
            )
            return result
        }
    }

    /**
     * 普通上传
     * */
    private fun commonUpload(request: UploadRequest) {
        with(request) {
            logger.info("Start use generic upload.")
            genericUrl ?: throw IllegalArgumentException("No genericUrl.")
            val body = RequestBody.create(MediaType.get(APPLICATION_OCTET_STREAM), file)
            val commonRequest = Request.Builder()
                .url(genericUrl!!)
                .put(body)
                .headers(headers)
                .build()
            val nanos = measureNanoTime {
                val response = client.newCall(commonRequest).execute()
                response.use {
                    if (!it.isSuccessful) {
                        throw PatchRequestException("Generic upload[$genericUrl] failed.")
                    }
                }
            }
            logger.info("Generic upload[$file] success, elapsed ${HumanReadable.time(nanos)}.")
        }
    }

    /**
     * 添加header
     * */
    private fun Request.Builder.headers(headers: Map<String, String>): Request.Builder {
        headers.forEach { (name, value) -> header(name, value) }
        return this
    }

    private class PatchEventListener(val countDownLatch: CountDownLatch, val errorCallback: PatchErrorCallback) :
        EventSourceListener() {
        override fun onOpen(eventSource: EventSource, response: Response) {
            logger.info("Begin patch.")
        }

        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            if (type == PATCH_EVENT_TYPE_ERROR) {
                errorCallback.onFailure(data)
                countDownLatch.countDown()
            }
            logger.info("Receive event[$type]: $data")
        }

        override fun onClosed(eventSource: EventSource) {
            countDownLatch.countDown()
            logger.info("End patch")
        }

        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
            logger.error("Patch failed", t)
            errorCallback.onFailure(t?.message.orEmpty())
            countDownLatch.countDown()
            response?.close()
        }
    }

    private interface PatchErrorCallback {
        fun onFailure(msg: String)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HttpBkSyncCall::class.java)
        private const val APPLICATION_OCTET_STREAM = "application/octet-stream"
        private const val X_BKREPO_OLD_FILE_PATH = "X-BKREPO-OLD-FILE-PATH"
        private const val DEFAULT_THRESHOLD = 0.2f
        private const val BLOCK_SIZE = 2048
        private const val PATCH_EVENT_TYPE_ERROR = "ERROR"
        private const val QUERY_PARAM_MD5 = "md5"
        private const val HEADER_MD5 = "X-BKREPO-MD5"
        private const val UPLOAD_ACTION = "UPLOAD"
    }
}