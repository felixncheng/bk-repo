package com.tencent.bkrepo.grpc.server.error

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.Status
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GrpcGlobalExceptionHandler : GrpcExceptionResponseHandler {
    override fun handlerError(serverCall: ServerCall<*, *>, error: Throwable) {
        try {
            logger.error("Grpc service error", error)
            val status = Status.fromThrowable(error).withDescription(error.message)
            val metadata = Status.trailersFromThrowable(error) ?: Metadata()
            serverCall.close(status, metadata)
        } catch (error: Throwable) {
            logger.error("Handler exception error", error)
            serverCall.close(Status.INTERNAL, Metadata())
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GrpcGlobalExceptionHandler::class.java)
    }
}
