pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "YeonhyuGak"
include(":app")
include(":core:engine")
include(":core:data")
include(":core:ui")
include(":feature:calendar")
include(":feature:recommend")
include(":feature:leave")
include(":feature:settings")
include(":feature:alerts")
include(":feature:widget")
include(":feature:share")
