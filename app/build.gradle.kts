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
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.network.tls.certificates)
    implementation(libs.ktor.server.call.logging)
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
    systemProperty("bot.token", localProperties.getProperty("bot.token"))
    systemProperty("bot.host", localProperties.getProperty("bot.host"))
    systemProperty("keystore.pswd", localProperties.getProperty("keystore.pswd"))
    systemProperty("debug", localProperties.getProperty("debug"))
}

tasks.named<Test>("test") {
    useJUnitPlatform() 
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "wedbot.AppKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
}
