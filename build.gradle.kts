plugins {
    java
    `maven-publish`
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }

    maven {
        url = uri("https://repository.apache.org/content/repositories/snapshots/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    implementation("org.json:json:20211205")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.maven.plugins:maven-shade-plugin:3.3.0-SNAPSHOT")
    implementation("dev.morphia.morphia:morphia-core:2.2.3")
    implementation("redis.clients:jedis:4.0.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:2.7.3")
    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")
    compileOnly("io.papermc.paper:paper-api:1.18.1-R0.1-SNAPSHOT")
}

group = "dev.plex"
version = "1.0"
description = "Plex"
java.sourceCompatibility = JavaVersion.VERSION_17

bukkit {
    name = "Plex"
    version = rootProject.version.toString()
    description = "Plex provides a new experience for freedom servers"
    authors = listOf("Telesphoreo", "taahanis", "super")
    main = "dev.plex.Plex"
    website = "https://telesphoreo.me"
    apiVersion = "1.18"
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}
