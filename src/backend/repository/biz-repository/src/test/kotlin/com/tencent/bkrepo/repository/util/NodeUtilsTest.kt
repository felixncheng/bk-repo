package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.repository.util.NodeUtils.ROOT_PATH
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("节点工具类测试")
class NodeUtilsTest {

    @Test
    fun testParseDirName() {
        assertEquals(ROOT_PATH, NodeUtils.parseFullPath("/"))
        assertEquals(ROOT_PATH, NodeUtils.parseFullPath("  /   "))
        assertEquals(ROOT_PATH, NodeUtils.parseFullPath("  "))
        assertEquals("/a", NodeUtils.parseFullPath("  /   a"))
        assertEquals(
            "/a/b",
            NodeUtils.parseFullPath("  /   a  /b")
        )
        assertEquals(
            "/a/b",
            NodeUtils.parseFullPath("  /   a  /b/")
        )

        assertDoesNotThrow { NodeUtils.parseFullPath("/1/2/3/4/5/6/7/8/9/10") }
        assertThrows<ErrorCodeException> {
            NodeUtils.parseFullPath(
                "/../"
            )
        }
        assertEquals(ROOT_PATH, NodeUtils.parseFullPath("/./"))
        assertEquals("/.1", NodeUtils.parseFullPath("/.1/"))
        assertEquals("/....", NodeUtils.parseFullPath("/..../"))
    }

    @Test
    fun testParseFileName() {
        assertEquals("abc", NodeUtils.validateFileName("abc"))
        assertEquals("中文测试", NodeUtils.validateFileName("中文测试"))
        assertEquals(
            "！@……&%#&¥*@#¥*（！——#！!@(#(!\$",
            NodeUtils.validateFileName("！@……&%#&¥*@#¥*（！——#！!@(#(!$")
        )
        assertThrows<ErrorCodeException> {
            NodeUtils.validateFileName(
                ""
            )
        }
        assertThrows<ErrorCodeException> {
            NodeUtils.validateFileName(
                "   "
            )
        }
        assertThrows<ErrorCodeException> {
            NodeUtils.validateFileName(
                ".."
            )
        }
        assertThrows<ErrorCodeException> {
            NodeUtils.validateFileName(
                "."
            )
        }
        assertThrows<ErrorCodeException> {
            NodeUtils.validateFileName(
                "dsjfkjafk/dsajdklsak"
            )
        }
    }

    @Test
    fun testCombineFullPath() {
        assertEquals("/a", NodeUtils.combineFullPath("", "a"))
        assertEquals("/a/b", NodeUtils.combineFullPath("/a", "b"))
        assertEquals(
            "/a/b",
            NodeUtils.combineFullPath("/a/", "b")
        )
    }

    @Test
    fun testGetParentPath() {
        assertEquals("/a/", NodeUtils.getParentPath("/a/b"))
        assertEquals("/a/", NodeUtils.getParentPath("/a/b.txt"))
        assertEquals("/a/b/", NodeUtils.getParentPath("/a/b/c/"))
        assertEquals("/", NodeUtils.getParentPath("/a"))
        assertEquals("/", NodeUtils.getParentPath("/"))
    }

    @Test
    fun testGetName() {
        assertEquals("b", NodeUtils.getName("/a/b"))
        assertEquals("b.txt", NodeUtils.getName("/a/b.txt"))
        assertEquals("", NodeUtils.getName("/"))
        assertEquals("c", NodeUtils.getName("/a/b/c/"))
    }

    @Test
    fun testEscapeRegex() {
        assertEquals("""\.\*""", NodeUtils.escapeRegex(".*"))
        assertEquals(
            """/\.\*\|\^/a/""",
            NodeUtils.escapeRegex("/.*|^/a/")
        )
    }

    @Test
    fun testFormatPath() {
        assertEquals("/.*|^/a/", NodeUtils.formatPath("/.*|^/a"))
        assertEquals(
            "/.*|^/a",
            NodeUtils.formatFullPath("/.*|^/a")
        )

        assertEquals("/a/b/c", NodeUtils.formatFullPath("./a/b/c"))
        assertEquals("/.a/b/c", NodeUtils.formatFullPath("./.a/./b/c/."))

        assertEquals("/a/b/", NodeUtils.formatPath("/a/b"))
        assertEquals("/a/b/", NodeUtils.formatPath("/a/b/"))
    }
}