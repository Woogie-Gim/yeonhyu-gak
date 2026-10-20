import com.android.build.api.variant.HasHostTestsBuilder
import com.android.build.api.variant.HostTestBuilder
import java.util.Properties

plugins {
    alias(libs.plugins.yh.android.application)
    alias(libs.plugins.yh.android.compose)
    alias(libs.plugins.yh.android.hilt)
    alias(libs.plugins.roborazzi)
}

// 서명 정보: 파일 없으면 release 서명 없이 빌드
val keystoreFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) }
}

android {
    namespace = "com.gsw.yeonhyugak"

    defaultConfig {
        applicationId = "com.gsw.yeonhyugak"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 원격 공휴일 JSON. 테스트는 MockWebServer 주소로 대체
        buildConfigField(
            "String",
            "HOLIDAY_OVERRIDE_URL",
            "\"https://woogie-gim.github.io/yeonhyu-gak-pages/holidays_override.json\"",
        )
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            // R8 축소. keep 규칙은 src/main/keepRules
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
}

// 릴리스 단위 테스트는 개발자 메뉴 미포함 검사용 (AGP 9 기본은 debug만)
androidComponents {
    beforeVariants(selector().withBuildType("release")) { variant ->
        (variant as HasHostTestsBuilder).hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
    }
}

dependencies {
    implementation(project(":core:engine"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:calendar"))
    implementation(project(":feature:recommend"))
    implementation(project(":feature:leave"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:share"))
    implementation(project(":feature:widget"))
    implementation(project(":feature:alerts"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.room.runtime)
    testImplementation(libs.datastore.preferences)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
