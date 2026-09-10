rootProject.name = "evolutiontiers-tagger"

pluginManagement {
    repositories {
        maven { url = uri("https://maven.fabricmc.net/") }
        gradlePluginPortal()
    }
}

// NeoForge module excluded: this fork only builds Fabric (see BuildConfig.kt).
// The neoforge/ folder and its source are left untouched in case you want to
// re-enable it later -- just add "neoforge" back to this include() line and
// restore BuildConfig.NEOFORGE_VERSION plus the neoforge maven repo above.
include("common", "fabric")