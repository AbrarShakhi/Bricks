pluginManagement {
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
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.AbrarShakhi")
            }
        }
    }
}

rootProject.name = "Bricks"
include(":app")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":core:ui")
include(":feature:home")
include(":feature:drawer")
include(":feature:settings")
include(":feature:widgets")
