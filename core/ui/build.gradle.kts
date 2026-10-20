plugins {
    alias(libs.plugins.yh.android.library)
    alias(libs.plugins.yh.android.compose)
}

android {
    namespace = "com.gsw.yeonhyugak.core.ui"
}

dependencies {
    // 테마 id · 효과음 정책
    api(project(":core:engine"))
    // 알림 권한 요청
    implementation(libs.androidx.activity.compose)
}
