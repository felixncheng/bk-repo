package com.tencent.bkrepo.grpc.server.event

import com.tencent.bkrepo.grpc.server.GrpcApplicationContext
import io.grpc.Server
import org.springframework.context.ApplicationEvent

class GrpcServerInitializedEvent(server: Server, context: GrpcApplicationContext) : ApplicationEvent(context)