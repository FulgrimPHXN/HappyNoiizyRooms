val springCloudVersion = "2025.1.2"

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    // Как и в :users — сами по себе метрики по маршруту (gateway.requests с тегами
    // routeId/routeUri/httpStatusCode) появляются автоматически, как только Actuator
    // + Micrometer видны в classpath (GatewayMetricsFilter — встроенный, не наш код).
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}
