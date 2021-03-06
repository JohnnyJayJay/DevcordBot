
plugins {
    java
    application
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

group = "com.github.seliba"
version = "2.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx")
}

dependencies {
    implementation(project(":"))

    // Logging
    implementation("org.slf4j", "slf4j-api", "2.0.0-alpha1")
    implementation("org.slf4j", "slf4j-simple", "2.0.0-alpha1")

    // Database
    implementation("org.jetbrains.exposed", "exposed-core", "0.21.1")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.21.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.21.1")
    implementation("org.jetbrains.exposed", "exposed-java-time", "0.21.1")
    implementation("org.postgresql", "postgresql", "42.2.10")
    implementation("com.zaxxer", "HikariCP", "3.4.2")

    // util
    implementation("io.github.cdimascio", "java-dotenv", "5.1.3")
}

application {
    mainClassName = "com.github.seliba.devcordbot.LauncherKt"
}

tasks {
    compileTestKotlin {
        kotlinOptions.jvmTarget = "12"
    }

    jar {
        archiveClassifier.set("original")
    }
}


configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_13
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "12"
    }
}