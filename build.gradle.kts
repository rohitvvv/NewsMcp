plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    application
    id("com.gradleup.shadow") version "9.4.1"
}

repositories {
    mavenCentral()
}

dependencies {
    val mcpVersion = "0.12.0"
    val ktorVersion = "3.0.1"

    implementation("io.modelcontextprotocol:kotlin-sdk-server:$mcpVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.9.0")
    implementation("com.squareup.okio:okio:3.9.0")
    
    // Explicitly add kotlin-logging to ensure it's in the classpath
    implementation("io.github.oshai:kotlin-logging:8.0.01")
    
    // SLF4J implementation to redirect logs to stderr
    implementation("org.slf4j:slf4j-simple:2.0.17")
    
    implementation("org.jsoup:jsoup:1.18.3")
    
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.example.newsmcp.MainKt")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile> {
    targetCompatibility = "17"
    sourceCompatibility = "17"
}

tasks.shadowJar {
    archiveBaseName.set("news-mcp")
    archiveClassifier.set("all")
    archiveVersion.set("1.0.0")
}
