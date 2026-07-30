plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    //alias(libs.plugins.hiltAndroid) apply false
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.kotlinParcelize)
    alias(libs.plugins.kotlinKapt)
    alias(libs.plugins.moduleCrashlytics )
}

android {
    namespace = "com.pioneer.nycfirewire"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pioneer.nycfirewire"
        minSdk = 24
        targetSdk = 35
        versionCode = 111
        versionName = "9.9"
       /* versionCode = 25
        versionName = "2.5"*/

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }


    buildFeatures {
        viewBinding = true
        buildConfig= true
    }
    dexOptions{
        incremental=true

    }


}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.coil)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.converter.scalars)
    implementation(libs.logging.interceptor)
    implementation(libs.circleimageview)
    implementation(libs.play.services.auth)
    implementation(libs.gson)
    implementation(libs.shimmer)
    implementation(libs.commons.lang3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.converter.simplexml)
    implementation(libs.jsoup)
    implementation(libs.glide)
    implementation(libs.facebook.login)
    implementation(libs.onesignal)
    implementation(libs.billing.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.hilt.android)
    implementation(libs.androidx.databinding.runtime)
    implementation(libs.androidx.hilt.work)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.app.update)
    implementation(libs.androidx.paging.runtime)
    kapt(libs.hilt.android.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation(libs.targetview)

        // Add the dependencies for the Crashlytics and Analytics libraries
        // When using the BoM, you don't specify versions in Firebase library dependencies
        implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")
    implementation ("androidx.work:work-runtime-ktx:2.9.0")
    implementation ("com.github.Baseflow:PhotoView:2.3.0")



}

kapt {
    correctErrorTypes = true
}

// ---------------------------------------------------------------------------
// RELEASE GUARD: refuse to build a release while the API points at staging.
//
// ApiClient.BASE_URL has sat on staging.api.nycfirewireapp.com since 2025-11-30
// while this branch carried release version codes, so a release built straight
// from source would silently ship pointed at staging. Debug builds are
// unaffected — pointing them at staging is normal and expected.
// ---------------------------------------------------------------------------
val checkReleaseApiUrl = tasks.register("checkReleaseApiUrl") {
    val apiClient = file("src/main/java/com/pioneer/nycfirewire/data/auth/ApiClient.kt")
    inputs.file(apiClient)
    doLast {
        if (!apiClient.exists()) {
            throw GradleException("Release guard: ${apiClient.path} not found — cannot verify the API base URL.")
        }
        val active = apiClient.readLines()
            .map { it.trim() }
            .filter { !it.startsWith("//") && it.contains("const val BASE_URL") }
        if (active.isEmpty()) {
            throw GradleException("Release guard: no active BASE_URL found in ApiClient.kt.")
        }
        val bad = active.filter { it.contains("staging.") || it.contains("dev-firewire") }
        if (bad.isNotEmpty()) {
            throw GradleException(
                "\n\nRELEASE BLOCKED — the API base URL is not production:\n" +
                bad.joinToString("\n") { "    $it" } +
                "\n\nSet ApiClient.BASE_URL to https://api.nycfirewireapp.com before building a release.\n"
            )
        }
        logger.lifecycle("Release guard OK — API base URL: ${active.first()}")
    }
}

tasks.matching { it.name.startsWith("assembleRelease") || it.name.startsWith("bundleRelease") }
    .configureEach { dependsOn(checkReleaseApiUrl) }
