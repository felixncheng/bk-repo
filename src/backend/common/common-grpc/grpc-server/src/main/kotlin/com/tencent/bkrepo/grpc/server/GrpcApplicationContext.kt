package com.tencent.bkrepo.grpc.server

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.ApplicationEvent
import org.springframework.context.support.GenericApplicationContext

class GrpcApplicationContext(val applicationContext: GenericApplicationContext, val factory: GrpcServerFactory) {

    var grpcServerManager: GrpcServerManager? = null

    fun createGrpcServer() {
        grpcServerManager ?: let {
            val createServer = applicationContext.applicationStartup.start("devops.grpc.server.create")
            createServer.tag("factory", factory::class.java.toString())
            val grpcServer = factory.getGrpcServer()
            this.grpcServerManager = GrpcServerManager(grpcServer, this)
            getBeanFactory().registerSingleton(
                "grpcServerStartStop",
                GrpcServerStartStopLifecycle(this.grpcServerManager!!)
            )
            createServer.end()
        }
    }

    fun publishEvent(event: ApplicationEvent) {
        applicationContext.publishEvent(event)
    }

    private fun getBeanFactory(): ConfigurableListableBeanFactory {
        return applicationContext.beanFactory
    }
}
