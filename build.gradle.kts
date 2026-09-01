plugins {
    kotlin("jvm") version "2.4.10" apply false
    id("com.diffplug.spotless") version "8.10.1" apply false
}

allprojects {
    group = "dev.riseri"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}