plugins {
    alias(libs.plugins.yh.android.feature)
}

android {
    namespace = "com.gsw.yeonhyugak.feature.calendar"
}

dependencies {
    // 테스트 하네스가 실제 저장소를 메모리 DB로 만든다
    testImplementation(libs.room.runtime)
    testImplementation(libs.datastore.preferences)
}
