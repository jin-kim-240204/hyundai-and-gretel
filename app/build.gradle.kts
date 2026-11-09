plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.3.13"
}
android {
    namespace = "com.jinkim.hyundaiAndGretel"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.jinkim.hyundaiAndGretel"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true }
    packaging.resources.excludes += setOf("darwin-*/**", "windows-*/**", "META-INF/native-image/**")
}
dependencies {
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:FusedLocation:1.1.3")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    implementation("androidx.room:room-runtime:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("com.uber:h3:4.5.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    debugImplementation(libs.androidx.compose.ui.tooling)
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

// H3's JVM jar stores Android .so files as resources; package them as Android JNI libraries.
val h3Natives by configurations.creating
dependencies { h3Natives("com.uber:h3:4.5.1") { isTransitive = false } }
val extractH3Natives by tasks.registering(Copy::class) {
    from(h3Natives.map { zipTree(it) }) {
        include("android-arm64/*.so", "android-arm/*.so")
        eachFile {
            relativePath = RelativePath(true, if (path.startsWith("android-arm64/")) "arm64-v8a" else "armeabi-v7a", name)
        }
        includeEmptyDirs = false
    }
    into(layout.buildDirectory.dir("generated/h3Jni"))
}
android.sourceSets.getByName("main").jniLibs.srcDir(layout.buildDirectory.dir("generated/h3Jni").get().asFile)
tasks.configureEach {
    if (name.startsWith("merge") && name.endsWith("JniLibFolders")) dependsOn(extractH3Natives)
}
