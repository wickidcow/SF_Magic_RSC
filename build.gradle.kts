plugins {
    java
    id("com.gradleup.shadow") version "9.3.2"
}

val slimefunLegacyVersion = "4.1.58"

repositories {
    exclusiveContent {
        forRepository {
            ivy {
                name = "slimefunLegacyRelease"
                url = uri("https://github.com/wickidcow/Slimefun-Legacy/releases/download/v$slimefunLegacyVersion")
                patternLayout {
                    artifact("[artifact][revision].[ext]")
                }
                metadataSources {
                    artifact()
                }
            }
        }
        filter {
            includeModule("com.github.wickidcow", "Slimefun-Legacy")
        }
    }

    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val platformVersion = providers.gradleProperty("platformVersion").orElse("1.21.11-R0.1-SNAPSHOT")
val releaseJvm = providers.gradleProperty("releaseJvm").orElse("21").get().toInt()
val runtimeDir = layout.buildDirectory.dir("generated/magic-runtime/Magic")

dependencies {
    compileOnly("io.papermc.paper:paper-api:${platformVersion.get()}")
    compileOnly("com.github.wickidcow:Slimefun-Legacy:$slimefunLegacyVersion")
}

group = "io.github.wickidcow"
version = "2.0.37"
description = "Magic Legacy managed runtime bridge and migration host for Slimefun Legacy"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    sourceCompatibility = JavaVersion.toVersion(releaseJvm)
    targetCompatibility = JavaVersion.toVersion(releaseJvm)
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(releaseJvm)
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val prepareMagicRuntime by tasks.registering(Exec::class) {
    inputs.files(
        fileTree(projectDir) { include("*.yml") },
        fileTree("saveditems") { include("**/*.yml", "**/*.yaml") },
        fileTree("scripts") { include("**/*.js") },
        fileTree("tools") { include("apply_runtime_fixes.py", "prepare_plugin_runtime.py") }
    )
    outputs.dir(runtimeDir)
    commandLine(
        "python3",
        "tools/prepare_plugin_runtime.py",
        runtimeDir.get().asFile.absolutePath,
        project.version.toString()
    )
}

tasks.processResources {
    dependsOn(prepareMagicRuntime)
    filesMatching("plugin.yml") {
        expand(mapOf("version" to project.version))
    }
    from(runtimeDir) {
        into("embedded/Magic")
    }
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("SF_MagicLegacy${project.version}.jar")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.register("verifyMagicJar") {
    dependsOn(tasks.shadowJar)
    doLast {
        val jar = tasks.shadowJar.get().archiveFile.get().asFile
        require(jar.isFile && jar.length() > 0) { "Magic Legacy JAR was not created" }
        val contents = zipTree(jar)
        val required = listOf(
            "plugin.yml",
            "embedded/Magic/info.yml",
            "embedded/Magic/items.yml",
            "embedded/Magic/recipe_machines.yml"
        )
        required.forEach { path ->
            require(!contents.matching { include(path) }.isEmpty) {
                "Missing required JAR entry: $path"
            }
        }

        require(contents.matching { include("embedded/Magic/scripts/服务器.js") }.isEmpty) {
            "Migrated global listener script must not be packaged: embedded/Magic/scripts/服务器.js"
        }
    }
}

tasks.build {
    dependsOn(tasks.shadowJar, "verifyMagicJar")
}
