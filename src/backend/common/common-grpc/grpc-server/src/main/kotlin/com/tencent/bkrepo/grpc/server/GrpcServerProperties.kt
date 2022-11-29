package com.tencent.bkrepo.grpc.server

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("grpc.server")
data class GrpcServerProperties(
    var port: Int = 8080,
    var allowProtoReflection: Boolean = false
)
