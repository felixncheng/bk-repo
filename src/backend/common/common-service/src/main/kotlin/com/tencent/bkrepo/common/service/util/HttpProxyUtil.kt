package com.tencent.bkrepo.common.service.util

import com.tencent.bkrepo.common.api.constant.BASIC_AUTH_PREFIX
import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.util.BasicAuthUtils
import com.tencent.bkrepo.common.service.util.okhttp.HttpClientBuilderFactory
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.source
import org.slf4j.LoggerFactory
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

object HttpProxyUtil {
    private val logger = LoggerFactory.getLogger(HttpProxyUtil::class.java)
    fun proxy(
        proxyRequest: HttpServletRequest,
        proxyResponse: HttpServletResponse,
        targetUrl: String,
        prefix: String? = null,
    ) {
        val client = HttpClientBuilderFactory.create().build()
        val newUrl = "$targetUrl${proxyRequest.requestURI.removePrefix(prefix.orEmpty())}?${proxyRequest.queryString}"
        val newRequest = Request.Builder()
            .url(newUrl)
            .apply {
                proxyRequest.headers().forEach { (key, value) -> this.header(key, value) }
            }
            .method(proxyRequest.method, proxyRequest.body())
            .build()
        val newResponse = client
            .newCall(newRequest)
            .execute()
        proxyRequest.accessLog(newResponse)
        // 转发状态码
        proxyResponse.status = newResponse.code
        // 转发头
        newResponse.headers.forEach { (key, value) -> proxyResponse.addHeader(key, value) }
        // 转发body
        newResponse.body?.byteStream()?.use {
            it.copyTo(proxyResponse.outputStream)
        }
    }

    fun HttpServletRequest.headers(): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val headerNames = this.headerNames
        while (headerNames.hasMoreElements()) {
            val headerName = headerNames.nextElement()
            headers[headerName] = this.getHeader(headerName)
        }
        return headers
    }

    fun HttpServletRequest.body(): RequestBody? {
        if (this.contentLengthLong <= 0) {
            return null
        }
        val mediaType = this.contentType?.toMediaTypeOrNull()
        val inputStream = this.inputStream
        val contentLength = this.contentLengthLong
        return object : RequestBody() {
            override fun contentType(): MediaType? = mediaType

            override fun contentLength(): Long = contentLength

            override fun writeTo(sink: BufferedSink) {
                inputStream.source().use {
                    sink.writeAll(it)
                }
            }
        }
    }

    private fun HttpServletRequest.accessLog(upRes: Response) {
        var user = "-"
        if (getHeader(HttpHeaders.AUTHORIZATION).orEmpty().startsWith(BASIC_AUTH_PREFIX)) {
            val authorizationHeader = getHeader(HttpHeaders.AUTHORIZATION).orEmpty()
            user = BasicAuthUtils.decode(authorizationHeader).first
        }
        val requestTime = System.currentTimeMillis() - upRes.sentRequestAtMillis
        val httpUserAgent = getHeader(HttpHeaders.USER_AGENT)
        val url = upRes.request.url.host
        val requestBodyBytes = contentLengthLong
        logger.info(
            "\"$method $requestURI $protocol\" - " +
                "user:$user up_status: ${upRes.code} ms:$requestTime up:$url agent:$httpUserAgent $requestBodyBytes",
        )
    }
}
