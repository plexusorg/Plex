plugins {
    id("java")
    id("maven-publish")
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.7"
    id("net.kyori.blossom") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.plex"
version = "1.4-SNAPSHOT"
description = "Plex"

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.gradle.plugin.idea-ext")
    apply(plugin = "net.kyori.blossom")
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories {
        maven {
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }

        maven {
            url = uri("https://repository.apache.org/content/repositories/snapshots/")
        }

        maven {
            url = uri("https://repo.maven.apache.org/maven2/")
        }

        maven {
            url = uri("https://jitpack.io")
            content {
                includeGroup("com.github.MilkBowl")
            }
        }

        mavenCentral()
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    tasks {
        compileJava {
            options.encoding = Charsets.UTF_8.name()
        }
        javadoc {
            options.encoding = Charsets.UTF_8.name()
        }
        processResources {
            filteringCharset = Charsets.UTF_8.name()
        }
    }

    publishing {
        repositories {
            maven {
                val releasesRepoUrl = uri("https://nexus.telesphoreo.me/repository/plex-releases/")
                val snapshotsRepoUrl = uri("https://nexus.telesphoreo.me/repository/plex-snapshots/")
                url = if (rootProject.version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                credentials {
                    username = System.getenv("plexUser")
                    password = System.getenv("plexPassword")
                }
            }
        }
    }
}

tasks.clean {
    // Is there any way to not need to specify each subproject?
    dependsOn(":server:clean")
    dependsOn(":proxy:clean")
}

tasks.create<Copy>("copyJars") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // Is there any way to not need to specify each subproject?
    from(tasks.getByPath(":server:shadowJar"))
    from(tasks.getByPath(":proxy:jar"))
    into(file("build/libs"))
}