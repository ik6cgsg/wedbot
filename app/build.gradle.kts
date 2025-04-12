plugins {
    alias(libs.plugins.kotlin.jvm) 
    application 
}

repositories {
    mavenCentral() 
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5") 
    testImplementation(libs.junit.jupiter) 
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(libs.guava) 
}

application {
    mainClass = "wedbot.AppKt" 
}

tasks.named<Test>("test") {
    useJUnitPlatform() 
}