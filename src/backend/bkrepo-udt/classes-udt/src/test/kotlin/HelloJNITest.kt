import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class HelloJNITest {
    @Test
    fun test() {
        Assertions.assertDoesNotThrow{ HelloJNI().sayHello("127.0.0.1", 9000)}
    }
}