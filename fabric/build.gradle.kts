plugins {
    id("multiloader-platform")
    id("net.fabricmc.fabric-loom-remap")
}

loom {
    // Loom 1.14's (fabric-loom-remap, used for this obfuscated 1.21.x
    // generation) run-config DSL doesn't have displayName/runDirectory/
    // appendProjectPathToDisplayName/generateRunConfig -- those are newer
    // fabric-loom (unobfuscated 26.x) additions the original template used.
    // The bare client() call is all that's actually needed to build; this
    // only affects the convenience "run client" IDE task, not the mod itself.
    runs.named("client") {
        client()
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${BuildConfig.MINECRAFT_VERSION}")
    mappings(loom.officialMojangMappings())
    implementation("net.fabricmc:fabric-loader:${BuildConfig.FABRIC_LOADER_VERSION}")

    // Same "mod" reasoning as common/build.gradle.kts: fabric-command-api-v2
    // and ukulib both reference Minecraft types and need remapping for this
    // obfuscated version, which only the mod* configurations trigger.
    modImplementation(fabricApi.module("fabric-command-api-v2", BuildConfig.FABRIC_API_VERSION))

    modApi("net.uku3lig:ukulib:${BuildConfig.UKULIB_VERSION}")
}

modrinth {
    loaders.add("quilt")
}