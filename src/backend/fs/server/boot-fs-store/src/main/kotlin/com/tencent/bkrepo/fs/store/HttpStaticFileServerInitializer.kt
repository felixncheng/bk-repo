package com.tencent.bkrepo.fs.store

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.ssl.SslContext
import io.netty.handler.stream.ChunkedWriteHandler

class HttpStaticFileServerInitializer(private val sslCtx: SslContext?, val path: String) :
    ChannelInitializer<SocketChannel>() {
    public override fun initChannel(ch: SocketChannel) {
        val pipeline = ch.pipeline()
        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()))
        }
        pipeline.addLast(HttpServerCodec())
        pipeline.addLast(HttpObjectAggregator(65536))
        pipeline.addLast(ChunkedWriteHandler())
        pipeline.addLast(HttpStaticFileServerHandler(path))
    }
}
