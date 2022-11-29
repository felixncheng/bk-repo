package com.tencent.bkrepo.grpc.server

import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService

class GrpcServerFactoryDelegate {
    val builderCustomizers = mutableListOf<GrpcServerBuilderCustomizer>()
    var allowProtoReflection: Boolean = false
    fun createBuilder(port: Int): ServerBuilder<*> {
        val builder = ServerBuilder.forPort(port)
        if (allowProtoReflection) {
            builder.addService(ProtoReflectionService.newInstance())
        }
        builderCustomizers.forEach { it.customize(builder) }
        return builder
    }
}
