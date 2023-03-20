plugins {
    kotlin("jvm") version "1.7.21"
}

val findProject = findProject(":native-udt")!!


tasks.build {
    val buildTask = findProject(":native-udt")?.tasks?.build
    dependsOn(buildTask)
}

findProject.afterEvaluate {
    this.tasks.whenTaskAdded {
        if (this.name.startsWith("linkDebug")) {
            findProject(":classes-udt")!!.tasks.findByName("test")!!.dependsOn(this)
        }
    }
}


dependencies {
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.2.10")
    implementation("io.netty:netty-common:4.1.84.Final")
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
tasks.test {
    useJUnitPlatform()

    doFirst {
        val library = findProject.extensions.getByName("library") as org.gradle.language.cpp.CppLibrary
        val sharedLib = library.developmentBinary.get() as CppSharedLibrary
        systemProperty("java.library.path", sharedLib.linkFile.get().asFile.parentFile)
    }
}

tasks.jar {
    val library = findProject.extensions.getByName("library") as org.gradle.language.cpp.CppLibrary
    from(library.developmentBinary.flatMap { (it as CppSharedLibrary).linkFile })
}
