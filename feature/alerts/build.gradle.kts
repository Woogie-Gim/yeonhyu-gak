plugins {
    alias(libs.plugins.yh.android.library)
    alias(libs.plugins.yh.android.hilt)
}

android {
    namespace = "com.gsw.yeonhyugak.feature.alerts"
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.work.runtime.ktx)

    testImplementation(libs.androidx.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.work.testing)
    testImplementation(libs.room.runtime)
    testImplementation(libs.datastore.preferences)
}
