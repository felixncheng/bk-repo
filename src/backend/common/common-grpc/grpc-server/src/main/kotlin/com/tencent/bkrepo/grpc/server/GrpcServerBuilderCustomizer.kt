package com.tencent.bkrepo.grpc.server

import io.grpc.ServerBuilder

interface GrpcServerBuilderCustomizer {

    fun customize(builder: ServerBuilder<*>)
}
