import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = rootProject.group
version = rootProject.version
description = "Plex-API"

repositories {
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
}

tasks.getByName<Jar>("jar") {
    archiveBaseName.set("Plex")
    archiveClassifier.set("proxy")
    val props = mapOf("version" to rootProject.version)
//    filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to props)
}

tasks.getByName<ShadowJar>("shadowJar") {
    val props = mapOf("version" to rootProject.version)
//    filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to props)
}

tasks {
    jar {
        finalizedBy(rootProject.tasks["copyJars"])
    }

    shadowJar {
        enabled = false
    }
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