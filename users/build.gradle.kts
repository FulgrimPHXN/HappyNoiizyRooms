dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation(libs.uuid.creator)
    implementation(libs.jjwt.api)
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    //сама реализация registry, которая превращает уже собранные Micrometer-метрики в формат,
    //понятный Prometheus — Spring Boot подхватывает её по наличию в classpath, в коде на неё не ссылаемся
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}
//управления версиями в /graqdle/libs.versions