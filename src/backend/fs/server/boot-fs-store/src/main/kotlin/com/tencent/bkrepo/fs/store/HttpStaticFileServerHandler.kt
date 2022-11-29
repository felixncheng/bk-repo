package com.tencent.bkrepo.fs.store

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelProgressiveFuture
import io.netty.channel.ChannelProgressiveFutureListener
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpChunkedInput
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpUtil
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.stream.ChunkedFile
import java.io.File
import java.io.RandomAccessFile

class HttpStaticFileServerHandler(val path:String) : SimpleChannelInboundHandler<FullHttpRequest>() {
    private var request: FullHttpRequest? = null

    @Throws(Exception::class)
    public override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {
        this.request = request
        val file = File(path)
        val raf = RandomAccessFile(file, "r")
        val fileLength = raf.length()
        val response: HttpResponse = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
        HttpUtil.setContentLength(response, fileLength)
        if (!HttpUtil.isKeepAlive(request)) {
            response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.CLOSE
        } else if (request.protocolVersion() == HttpVersion.HTTP_1_0) {
            response.headers()[HttpHeaderNames.CONNECTION] = HttpHeaderValues.KEEP_ALIVE
        }

        // Write the initial line and the header.
        ctx.write(response)

        // Write the content.

        val newProgressivePromise = ctx.newProgressivePromise()
        var start=System.currentTimeMillis()
        println("Add $newProgressivePromise")
        val sendFileFuture = ctx.write(
            HttpChunkedInput(ChunkedFile(raf, 0, fileLength, 8192)),
            newProgressivePromise
        ).addListener(ChannelFutureListener.CLOSE)
        println("Add listen ${System.currentTimeMillis()-start}")
        sendFileFuture.addListener(object : ChannelProgressiveFutureListener {
            var start = System.currentTimeMillis()
            var last = System.currentTimeMillis()
            var lastProgress = 0L
            override fun operationProgressed(future: ChannelProgressiveFuture, progress: Long, total: Long) {
                val current = System.currentTimeMillis()
                val took = current - last
                last = current
                val send = progress - lastProgress
                lastProgress = progress
                if (total < 0) { // total unknown
                    System.err.println(future.channel().toString() + " Transfer progress: " + progress)
                } else {
                    System.err.println(
                        future.channel()
                            .toString() + " Transfer progress: " + progress + " / " + total + " send " + send + " took " + took + " ms"
                    )
                }
            }

            override fun operationComplete(future: ChannelProgressiveFuture) {
                System.err.println(
                    future.channel()
                        .toString() + " Transfer complete , send " + lastProgress + " took " + (System.currentTimeMillis() - start) + " ms"
                )
            }
        })
        ctx.channel().flush()
    }
}
