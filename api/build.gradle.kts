plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api("com.j256.ormlite:ormlite-core:6.1")
    api("com.google.code.gson:gson:2.13.2")

    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("org.apache.logging.log4j:log4j-api:2.26.0")
    compileOnly("org.jetbrains:annotations:26.1.0")
}

group = rootProject.group
version = rootProject.version
description = "Plex-API"

tasks.getByName<Jar>("jar") {
    archiveBaseName.set("Plex-API")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
