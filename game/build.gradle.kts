plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(libs.gdx.core)
    implementation(libs.ktx.app)
    implementation(libs.ktx.graphics)

    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":shared"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
