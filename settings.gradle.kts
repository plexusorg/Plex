pluginManagement {
    repositories {
        maven("https://telesphorepo.com/maven/gradle-plugins-releases/")
        gradlePluginPortal()
    }
}

rootProject.name = "Plex"
include("api", "server", "proxy")