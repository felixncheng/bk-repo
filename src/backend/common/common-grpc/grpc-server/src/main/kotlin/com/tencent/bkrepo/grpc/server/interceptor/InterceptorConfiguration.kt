package com.tencent.bkrepo.grpc.server.interceptor

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    GrpcAccessLogInterceptor::class,
    GrpcExceptionInterceptor::class
)
class InterceptorConfiguration
