package com.tencent.bkrepo.net.udt

import HelloJNI
import java.io.IOException
import java.io.UncheckedIOException
import java.net.InetAddress
import java.nio.file.Files

class UDTSocket(type: SocketType) {

    companion object {
        init {
            try {
                System.loadLibrary("native-udt")
            } catch (e: UnsatisfiedLinkError) {
                val libName = "native-udt"
                val url = UDTSocket::class.java.classLoader.getResource(libFilename(libName))
                try {
                    val file = Files.createTempFile("jni", "udt").toFile()
                    file.deleteOnExit()
                    file.delete()
                    url?.openStream()?.use {
                        Files.copy(it, file.toPath())
                        System.load(file.canonicalPath)
                    }
                } catch (e: IOException) {
                    throw UncheckedIOException(e)
                }
            }
        }

        private fun libFilename(libName: String): String {
            return "lib$libName.so"
        }
    }
    
    private val fd: Int

    init {
        val isStream = type == SocketType.STREAM
        fd = socketCreate(isStream)
    }

    fun connect(address: InetAddress, port: Int) {
        connect0(fd, address, port)
    }

    private external fun socketCreate(stream: Boolean): Int
    private external fun connect0(fd: Int, address: InetAddress, port: Int)
    private external fun socketBind()
    private external fun socketListen()
    private external fun socketAccept()
    private external fun socketAvailable()
    private external fun socketClose0()
    private external fun socketShutdown()
    private external fun socketSetOption()
    private external fun socketGetOption()
}
