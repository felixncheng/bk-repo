package com.tencent.bkrepo.grpc.server.error

import io.grpc.ServerCall

interface GrpcExceptionResponseHandler {

    fun handlerError(serverCall: ServerCall<*, *>, error: Throwable)
}
