package com.tencent.bkrepo.grpc.server

import com.tencent.bkrepo.grpc.server.event.GrpcServerInitializedEvent
import io.grpc.Server
import org.slf4j.LoggerFactory

class GrpcServerManager(val server: Server, val applicationContext: GrpcApplicationContext) {

    fun start() {
        server.start()
        logger.info("Server started, listening on ${server.port}")
        applicationContext.publishEvent(GrpcServerInitializedEvent(server, applicationContext))
    }

    fun stop() {
        server.shutdown()
        logger.info("Shutting down gRPC server")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GrpcServerManager::class.java)
    }
}
