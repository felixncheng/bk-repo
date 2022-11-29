package com.tencent.bkrepo.grpc.server

interface GrpcServerFactoryCustomizer {

    fun customize(factory: GrpcServerFactory)
}
