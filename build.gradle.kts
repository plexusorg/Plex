plugins {
    id("java")
    id("maven-publish")
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.3" apply false
    id("net.kyori.blossom") version "2.2.0" apply false
    id("com.gradleup.shadow") version "9.3.0" apply false
}

group = "dev.plex"
version = "1.7-SNAPSHOT"
description = "Plex"

subprojects {
    repositories {
        maven {
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }

        maven {
            url = uri("https://repository.apache.org/content/repositories/snapshots/")
        }

        maven {
            url = uri("https://jitpack.io")
            content {
                includeGroup("com.github.MilkBowl")
                includeGroup("com.github.LeonMangler")
            }
        }

        mavenCentral()
    }

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(25))
        }
        tasks.withType<JavaCompile>().configureEach {
            options.encoding = Charsets.UTF_8.name()
        }
        tasks.withType<Javadoc>().configureEach {
            options.encoding = Charsets.UTF_8.name()
        }
        tasks.withType<ProcessResources>().configureEach {
            filteringCharset = Charsets.UTF_8.name()
        }
    }

    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "plex"
                    val releasesRepoUrl = uri("https://nexus.telesphoreo.me/repository/plex-releases/")
                    val snapshotsRepoUrl = uri("https://nexus.telesphoreo.me/repository/plex-snapshots/")
                    url = if (rootProject.version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                    credentials(PasswordCredentials::class)
                }
            }
        }
    }
}

tasks.clean {
    dependsOn(subprojects.map {
        it.project.tasks.clean
    })
}

tasks.register<Copy>("copyJars", fun Copy.() {
    dependsOn(tasks.jar)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    subprojects.forEach { sub ->
        from(sub.tasks.matching { it.name == "shadowJar" })
        from(sub.tasks.matching { it.name == "jar" })
    }
    into(file("build/libs"))
})