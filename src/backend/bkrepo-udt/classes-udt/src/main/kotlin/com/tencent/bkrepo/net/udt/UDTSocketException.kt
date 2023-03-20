package com.tencent.bkrepo.net.udt

import java.net.SocketException

class UDTSocketException(fd: Int, errorCode: Int, comment: String) : SocketException(comment)