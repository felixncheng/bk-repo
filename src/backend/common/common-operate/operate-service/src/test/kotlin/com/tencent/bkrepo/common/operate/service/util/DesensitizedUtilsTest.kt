/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.common.operate.service.util

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.operate.api.annotation.Sensitive
import com.tencent.bkrepo.common.operate.api.handler.MaskString
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class DesensitizedUtilsTest {

    @Test
    fun testDesensitizeObject() {
        assert(DesensitizedUtils.desensitizeObject(create()) as Child)
    }

    @Test
    fun testConvertMethodArgsToMap() {
        val method1 = DesensitizedUtilsTest::class.java.getDeclaredMethod("testMethod1", Child::class.java)
        val desensitizedMap = DesensitizedUtils.convertMethodArgsToMap(method1, arrayOf(create()), true)
        Assertions.assertNull(desensitizedMap["child"])

        val method2 = DesensitizedUtilsTest::class.java.getDeclaredMethod("testMethod2", Child::class.java)
        val desensitizedMap2 = DesensitizedUtils.convertMethodArgsToMap(method2, arrayOf(create()), true)
        assert(desensitizedMap2["child"] as Child)

        val desensitizedMap3 = DesensitizedUtils.convertMethodArgsToMap(method1, arrayOf(create()), false)
        val child = desensitizedMap3["child"] as Child
        Assertions.assertEquals(child.nickName, "mk")
        Assertions.assertEquals(child.age, 100)
        Assertions.assertEquals(child.password, "pwd-666666")
        Assertions.assertEquals(child.card.cardPassword, "card-pwd-123-1")
    }

    @Test
    fun testToString() {
        val result = "[normal=normal, password=******, nullPassword=null, emptyPassword=******, elements=[123, 456]]"
        Assertions.assertEquals(DesensitizedUtils.toString(TestDataClass()), "TestDataClass=${result}")
        Assertions.assertEquals(DesensitizedUtils.toString(TestNormalClass()), "TestNormalClass=${result}")
    }

    @Suppress("UnusedPrivateMember")
    private fun testMethod1(@Sensitive child: Child) {
        // do nothing
    }

    @Suppress("UnusedPrivateMember")
    private fun testMethod2(child: Child) {
        // do nothing
    }

    private fun assert(desensitizedChild: Child) {
        Assertions.assertNull(desensitizedChild.password)
        Assertions.assertEquals(desensitizedChild.age, 0)
        desensitizedChild.credentials.forEach { Assertions.assertNull(it) }
        Assertions.assertNull(desensitizedChild.card)
    }

    private fun create(): Child {
        val credentials = listOf(
            Credential("appId-12345-1", "secret-88888-1"),
            Credential("appId-12345-2", "secret-88888-2"),
            Credential("appId-12345-3", "secret-88888-3"),
            Credential("appId-12345-4", "secret-88888-4")
        )
        return Child("mk", "pwd-666666", credentials, ChildCard("card-pwd-123-1"))
    }
}

open class Parent(
    val name: String,
    @field:Sensitive
    val age: Int
)

data class Child(
    val nickName: String,
    @field:Sensitive
    val password: String,
    val credentials: List<Credential>,
    val card: ChildCard
) : Parent("mike", 100)

@Sensitive
data class Credential(
    val appId: String,
    val secret: String,
)

@Sensitive
open class Card(
    val id: String
)

class ChildCard(
    @field:Sensitive
    val cardPassword: String
) : Card("id-ccc-1")

data class TestDataClass(
    val normal: String = "normal",
    @field:Sensitive(MaskString::class)
    val password: String = "123456",
    @field:Sensitive(MaskString::class)
    val nullPassword: String? = null,
    @field:Sensitive(MaskString::class)
    val emptyPassword: String = StringPool.EMPTY,
    val elements: List<String> = listOf("123", "456")
)

class TestNormalClass(
    val normal: String = "normal",
    @field:Sensitive(MaskString::class)
    val password: String = "123456",
    @field:Sensitive(MaskString::class)
    val nullPassword: String? = null,
    @field:Sensitive(MaskString::class)
    val emptyPassword: String = StringPool.EMPTY,
    val elements: List<String> = listOf("123", "456")
)
