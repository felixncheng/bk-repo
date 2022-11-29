package com.tencent.bkrepo.grpc.server

import com.tencent.bkrepo.grpc.server.annotation.GrpcGlobalInterceptor
import io.grpc.ServerBuilder
import io.grpc.ServerInterceptor
import java.lang.IllegalStateException
import org.slf4j.LoggerFactory
import org.springframework.context.support.GenericApplicationContext

class GrpcGlobalInterceptorServerBuilderCustomizer(val applicationContext: GenericApplicationContext) : GrpcServerBuilderCustomizer {
    override fun customize(builder: ServerBuilder<*>) {
        val interceptors = applicationContext.getBeansWithAnnotation(GrpcGlobalInterceptor::class.java).values
        interceptors.forEach {
            if (it is ServerInterceptor) {
                builder.intercept(it)
                logger.info("Adding interceptor: ${it.javaClass.simpleName}")
            } else {
                throw IllegalStateException("GrpcGlobalInterceptor must instance of ServerInterceptor")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GrpcGlobalInterceptorServerBuilderCustomizer::class.java)
    }
}
