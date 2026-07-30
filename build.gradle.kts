plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

allprojects {
    group = "fr.solremi.minerspace"
    version = "0.1.0-alpha01"
}
