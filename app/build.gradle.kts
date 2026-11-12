plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "com.jinkim.hyundaiAndGretel"
    compileSdk = 35
    useLibrary("android.car", false)
    defaultConfig {
        applicationId = "com.jinnayeong.wakeup"
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
    buildFeatures { compose = true; buildConfig = true }
}
dependencies {
    implementation(libs.uber.h3)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:FusedLocation:1.1.3")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    // TODO(코루틴 업데이트 검토): 버전 변경 시 docs/work/coroutines-upgrade-review.md의 flatMapLatest 점검을 수행한다.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.test:runner:1.7.0")
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    debugImplementation(libs.androidx.compose.ui.tooling)
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

// H3의 Android용 네이티브 파일을 시스템에서 불러올 수 있도록 JNI 라이브러리로도 패키징한다.
val h3Natives by configurations.creating
dependencies { h3Natives(libs.uber.h3) { isTransitive = false } }
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
android.packaging.resources.excludes += setOf("android-*/**", "linux-*/**", "darwin-*/**", "windows-*/**", "META-INF/native-image/**")
tasks.configureEach {
    if (name.startsWith("merge") && name.endsWith("JniLibFolders")) dependsOn(extractH3Natives)
}
