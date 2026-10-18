package com.gsw.yeonhyugak.buildlogic

import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

// ktlint·detekt 공통 적용
internal fun Project.configureStaticAnalysis() {
    pluginManager.apply(libs.pluginId("ktlint"))
    pluginManager.apply(libs.pluginId("detekt"))

    extensions.configure<KtlintExtension> {
        version.set(libs.findVersion("ktlint").get().requiredVersion)
        android.set(true)
        filter {
            exclude { it.file.path.contains("generated") }
        }
    }
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        parallel.set(true)
    }
}
