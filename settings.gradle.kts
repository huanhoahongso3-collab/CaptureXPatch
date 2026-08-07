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
        // Hosts the legacy de.robv.android.xposed:api artifact (compileOnly stub for the
        // legacy IXposedHookLoadPackage entry point), maintained by rovo89 since jcenter's
        // shutdown removed the old distribution channel.
        maven("https://api.xposed.info/") {
            content {
                includeGroup("de.robv.android.xposed")
            }
        }
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
 