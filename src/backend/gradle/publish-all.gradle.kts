/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
val allJarRepo: String? by project
val allJarUsername: String? by project
val allJarPassword: String? by project

allprojects {
    if (!name.startsWith("boot-") && !name.startsWith("biz-")) {
        project.logger.lifecycle(allJarUsername)
        project.logger.lifecycle(allJarPassword)
        apply(plugin = "com.tencent.devops.publish")
        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "allJar"
                    url = uri(allJarRepo.orEmpty())
                    credentials {
                        username = allJarUsername
                        password = allJarPassword
                    }
                }
            }
            publications.withType<MavenPublication> {
                pom {
                    name.set(project.name)
                    description.set(project.description ?: project.name)
                    url.set("https://github.com/Tencent/bk-ci")
                    licenses {
                        license {
                            name.set("The MIT License (MIT)")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            name.set("bk-ci")
                            email.set("devops@tencent.com")
                            url.set("https://bk.tencent.com")
                            roles.set(listOf("Manager"))
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/Tencent/bk-ci.get")
                        developerConnection.set("scm:git:ssh://github.com/Tencent/bk-ci.git")
                        url.set("https://github.com/Tencent/bk-ci")
                    }
                }
            }
        }
    }
}
