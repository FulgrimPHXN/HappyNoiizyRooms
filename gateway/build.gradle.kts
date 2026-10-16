val springCloudVersion = "2025.1.2"

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}
