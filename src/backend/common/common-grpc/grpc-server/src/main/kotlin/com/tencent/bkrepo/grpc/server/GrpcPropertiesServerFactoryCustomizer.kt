package com.tencent.bkrepo.grpc.server

import org.springframework.boot.context.properties.PropertyMapper
import org.springframework.core.Ordered

class GrpcPropertiesServerFactoryCustomizer(private val serverProperties: GrpcServerProperties) : GrpcServerFactoryCustomizer, Ordered {
    override fun customize(factory: GrpcServerFactory) {
        val map = PropertyMapper.get().alwaysApplyingWhenNonNull()
        map.from(serverProperties.port).to(factory::setPort)
        map.from(serverProperties.allowProtoReflection).to(factory::allowProtoReflection)
    }

    override fun getOrder(): Int {
        return 0
    }
}
