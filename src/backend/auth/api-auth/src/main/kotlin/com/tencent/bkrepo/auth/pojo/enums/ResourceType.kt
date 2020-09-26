package com.tencent.bkrepo.auth.pojo.enums

enum class ResourceType {
    SYSTEM,
    PROJECT,
    REPO,
    NODE;

    fun id() = this.name.toLowerCase()
}
