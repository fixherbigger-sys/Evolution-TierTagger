plugins {
    id("multiloader-base")
    id("java-library")

    id("net.fabricmc.fabric-loom-remap")
    id("io.freefair.lombok") version "9.5.0"
}

dependencies {
    minecraft("com.mojang:minecraft:${BuildConfig.MINECRAFT_VERSION}")
    // Official Mojang mappings, NOT Yarn -- even though 1.21.11 is still
    // obfuscated (hence still needing fabric-loom-remap), Loom can remap
    // straight to Mojang's official names instead of Yarn's. Since Mojang
    // names are stable across versions (that's the whole point of them),
    // this lets the rest of this codebase -- written with Mojang-mapped
    // names throughout -- compile as-is, instead of needing a full port to
    // Yarn's different class names.
    mappings(loom.officialMojangMappings())

    // "mod" variant (not plain compileOnly) is required here: ukulib's
    // compiled classes reference Minecraft types by their obfuscated/
    // intermediary names, and Loom only remaps those to match our chosen
    // mappings (official Mojang) for dependencies declared via the mod*
    // configurations. Plain compileOnly worked fine in the original 26.2
    // template because that version is unobfuscated -- there's nothing to
    // remap there. 1.21.11 still needs the distinction.
    modCompileOnly("net.uku3lig:ukulib:${BuildConfig.UKULIB_VERSION}")

    // provided both by fabric and neoforge
    compileOnly("net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7")
    compileOnly("io.github.llamalad7:mixinextras-common:0.5.4")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.4")
}

tasks.jar { enabled = false }
// Same reasoning as disabling `jar` above: :common isn't a standalone
// distributable (its classes get compiled directly into :fabric's jar),
// so there's nothing here for fabric-loom-remap to remap. Without this,
// remapJar fails looking for a dev jar that jar{} was never going to produce.
tasks.named("remapJar") { enabled = false }