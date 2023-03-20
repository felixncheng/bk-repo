import com.tencent.bkrepo.net.udt.SocketType
import com.tencent.bkrepo.net.udt.UDTSocket
import java.net.InetAddress

class Sample

fun main() {
   val udtSocket = UDTSocket(SocketType.STREAM)
   udtSocket.connect(InetAddress.getByName("127.0.0.1"), 9000)
}