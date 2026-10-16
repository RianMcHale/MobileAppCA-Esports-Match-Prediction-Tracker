// lists places where gradle can find build plugins.
pluginManagement {
    repositories {
        google {
            // limits this repository to android and google plugin groups.
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

// helps gradle locate a suitable java toolchain.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// lists places where app libraries can be downloaded.
dependencyResolutionManagement {
    // keeps repositories in this file instead of individual modules.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// names the overall project and includes its app module.
rootProject.name = "EsportsPredictionTracker"
include(":app")