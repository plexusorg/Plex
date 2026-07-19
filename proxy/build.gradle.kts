plugins {
    java
    id("org.jetbrains.gradle.plugin.idea-ext")
    id("net.kyori.blossom")
    id("com.gradleup.shadow")
}

group = rootProject.group
version = rootProject.version
description = "Plex-Velocity"

repositories {
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

tasks.getByName<Jar>("jar") {
    archiveBaseName.set("Plex-Velocity")
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    jar {
        enabled = false
    }

    shadowJar {
        archiveBaseName.set("Plex-Velocity")
        archiveClassifier.set("")
        finalizedBy(rootProject.tasks["copyJars"])
    }
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", project.version.toString())
            }
        }
    }
}

dependencies {
    implementation(project(":api"))
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    compileOnly("org.json:json:20260522")
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
}
