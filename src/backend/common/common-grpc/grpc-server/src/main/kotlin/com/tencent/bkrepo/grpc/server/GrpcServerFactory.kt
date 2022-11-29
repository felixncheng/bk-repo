package com.tencent.bkrepo.grpc.server

import io.grpc.Server

class GrpcServerFactory {
    private val delegate = GrpcServerFactoryDelegate()
    var port: Int = 8080

    fun getBuilderCustomizers(): MutableList<GrpcServerBuilderCustomizer> {
        return delegate.builderCustomizers
    }

    fun getGrpcServer(): Server {
        val builder = delegate.createBuilder(port)
        return builder.build()
    }

    @JvmName("setPort1")
    fun setPort(port: Int) {
        this.port = port
    }

    fun allowProtoReflection(allowProtoReflection: Boolean) {
        delegate.allowProtoReflection = allowProtoReflection
    }
}
