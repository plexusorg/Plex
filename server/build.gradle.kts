import net.minecrell.pluginyml.paper.PaperPluginDescription
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("net.kyori.indra.git") version "3.1.3"
    id("de.eldoria.plugin-yml.paper") version "0.7.1"
}

repositories {
    maven(url = uri("https://maven.playpro.com"))
    maven(url = uri("https://nexus.telesphoreo.me/repository/plex-modules/"))
}

dependencies {
    library("org.projectlombok:lombok:1.18.38")
    library("org.json:json:20250107")
    library("commons-io:commons-io:2.19.0")
    library("redis.clients:jedis:6.0.0-beta2")
    library("org.mariadb.jdbc:mariadb-java-client:3.5.3")
    library("com.zaxxer:HikariCP:6.3.0")
    library("org.apache.maven.resolver:maven-resolver-transport-http:1.9.22")
    library("org.jetbrains:annotations:26.0.2")
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude("org.bukkit", "bukkit")
    }
    compileOnly("net.coreprotect:coreprotect:22.4")
    compileOnly("network.darkhelmet.prism:Prism-Api:1.0.0")

    implementation("org.bstats:bstats-base:3.1.0")
    implementation("org.bstats:bstats-bukkit:3.1.0")

    implementation("com.github.LeonMangler:SuperVanish:6.2.18-3")

    annotationProcessor("org.projectlombok:lombok:1.18.38")
}

group = rootProject.group
version = rootProject.version
description = "Plex-Server"

paper {
    name = "Plex"
    version = rootProject.version.toString()
    description = "Plex provides a new experience for freedom servers."
    main = "dev.plex.Plex"
    loader = "dev.plex.PlexLibraryManager"
    website = "https://plex.us.org"
    authors = listOf("Telesphoreo", "taahanis", "supernt")
    apiVersion = "1.20.5"
    foliaSupported = true
    generateLibrariesJson = true
    // Load BukkitTelnet and LibsDisguises before Plex so the modules register properly
    serverDependencies {
        register("BukkitTelnet") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("Essentials") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("LibsDisguises") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("Prism") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("CoreProtect") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("PremiumVanish") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("SlimeWorldManager") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.AFTER
        }
        register("SuperVanish") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("Vault") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }
}

fun getBuildNumber(): String {
    val stdout = ByteArrayOutputStream()
    try {
        exec {
            commandLine("git", "rev-list", "HEAD", "--count")
            standardOutput = stdout
            isIgnoreExitValue = true
        }
    } catch (e: GradleException) {
        logger.error("Couldn't determine build number because Git is not installed. " + e.message)
    }
    return if (stdout.size() > 0) stdout.toString().trim() else "unknown"
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    jar {
        enabled = false
    }

    sourceSets {
        main {
            blossom {
                resources {
                    property("author", if (System.getenv("JENKINS_URL") != null) "jenkins" else System.getProperty("user.name"))
                    property("buildNumber", if (System.getenv("BUILD_NUMBER") != null) System.getenv("BUILD_NUMBER") else getBuildNumber())
                    property("date", SimpleDateFormat("MM/dd/yyyy '<light_purple>at<gold>' hh:mm:ss a z").format(Date()))
                    property("gitCommit", indraGit.commit()?.name?.take(7))
                }
            }
        }
    }

    shadowJar {
        archiveBaseName.set("Plex")
        archiveClassifier.set("")
        relocate("org.bstats", "dev.plex")
        finalizedBy(rootProject.tasks["copyJars"])
    }

    javadoc {
        options.memberLevel = JavadocMemberLevel.PRIVATE
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom.withXml {
                val dependenciesNode = asNode().appendNode("dependencies")
                configurations.getByName("library").allDependencies.configureEach {
                    dependenciesNode.appendNode("dependency")
                            .appendNode("groupId", group).parent()
                            .appendNode("artifactId", name).parent()
                            .appendNode("version", version).parent()
                            .appendNode("scope", "provided").parent()
                }
                configurations.getByName("implementation").allDependencies.configureEach {
                    dependenciesNode.appendNode("dependency")
                            .appendNode("groupId", group).parent()
                            .appendNode("artifactId", name).parent()
                            .appendNode("version", version).parent()
                            .appendNode("scope", "provided").parent()
                }
            }
            artifacts.artifact(tasks.shadowJar)
        }
    }
}