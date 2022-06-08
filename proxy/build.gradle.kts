plugins {
    id("net.kyori.blossom") version "1.3.0"
}

group = rootProject.group
version = rootProject.version
description = "Plex-Velocity"

repositories {
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
}

tasks.getByName<Jar>("jar") {
    archiveBaseName.set("Plex-Velocity")
}

tasks {
    jar {
        finalizedBy(rootProject.tasks["copyJars"])
    }

    shadowJar {
        enabled = false
    }
}

blossom {
    replaceToken("@version@", rootProject.version)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    compileOnly("org.json:json:20220320")
    compileOnly("com.velocitypowered:velocity-api:3.1.2-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.2-SNAPSHOT")
    implementation(project(":api"))
}