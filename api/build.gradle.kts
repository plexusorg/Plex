plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    compileOnlyApi("org.jdbi:jdbi3-core:3.54.0")
    api("com.google.code.gson:gson:2.14.0")
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    compileOnly("org.apache.logging.log4j:log4j-api:2.26.1")
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
