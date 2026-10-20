plugins {
    alias(libs.plugins.yh.android.feature)
}

android {
    namespace = "com.gsw.yeonhyugak.feature.share"
}

dependencies {
    implementation(libs.zxing.core)

    testImplementation(libs.room.runtime)
    testImplementation(libs.datastore.preferences)
}
