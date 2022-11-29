package com.tencent.bkrepo.grpc.server.annotation

import java.lang.annotation.Inherited
import org.springframework.stereotype.Component

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@MustBeDocumented
@Component
annotation class GrpcService
