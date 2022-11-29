package com.tencent.bkrepo.grpc.server.interceptor

import com.tencent.bkrepo.grpc.server.error.GrpcExceptionListener
import com.tencent.bkrepo.grpc.server.error.GrpcExceptionResponseHandler
import com.tencent.bkrepo.grpc.server.error.GrpcExceptionServerCall
import com.tencent.bkrepo.grpc.server.annotation.GrpcGlobalInterceptor
import com.tencent.bkrepo.grpc.server.error.GrpcGlobalExceptionHandler
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import org.springframework.context.annotation.Import

@GrpcGlobalInterceptor
@Import(GrpcGlobalExceptionHandler::class)
class GrpcExceptionInterceptor(private val exceptionHandler: GrpcExceptionResponseHandler) : ServerInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val handledCall = GrpcExceptionServerCall(call, exceptionHandler)
        val delegate = next.startCall(handledCall, headers)
        return GrpcExceptionListener(delegate, call, exceptionHandler)
    }
}
