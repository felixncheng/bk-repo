plugins {
    kotlin("jvm") version "1.7.21"
    application
}

application {
    mainClass.set("SampleKt")
}

dependencies{
    implementation(project(":classes-udt"))
}