plugins {
    alias(libs.plugins.android.application)
}

val natives by configurations.creating
val generatedNatives = layout.buildDirectory.dir("generated/minerSpaceNatives")
val admobAppId = providers.gradleProperty("ADMOB_APP_ID")
    .orElse("ca-app-pub-3940256099942544~3347511713")
val admobRewardedUnitId = providers.gradleProperty("ADMOB_REWARDED_UNIT_ID")
    .orElse("ca-app-pub-3940256099942544/5224354917")

android {
    namespace = "fr.solremi.minerspace.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "fr.solremi.minerspace"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-alpha01"
        manifestPlaceholders["ADMOB_APP_ID"] = admobAppId.get()
        resValue("string", "admob_rewarded_unit_id", admobRewardedUnitId.get())
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(rootProject.file("assets"))
            jniLibs.srcDir(generatedNatives)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/NOTICE",
            "META-INF/NOTICE.txt",
        )
    }
}


dependencies {
    implementation(project(":game"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":shared"))

    implementation(libs.gdx.backend.android)
    implementation(libs.google.mobile.ads)
    implementation(libs.google.ump)

    val gdxVersion = libs.versions.gdx.get()
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

val extractAndroidNatives by tasks.registering {
    inputs.files(natives)
    outputs.dir(generatedNatives)

    doLast {
        val outputRoot = generatedNatives.get().asFile
        delete(outputRoot)

        natives.files.forEach { archive ->
            val abi = archive.name
                .substringAfter("natives-")
                .substringBefore(".jar")

            copy {
                from(zipTree(archive))
                into(outputRoot.resolve(abi))
                include("*.so")
            }
        }
    }
}

tasks.configureEach {
    if (name.contains("merge", ignoreCase = true) &&
        name.contains("JniLibFolders", ignoreCase = true)
    ) {
        dependsOn(extractAndroidNatives)
    }
}
