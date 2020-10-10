import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"
    maven
}

group = "com.squaredcandy"
version = "0.0.4"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    jcenter()
}

dependencies {
    val europa = "0.0.6"
    val exposed = "0.25.1"
    val slf4j = "1.7.30"
    val h2 = "1.4.199"
    val pgjdbc = "0.8.3"
    val hikari = "3.4.5"
    val coroutine = "1.3.9"
    val truth = "1.0.1"
    val turbine = "0.2.1"

    implementation(kotlin("stdlib"))

    api("com.github.squaredcandy:Europa:$europa")

    implementation("org.slf4j:slf4j-nop:$slf4j")
    implementation("org.jetbrains.exposed:exposed-core:$exposed")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed")
    implementation("com.impossibl.pgjdbc-ng:pgjdbc-ng:$pgjdbc")
    implementation("com.zaxxer:HikariCP:$hikari")

    testImplementation("app.cash.turbine:turbine:$turbine")
    testImplementation("com.google.truth:truth:$truth")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutine")
    testImplementation("com.h2database:h2:$h2")
    testImplementation(kotlin("test-junit5"))
}
tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}