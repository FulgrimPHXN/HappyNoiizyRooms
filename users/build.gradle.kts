dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    //сама реализация registry, которая превращает уже собранные Micrometer-метрики в формат,
    //понятный Prometheus — Spring Boot подхватывает её по наличию в classpath, в коде на неё не ссылаемся
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}
//управления версиями в /graqdle/libs.versions