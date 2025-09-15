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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ✅ Add PhonePe repository (Kotlin DSL)
        maven {
            url = uri("https://phonepe.mycloudrepo.io/public/repositories/phonepe-intentsdk-android")
        }
    }
}

rootProject.name = "The Doctor At Home user"
include(":app")
