plugins {
    alias(libs.plugins.yh.android.feature)
}

android {
    namespace = "com.gsw.yeonhyugak.feature.recommend"
}

dependencies {
    // 테스트가 실제 저장소를 메모리 DB·임시 DataStore로 만든다
    testImplementation(libs.room.runtime)
    testImplementation(libs.datastore.preferences)
}
