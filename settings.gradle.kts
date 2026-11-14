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

rootProject.name = "Miaopu"
include(":app")

// Miuix's development snapshots require GitHub Packages credentials. Build the
// pinned upstream commit from source so local and CI builds use the same APIs.
includeBuild("vendor/miuix") {
    dependencySubstitution {
        substitute(module("top.yukonga.miuix.kmp:miuix-ui")).using(project(":miuix-ui"))
        substitute(module("top.yukonga.miuix.kmp:miuix-nav")).using(project(":miuix-nav"))
        substitute(module("top.yukonga.miuix.kmp:miuix-glass")).using(project(":miuix-glass"))
    }
}
