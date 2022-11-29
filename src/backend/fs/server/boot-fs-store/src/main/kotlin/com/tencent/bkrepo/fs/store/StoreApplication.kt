package com.tencent.bkrepo.fs.store

import com.tencent.bkrepo.common.service.condition.MicroService
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.devops.boot.runApplication
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContextBuilder
import io.netty.incubator.codec.http3.Http3
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler
import io.netty.incubator.codec.quic.InsecureQuicTokenHandler
import io.netty.incubator.codec.quic.QuicChannel
import io.netty.incubator.codec.quic.QuicSslContextBuilder
import io.netty.incubator.codec.quic.QuicStreamChannel
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner

@MicroService
class StoreApplication(val storageService: StorageService) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        val ch1 = http3Server(args)
        val ch2 = httpsServer(args)
        ch1.closeFuture().sync()
        ch2.closeFuture().sync()
    }

    private fun httpsServer(args: ApplicationArguments): Channel {
        val port = 9998

        val key = File(args.sourceArgs[0])
        val cert = File(args.sourceArgs[1])
        val sslCtx = SslContextBuilder
            .forServer(cert, key)
            .build()
        val bossGroup: EventLoopGroup = NioEventLoopGroup(1)
        val workerGroup: EventLoopGroup = NioEventLoopGroup()
        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .handler(LoggingHandler(LogLevel.INFO))
            .childHandler(HttpStaticFileServerInitializer(sslCtx, args.sourceArgs[2]))
        val ch: Channel = b.bind(port).sync().channel()
        System.err.println("Open your web browser and navigate to https://127.0.0.1:$port/")
        ch.closeFuture().addListener {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
        return ch
    }

    private fun http3Server(args: ApplicationArguments): Channel {
        // Allow to pass in the port so we can also use it to run h3spec against
        val port = 9999

        val group = NioEventLoopGroup(1)
        val key = File(args.sourceArgs[0])

        val cert = File(args.sourceArgs[1])

        val file = File(args.sourceArgs[2])

        println("key :${key.exists()} ,crt: ${cert.exists()} ,file[${file.canonicalPath}] size:${file.length()}")
        val sslContext = QuicSslContextBuilder.forServer(key, null, cert)
            .applicationProtocols(*Http3.supportedApplicationProtocols()).build()
        val codec: ChannelHandler = Http3.newQuicServerCodecBuilder()
            .sslContext(sslContext)
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(10000000)
            .initialMaxStreamDataBidirectionalLocal(1000000)
            .initialMaxStreamDataBidirectionalRemote(1000000)
            .initialMaxStreamsBidirectional(100)
            .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
            .maxRecvUdpPayloadSize(64 * 1024)
            .maxSendUdpPayloadSize(64 * 1024)
            .handler(object : ChannelInitializer<QuicChannel>() {
                override fun initChannel(ch: QuicChannel) {
                    // Called for each connection
                    ch.pipeline().addLast(
                        Http3ServerConnectionHandler(
                            object : ChannelInitializer<QuicStreamChannel>() {
                                // Called for each request-stream,
                                override fun initChannel(ch: QuicStreamChannel) {
                                    ch.pipeline().addLast(Http3ServerInitializer(args.sourceArgs[2]))
                                }
                            }
                        )
                    )
                }
            }).build()
        val bs = Bootstrap()
        val channel = bs.group(group)
            .channel(NioDatagramChannel::class.java)
            .handler(codec)
            .bind(InetSocketAddress(port)).sync().channel()
        println("Http3 server start $port")
        channel.closeFuture().addListener { group.shutdownGracefully() }
        return channel
    }
}

fun main(args: Array<String>) {
    runApplication<StoreApplication>(args)
}
