package com.tencent.bkrepo.fs.store

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelProgressiveFuture
import io.netty.channel.ChannelProgressiveFutureListener
import io.netty.handler.stream.ChunkedFile
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame
import io.netty.incubator.codec.http3.Http3DataFrame
import io.netty.incubator.codec.http3.Http3HeadersFrame
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler
import io.netty.incubator.codec.quic.QuicStreamChannel
import io.netty.util.ReferenceCountUtil
import java.io.File

class Http3RequestHandler(val path: String) : Http3RequestStreamInboundHandler() {
    override fun channelRead(ctx: ChannelHandlerContext, frame: Http3HeadersFrame, isLast: Boolean) {
        println("Accept header package from ${ctx.channel().remoteAddress()}, $frame")
        frame.headers().forEach {
            println("${it.key}: ${it.value}")
        }
        if (isLast) {
            println("Accept last header package from ${ctx.channel().remoteAddress()}")
            writeResponse(ctx)
        }
        ReferenceCountUtil.release(frame)
    }

    override fun channelRead(ctx: ChannelHandlerContext, frame: Http3DataFrame, isLast: Boolean) {
        println("Accept data package from ${ctx.channel().remoteAddress()}")
        if (isLast) {
            println("Accept last data package from ${ctx.channel().remoteAddress()}")
            writeResponse(ctx)
        }
        ReferenceCountUtil.release(frame)
    }

    private fun writeResponse(ctx: ChannelHandlerContext) {
        val file = File(path)
//            file.writeText("Hello World!")
        val headersFrame: Http3HeadersFrame = DefaultHttp3HeadersFrame()
        headersFrame.headers().status("404")
        headersFrame.headers().add("server", "netty")
        headersFrame.headers().addLong("content-length", file.length())
        ctx.writeAndFlush(headersFrame)
        val sendFileFuture = ctx
            .writeAndFlush(
                ChunkedFile(file, 64 * 1024),
                ctx.newProgressivePromise()
            ).addListener(QuicStreamChannel.SHUTDOWN_OUTPUT)
        sendFileFuture.addListener(
            object : ChannelProgressiveFutureListener {
                val start = System.currentTimeMillis()
                var last = System.currentTimeMillis()
                var lastProgress = 0L
                override fun operationComplete(future: ChannelProgressiveFuture) {
                    println("${future.channel()} Transfer complete, send $lastProgress took${System.currentTimeMillis() - start} ms.")
                }

                override fun operationProgressed(future: ChannelProgressiveFuture, progress: Long, total: Long) {
                    val current = System.currentTimeMillis()
                    val took = current - last
                    last = current
                    val send = progress - lastProgress
                    lastProgress = progress
                    if (total < 0) {
                        println("${future.channel()} Transfer progress: $progress")
                    } else {
                        println("${future.channel()} Transfer progress: $progress/$total,send $send took$took ms")
                    }
                }
            }
        )
    }
}
