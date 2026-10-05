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
    }
}

rootProject.name = "Localhost"
include(":app")
include(":core:common")
include(":core:data")
include(":core:model")
include(":core:security")
include(":core:designsystem")
include(":runtime:manager")
include(":runtime:process")
include(":runtime:templates")
include(":tunnel")
include(":server-dashboard")
include(":cli")
include(":feature:projects")
include(":feature:logs")
include(":feature:files")
include(":feature:monitor")
include(":feature:tunnel")
include(":feature:settings")
include(":feature:supabase")
