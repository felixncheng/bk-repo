import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files

class HelloJNI {

    external fun sayHello(host: String, port: Int)

    companion object {
        init {
            try {
                System.loadLibrary("native-udt")
            } catch (e: UnsatisfiedLinkError) {
                val libName = "native-udt"
                val url = HelloJNI::class.java.classLoader.getResource(libFilename(libName))
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
}