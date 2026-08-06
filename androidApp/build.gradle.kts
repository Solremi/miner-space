plugins {
    alias(libs.plugins.android.application)
}

val natives by configurations.creating
val generatedNatives = layout.buildDirectory.dir("generated/minerSpaceNatives")
val appVersionName = providers.gradleProperty("MINER_SPACE_VERSION").get()
val appVersionCode = providers.gradleProperty("MINER_SPACE_VERSION_CODE").map { it.toInt() }.get()

fun releaseValue(name: String) = providers.gradleProperty(name).orElse(providers.environmentVariable(name))

val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testAdmobRewardedUnitId = "ca-app-pub-3940256099942544/5224354917"
val admobAppId = releaseValue("ADMOB_APP_ID")
val admobRewardedUnitId = releaseValue("ADMOB_REWARDED_UNIT_ID")
val privacyPolicyUrl = releaseValue("PRIVACY_POLICY_URL")
val supportEmail = releaseValue("SUPPORT_EMAIL")
val releaseStoreFile = releaseValue("RELEASE_STORE_FILE")
val releaseStorePassword = releaseValue("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseValue("RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseValue("RELEASE_KEY_PASSWORD")
val releaseSigningConfigured = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it.isPresent }

android {
    namespace = "fr.solremi.minerspace.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "fr.solremi.minerspace"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        manifestPlaceholders["ADMOB_APP_ID"] = testAdmobAppId
        resValue("string", "admob_rewarded_unit_id", testAdmobRewardedUnitId)
        resValue("string", "privacy_policy_url", "local://legal/privacy-policy-fr.md")
        resValue("string", "support_email", "contact-via-google-play")
    }

    signingConfigs {
        create("release") {
            if (releaseSigningConfigured) {
                storeFile = file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
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
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningConfigured) signingConfig = signingConfigs.getByName("release")
            manifestPlaceholders["ADMOB_APP_ID"] = admobAppId.orElse("").get()
            resValue("string", "admob_rewarded_unit_id", admobRewardedUnitId.orElse("").get())
            resValue("string", "privacy_policy_url", privacyPolicyUrl.orElse("").get())
            resValue("string", "support_email", supportEmail.orElse("").get())
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(rootProject.file("assets"))
            jniLibs.srcDir(generatedNatives.get().asFile)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        checkDependencies = true
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

val validateReleaseConfiguration by tasks.registering {
    group = "verification"
    description = "Rejects unsafe or incomplete Miner Space release configuration."
    doLast {
        val errors = mutableListOf<String>()
        if (!releaseSigningConfigured) errors += "Release signing properties are incomplete."
        if (!admobAppId.isPresent || admobAppId.get() == testAdmobAppId) errors += "A production ADMOB_APP_ID is required."
        if (!admobRewardedUnitId.isPresent || admobRewardedUnitId.get() == testAdmobRewardedUnitId) errors += "A production ADMOB_REWARDED_UNIT_ID is required."
        if (!privacyPolicyUrl.isPresent || !privacyPolicyUrl.get().startsWith("https://")) errors += "PRIVACY_POLICY_URL must be a published HTTPS URL."
        if (!supportEmail.isPresent || !supportEmail.get().contains('@')) errors += "SUPPORT_EMAIL must be configured."
        if ((android.defaultConfig.versionCode ?: 0) != appVersionCode) errors += "versionCode must match MINER_SPACE_VERSION_CODE."
        if (android.defaultConfig.versionName != appVersionName) errors += "versionName must match MINER_SPACE_VERSION."
        check(errors.isEmpty()) { errors.joinToString(separator = "\n") }
    }
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
    if (name.contains("merge", ignoreCase = true) && name.contains("JniLibFolders", ignoreCase = true)) {
        dependsOn(extractAndroidNatives)
    }
    if (name == "preReleaseBuild") dependsOn(validateReleaseConfiguration)
}
