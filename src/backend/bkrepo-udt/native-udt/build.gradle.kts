import org.gradle.internal.jvm.Jvm
import org.gradle.language.cpp.internal.DefaultCppSharedLibrary

plugins {
    `cpp-library`
}

library {
    binaries.configureEach {
        compileTask.get().compilerArgs.addAll(compileTask.get().targetPlatform.map {
            listOf("-I", "${Jvm.current().javaHome.canonicalPath}/include") + when {
                it.operatingSystem.isMacOsX -> listOf("-I", "${Jvm.current().javaHome.canonicalPath}/include/darwin")
                it.operatingSystem.isLinux -> listOf("-I", "${Jvm.current().javaHome.canonicalPath}/include/linux")
                else -> emptyList()
            }
        })
        (this as DefaultCppSharedLibrary).linkTask.get().linkerArgs.addAll("-lstdc++", "-ludt")
    }
}
