plugins {
    //Объявляет версии плагинов Spring Boot и dependency-management один раз в корне, но не применяет их к самому корневому проекту
    //apply false = "знай версию, но не включай здесь". Реально применяются они ниже, в subprojects.
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    //Блок, который выполняется и для корня, и для каждого будущего модуля (:users, :items и т.д.). group/version и репозиторий Maven Central задаются один раз вместо копирования в каждый модуль.
    group = "org.DiceOrDieAss"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    //java — каждый модуль компилируется как Java-проект.
    apply(plugin = "java")
    //пара плагинов, которая вместе даёт "управляемые версии": как только оба применены в модуле, Spring сам подставляет совместимые версии зависимостей из своего BOM, не нужно писать версию руками.
    //noinspection AvoidApplyPluginMethod
    apply(plugin = "org.springframework.boot")
    //noinspection AvoidApplyPluginMethod
    apply(plugin = "io.spring.dependency-management")

    the<JavaPluginExtension>().toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }

    dependencies {
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")

        "testImplementation"(platform("org.junit:junit-bom:6.0.0"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
    }
}
