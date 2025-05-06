import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(libs.kotlin.telegram.bot)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.datetime)
    implementation(libs.sqlite.jdbc)
    implementation(libs.slf4j.simple)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass = "wedbot.AppKt"
}

val localProperties = Properties().apply {
    file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

tasks.withType<JavaExec> {
    systemProperty("bot.token", localProperties.getProperty("telegram.bot.token"))
    systemProperty("debug", localProperties.getProperty("debug"))
}

tasks.named<Test>("test") {
    useJUnitPlatform() 
}
