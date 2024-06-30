plugins {
    application
    java
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.lwjgl:lwjgl:3.2.3")
    implementation("org.lwjgl:lwjgl-glfw:3.2.3")
    implementation("org.lwjgl:lwjgl-opengl:3.2.3")
    runtimeOnly("org.lwjgl:lwjgl:3.2.3:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-glfw:3.2.3:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengl:3.2.3:natives-windows")
    implementation("org.joml:joml:1.10.5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("org.example.App")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
