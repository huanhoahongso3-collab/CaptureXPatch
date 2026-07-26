pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage") dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal {
            content {
                includeGroup("io.github.libxposed")
            }
        }
    }

    versionCatalogs {
        create("libs")
    }
}

rootProject.name = "CaptureSposed"
include(":app")
 