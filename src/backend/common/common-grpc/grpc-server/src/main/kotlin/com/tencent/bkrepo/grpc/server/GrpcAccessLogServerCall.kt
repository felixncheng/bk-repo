package com.tencent.bkrepo.grpc.server

import io.grpc.ForwardingServerCall
import io.grpc.Grpc
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.Status
import org.slf4j.LoggerFactory

class GrpcAccessLogServerCall<ReqT, RespT>(delegate: ServerCall<ReqT, RespT>?) : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(
    delegate
) {

    override fun close(status: Status, trailers: Metadata?) {
        val logMessage = builderLogMsg(this, status)
        logger.info(logMessage)
        super.close(status, trailers)
    }

    private fun builderLogMsg(call: ServerCall<*, *>, status: Status): String {
        val method = call.methodDescriptor.fullMethodName
        val remoteIp = call.attributes[Grpc.TRANSPORT_ATTR_REMOTE_ADDR].toString().removePrefix("/")
        val code = status.code
        return "[$method] $remoteIp $code"
    }

    companion object {
        private const val ACCESS_LOGGER_NAME = "AccessLogger"
        private val logger = LoggerFactory.getLogger(ACCESS_LOGGER_NAME)
    }
}
