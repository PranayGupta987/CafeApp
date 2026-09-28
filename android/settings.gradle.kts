pluginManagement {
    repositories {
        google()
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

rootProject.name = "cafeapp"
include(":app")
include(":core:common")
include(":core:ui")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:realtime")
include(":feature:auth")
include(":feature:menu")
include(":feature:offers")
include(":feature:reservations")
include(":feature:session")
include(":feature:cart")
include(":feature:orders")
include(":feature:cafe")
include(":feature:kitchen")
