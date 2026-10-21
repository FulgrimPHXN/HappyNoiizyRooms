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

    //JUnit5 сам по себе объявлен в корневом build.gradle.kts для всех модулей,
    //но этот стартер добавляет сверху Mockito, AssertJ и Spring-специфичные
    //тестовые срезы (MockMvc для @WebMvcTest, встроенная БД-инфраструктура
    //для @DataJpaTest и т.п.) — без него они недоступны.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    //MockMvc-постпроцессоры для подстановки Authentication в контекст запроса
    //(нужно для @WebMvcTest на UserController — его методы принимают Authentication напрямую)
    testImplementation("org.springframework.security:spring-security-test")
    //H2 — только для быстрых @DataJpaTest-срезов на этом шаге (U5 учебного
    //плана); реальный Postgres через Testcontainers — следующий шаг (U6),
    //намеренно отдельный повод, не заменять этим.
    testRuntimeOnly("com.h2database:h2")
}
//управления версиями в /graqdle/libs.versions