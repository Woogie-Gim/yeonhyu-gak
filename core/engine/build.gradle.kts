plugins {
    alias(libs.plugins.yh.jvm.library)
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}

// 엔진 테스트가 번들 JSON을 직접 읽는다
tasks.test {
    systemProperty(
        "yh.assetsDir",
        rootProject.file("core/data/src/main/assets/holidays").absolutePath,
    )
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
