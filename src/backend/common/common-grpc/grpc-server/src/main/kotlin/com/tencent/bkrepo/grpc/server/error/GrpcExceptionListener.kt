package com.tencent.bkrepo.grpc.server.error

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener
import io.grpc.ServerCall

class GrpcExceptionListener<ReqT, RespT>(
    delegate: ServerCall.Listener<ReqT>,
    private val serverCall: ServerCall<ReqT, RespT>,
    private val exceptionHandler: GrpcExceptionResponseHandler
) : SimpleForwardingServerCallListener<ReqT>(
    delegate
) {

    override fun onMessage(message: ReqT) {
        try {
            super.onMessage(message)
        } catch (error: Throwable) {
            exceptionHandler.handlerError(serverCall, error)
        }
    }

    override fun onHalfClose() {
        try {
            super.onHalfClose()
        } catch (error: Throwable) {
            exceptionHandler.handlerError(serverCall, error)
        }
    }
}
