package com.tencent.bkrepo.fs.store

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame

class ByteToHttp3FrameHandler : ChannelOutboundHandlerAdapter() {

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        if (msg is ByteBuf) {
            ctx.write(DefaultHttp3DataFrame(msg), promise)
        } else {
            ctx.write(msg, promise)
        }
    }
}
