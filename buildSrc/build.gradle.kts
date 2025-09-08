plugins {
    kotlin("jvm") version "2.2.0"
    `kotlin-dsl`
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)

}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
    google()

}
kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(libs.openApiGenerator)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.jgit)


    implementation(libs.kotlin.gradle.plugin.api)
    implementation(libs.kotlin.gradle.plugin.model)

    implementation(gradleApi())
    implementation(localGroovy())
    testImplementation(libs.jupiter.api)
    testImplementation(libs.jupiter.engine)
    testImplementation(libs.jupiter.launcher)
}
