package com.tencent.bkrepo.grpc.server.interceptor

import com.tencent.bkrepo.grpc.server.GrpcAccessLogServerCall
import com.tencent.bkrepo.grpc.server.annotation.GrpcGlobalInterceptor
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor

@GrpcGlobalInterceptor
class GrpcAccessLogInterceptor : ServerInterceptor {
    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val delegate = GrpcAccessLogServerCall(call)
        return next.startCall(delegate, headers)
    }
}
