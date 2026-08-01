plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

val minerSpaceVersion = providers.gradleProperty("MINER_SPACE_VERSION").get()

allprojects {
    group = "fr.solremi.minerspace"
    version = minerSpaceVersion
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs all local JVM checks plus Android debug lint and unit tests."
    dependsOn(
        ":shared:check",
        ":domain:check",
        ":data:check",
        ":simulation:check",
        ":game:check",
        ":androidApp:lintDebug",
        ":androidApp:testDebugUnitTest",
    )
}
