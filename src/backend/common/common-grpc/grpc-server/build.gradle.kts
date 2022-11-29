dependencies {
    runtimeOnly("io.grpc:grpc-netty:${Versions.Grpc}")
    api("io.grpc:grpc-services:${Versions.Grpc}")
    api("org.springframework.boot:spring-boot-starter")

    testImplementation(kotlin("test-junit"))
    testImplementation("io.grpc:grpc-testing:${Versions.Grpc}")
}
