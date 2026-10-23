rootProject.name = "MaxPlayerPro"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":androidApp")
include(":desktopApp")
include(":shared")
include(":webApp")

// Core KMP libraries (no app-specific code) — copied from MunafaPlus kmpcore, minus uidesign
include(":kmpcore:mvi")
include(":kmpcore:navigation")
include(":kmpcore:di")
include(":kmpcore:utils")
include(":kmpcore:permissions")
include(":kmpcore:storage:datastore")

// Reusable media player library — publishable on its own. core = engine, models, logic; ui = Compose player screens
include(":kmediaplayer:core")
include(":kmediaplayer:ui")

// App-level shared code
include(":appcore")
include(":appcore:appres")
include(":appcore:sharedlogic")

// One module per screen, each split into domain + presentation
include(":appfeatures:home:domain")
include(":appfeatures:home:presentation")
include(":appfeatures:folder:domain")
include(":appfeatures:folder:presentation")
include(":appfeatures:search:domain")
include(":appfeatures:search:presentation")
include(":appfeatures:music:domain")
include(":appfeatures:music:presentation")
include(":appfeatures:player:domain")
include(":appfeatures:player:presentation")
include(":appfeatures:settings:domain")
include(":appfeatures:settings:presentation")
include(":appfeatures:profile:domain")
include(":appfeatures:profile:presentation")
include(":appfeatures:theme:domain")
include(":appfeatures:theme:presentation")
include(":appfeatures:playlists:domain")
include(":appfeatures:playlists:presentation")
include(":appfeatures:privatefolder:domain")
include(":appfeatures:privatefolder:presentation")
include(":appfeatures:mediamanager:domain")
include(":appfeatures:mediamanager:presentation")
include(":appfeatures:help:domain")
include(":appfeatures:help:presentation")
include(":appfeatures:localnetwork:domain")
include(":appfeatures:localnetwork:presentation")
include(":appfeatures:gallery:domain")
include(":appfeatures:gallery:presentation")
