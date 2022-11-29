package com.tencent.bkrepo.grpc.server.error

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.Status

class GrpcExceptionServerCall<ReqT, RespT>(
    delegate: ServerCall<ReqT, RespT>,
    private val exceptionHandler: GrpcExceptionResponseHandler
) : SimpleForwardingServerCall<ReqT, RespT>(
    delegate
) {

    override fun close(status: Status, trailers: Metadata) {
        if (!status.isOk && status.cause != null) {
            exceptionHandler.handlerError(delegate(), status.cause!!)
        } else {
            super.close(status, trailers)
        }
    }
}
