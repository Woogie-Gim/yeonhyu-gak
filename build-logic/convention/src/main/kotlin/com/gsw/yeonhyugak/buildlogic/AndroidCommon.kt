package com.gsw.yeonhyugak.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

const val COMPILE_SDK = 36
const val COMPILE_SDK_MINOR = 1
const val MIN_SDK = 26
const val TARGET_SDK = 36
val JAVA_VERSION = JavaVersion.VERSION_17

// 앱·라이브러리 공통 안드로이드 설정
internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    extension.apply {
        compileSdk {
            version = release(COMPILE_SDK) {
                minorApiLevel = COMPILE_SDK_MINOR
            }
        }
        defaultConfig.minSdk = MIN_SDK
        compileOptions.sourceCompatibility = JAVA_VERSION
        compileOptions.targetCompatibility = JAVA_VERSION
        lint.apply {
            abortOnError = true
            warningsAsErrors = false
            lintConfig = rootProject.file("config/lint/lint.xml")
        }
        testOptions.unitTests.isIncludeAndroidResources = true
        // Robolectric이 최신 JDK 내부 API 접근
        testOptions.unitTests.all {
            it.jvmArgs(
                "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
            )
            // Hilt가 테스트 없는 모듈에도 생성 소스를 만듦. src/test 있으면 엄격 유지
            it.failOnNoDiscoveredTests.set(file("src/test").exists())
        }
    }
}
