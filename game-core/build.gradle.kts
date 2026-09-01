plugins {
    kotlin("jvm")

    id("com.diffplug.spotless")
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

spotless {
    kotlin {
        ktlint()
    }

    kotlinGradle {
        ktlint()
    }
}

tasks.test {
    useJUnitPlatform()
}
