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

rootProject.name = "ynab-receipt-scanner"

include(
    ":app",
    ":domain",
    ":data-local",
    ":data-ocr",
    ":data-parser",
    ":data-ynab",
    ":feature-auth",
    ":feature-history",
    ":feature-review",
    ":feature-scan"
)
