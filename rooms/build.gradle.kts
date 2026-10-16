dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.uuid.creator)
    //нужен тот же набор jjwt-зависимостей, что в :users, хотя :rooms не выпускает токены —
    //проверка подписи (Jwts.parser()...) требует jjwt-impl в runtime так же, как и генерация
    implementation(libs.jjwt.api)

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
}
