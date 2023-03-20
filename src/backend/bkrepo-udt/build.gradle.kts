plugins {
    base
}

subprojects {
    repositories {
        if (System.getenv("GITHUB_WORKFLOW") == null) {
            maven(url = "https://mirrors.tencent.com/nexus/repository/maven-public")
        } else {
            mavenCentral()
            gradlePluginPortal()
        }
    }
}
