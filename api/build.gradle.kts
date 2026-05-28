plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    compileOnlyApi("org.jdbi:jdbi3-core:3.53.0")
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
