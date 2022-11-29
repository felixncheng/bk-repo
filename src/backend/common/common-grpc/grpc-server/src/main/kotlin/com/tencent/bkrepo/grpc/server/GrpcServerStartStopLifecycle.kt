package com.tencent.bkrepo.grpc.server

import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle

class GrpcServerStartStopLifecycle(val grpcServerManager: GrpcServerManager) : SmartLifecycle {

    @Volatile
    var running: Boolean = false

    override fun start() {
        grpcServerManager.start()
        running = true
    }

    override fun stop() {
        grpcServerManager.stop()
    }

    override fun isRunning(): Boolean {
        return running
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GrpcServerStartStopLifecycle::class.java)
    }
}
