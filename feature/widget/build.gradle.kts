plugins {
    alias(libs.plugins.yh.android.feature)
}

android {
    namespace = "com.gsw.yeonhyugak.feature.widget"
}

dependencies {
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    testImplementation(libs.glance.appwidget.testing)
    testImplementation(libs.room.runtime)
    testImplementation(libs.datastore.preferences)
}
