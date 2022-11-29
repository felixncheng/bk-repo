package com.tencent.bkrepo.grpc.server

import com.tencent.bkrepo.grpc.server.annotation.GrpcService
import io.grpc.BindableService
import io.grpc.ServerBuilder
import java.lang.IllegalStateException
import org.slf4j.LoggerFactory
import io.grpc.protobuf.services.ProtoReflectionService
import org.springframework.context.support.GenericApplicationContext

class GrpcServiceServerBuilderCustomizer(val applicationContext: GenericApplicationContext) : GrpcServerBuilderCustomizer {
    override fun customize(builder: ServerBuilder<*>) {
        val services = applicationContext.getBeansWithAnnotation(GrpcService::class.java).values
        services.forEach {
            if (it is BindableService) {
                builder.addService(it)
                logger.info("Adding service: ${it.javaClass.simpleName}")
            } else {
                throw IllegalStateException("GrpcService must instance of BindableService")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GrpcServiceServerBuilderCustomizer::class.java)
    }
}
