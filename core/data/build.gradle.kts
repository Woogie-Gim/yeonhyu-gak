plugins {
    alias(libs.plugins.yh.android.library)
    alias(libs.plugins.yh.android.hilt)
}

android {
    namespace = "com.gsw.yeonhyugak.core.data"
}

dependencies {
    api(project(":core:engine"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
}
