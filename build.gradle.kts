plugins {
    kotlin("jvm") version "2.4.21"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "net.azisaba"
version = "1.1.1+1.15.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc-repo" }
    maven("https://repo.azisaba.net/repository/maven-public/") { name = "azisaba-repo" }
    maven("https://maven.azisaba.net/releases") { name = "azisabaRepo" }
    maven("https://nexus.neetgames.com/repository/maven-releases/") { name = "neetgames-repo" }
    maven("https://maven.enginehub.org/repo/") { name = "enginehub" }
}

dependencies {
    compileOnly("com.destroystokyo.paper:paper-api:1.15.2-R0.1-SNAPSHOT")
    compileOnly("xyz.acrylicstyle:StorageBox:1.5.6+1.15.2")
    compileOnly("net.azisaba:ItemStash:2.2.8") {
        isTransitive = false
    }
    compileOnly("com.gmail.nossr50.mcMMO:mcMMO:2.1.231") {
        isTransitive = false
    }
}

kotlin {
    jvmToolchain(8)
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("kotlin", "net.azisaba.fastfurnace.libs.kotlin")
        minimize()
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }
}
