plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.kyrixen:logger:0.1.1")
    implementation("com.google.code.gson:gson:2.13.1")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.web")
}

application {
    mainClass = "io.kyrixen.studio.Studio"
}